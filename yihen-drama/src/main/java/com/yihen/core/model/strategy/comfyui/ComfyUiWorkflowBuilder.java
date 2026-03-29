package com.yihen.core.model.strategy.comfyui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yihen.entity.ModelInstance;
import com.yihen.http.HttpExecutor;
import com.yihen.util.UrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ComfyUiWorkflowBuilder {

    private static final Set<String> PSEUDO_NODE_TYPES = Set.of(
            "PrimitiveInt",
            "PrimitiveFloat",
            "PrimitiveString",
            "PrimitiveStringMultiline",
            "PrimitiveBoolean",
            "PrimitiveCombo",
            "Reroute",
            "Note"
    );

    private final ComfyUiClient comfyUiClient;
    private final HttpExecutor httpExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PromptBuildResult buildPrompt(
            String baseUrl,
            String apiKey,
            ModelInstance modelInstance,
            Map<String, Object> runtimeInputs
    ) throws Exception {
        JsonNode template = loadWorkflowTemplate(baseUrl, apiKey, modelInstance);
        ObjectNode promptRoot = template.deepCopy();

        Map<String, Object> params = safeParams(modelInstance);
        Set<String> explicitlyMappedKeys = applyExplicitMappings(promptRoot, baseUrl, apiKey, params, runtimeInputs);
        applyHeuristics(promptRoot, baseUrl, apiKey, runtimeInputs, explicitlyMappedKeys);

        return new PromptBuildResult(
                objectMapper.convertValue(promptRoot, Map.class),
                extractOutputNodeIds(params)
        );
    }

    public List<String> getOutputNodeIds(ModelInstance modelInstance) {
        return extractOutputNodeIds(safeParams(modelInstance));
    }

    public String getQueuePath(ModelInstance modelInstance) {
        String path = modelInstance.getPath();
        return StringUtils.hasText(path) ? path : "/prompt";
    }

    private JsonNode loadWorkflowTemplate(String baseUrl, String apiKey, ModelInstance modelInstance) throws Exception {
        Map<String, Object> params = safeParams(modelInstance);
        String apiWorkflowPath = getStringParam(params, "apiWorkflowPath");
        if (StringUtils.hasText(apiWorkflowPath) && Files.exists(Paths.get(apiWorkflowPath))) {
            JsonNode root = readJson(apiWorkflowPath);
            return root.has("prompt") && root.get("prompt").isObject() ? root.get("prompt") : root;
        }

        String workflowPath = getStringParam(params, "workflowPath");
        if (!StringUtils.hasText(workflowPath)) {
            throw new IllegalStateException("ComfyUI workflowPath is not configured");
        }
        return convertUiWorkflowToPrompt(baseUrl, apiKey, readJson(workflowPath));
    }

    private JsonNode readJson(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Workflow file does not exist: " + filePath);
        }
        return objectMapper.readTree(Files.readString(path));
    }

    private JsonNode convertUiWorkflowToPrompt(String baseUrl, String apiKey, JsonNode workflowRoot) throws Exception {
        Map<String, JsonNode> nodesById = new LinkedHashMap<>();
        JsonNode nodes = workflowRoot.path("nodes");
        if (!nodes.isArray()) {
            throw new IllegalStateException("Invalid ComfyUI workflow json: missing nodes array");
        }
        for (JsonNode node : nodes) {
            nodesById.put(node.path("id").asText(), node);
        }

        Map<String, LinkInfo> linksById = new HashMap<>();
        JsonNode links = workflowRoot.path("links");
        if (links.isArray()) {
            for (JsonNode link : links) {
                LinkInfo linkInfo = parseLink(link);
                if (linkInfo != null) {
                    linksById.put(linkInfo.id(), linkInfo);
                }
            }
        }

        ObjectNode promptRoot = objectMapper.createObjectNode();
        Map<String, JsonNode> nodeInfoCache = new HashMap<>();

        for (JsonNode node : nodes) {
            String nodeId = node.path("id").asText();
            String classType = node.path("type").asText();
            if (isPseudoNode(classType)) {
                continue;
            }

            JsonNode nodeInfo = nodeInfoCache.computeIfAbsent(classType, key -> {
                try {
                    return comfyUiClient.getNodeInfo(baseUrl, apiKey, key);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            ObjectNode promptNode = objectMapper.createObjectNode();
            promptNode.put("class_type", classType);
            ObjectNode inputsNode = objectMapper.createObjectNode();

            Map<String, JsonNode> widgetValueMap = buildWidgetValueMap(node, nodeInfo);
            JsonNode inputArray = node.path("inputs");
            if (inputArray.isArray()) {
                for (JsonNode input : inputArray) {
                    String inputName = input.path("name").asText();
                    JsonNode linkNode = input.get("link");
                    if (linkNode != null && !linkNode.isNull()) {
                        Object resolved = resolveLinkValue(linkNode.asText(), nodesById, linksById);
                        if (resolved != null) {
                            inputsNode.set(inputName, objectMapper.valueToTree(resolved));
                        }
                        continue;
                    }
                    JsonNode widgetValue = widgetValueMap.get(inputName);
                    if (widgetValue != null && !widgetValue.isNull()) {
                        inputsNode.set(inputName, widgetValue.deepCopy());
                    }
                }
            }

            promptNode.set("inputs", inputsNode);
            promptRoot.set(nodeId, promptNode);
        }

        return promptRoot;
    }

    private Map<String, JsonNode> buildWidgetValueMap(JsonNode node, JsonNode nodeInfo) {
        Map<String, JsonNode> values = new HashMap<>();
        JsonNode inputs = node.path("inputs");
        JsonNode widgetsValues = node.path("widgets_values");
        if (!inputs.isArray() || !widgetsValues.isArray()) {
            return values;
        }

        int widgetIndex = 0;
        for (JsonNode input : inputs) {
            if (!input.has("widget")) {
                continue;
            }
            if (widgetIndex < widgetsValues.size()) {
                values.put(input.path("name").asText(), widgetsValues.get(widgetIndex));
            }
            widgetIndex++;
            if (hasControlAfterGenerate(nodeInfo, input.path("name").asText()) && widgetIndex < widgetsValues.size()) {
                widgetIndex++;
            }
        }
        return values;
    }

    private boolean hasControlAfterGenerate(JsonNode nodeInfo, String inputName) {
        JsonNode metadata = getInputMetadata(nodeInfo, inputName);
        return metadata != null && metadata.path("control_after_generate").asBoolean(false);
    }

    private JsonNode getInputMetadata(JsonNode nodeInfo, String inputName) {
        if (nodeInfo == null || nodeInfo.isMissingNode()) {
            return null;
        }
        JsonNode input = nodeInfo.path("input");
        for (String bucket : List.of("required", "optional")) {
            JsonNode bucketNode = input.path(bucket).path(inputName);
            if (bucketNode.isArray() && bucketNode.size() > 1 && bucketNode.get(1).isObject()) {
                return bucketNode.get(1);
            }
        }
        return null;
    }

    private Object resolveLinkValue(String linkId, Map<String, JsonNode> nodesById, Map<String, LinkInfo> linksById) {
        LinkInfo linkInfo = linksById.get(linkId);
        if (linkInfo == null) {
            return null;
        }
        JsonNode originNode = nodesById.get(linkInfo.originId());
        if (originNode == null) {
            return List.of(linkInfo.originId(), linkInfo.originSlot());
        }

        String originType = originNode.path("type").asText();
        if (originType.startsWith("Primitive")) {
            JsonNode widgets = originNode.path("widgets_values");
            return widgets.isArray() && widgets.size() > 0
                    ? objectMapper.convertValue(widgets.get(0), Object.class)
                    : null;
        }
        if ("Reroute".equals(originType)) {
            JsonNode rerouteInputs = originNode.path("inputs");
            if (rerouteInputs.isArray() && rerouteInputs.size() > 0) {
                JsonNode rerouteLink = rerouteInputs.get(0).get("link");
                if (rerouteLink != null && !rerouteLink.isNull()) {
                    return resolveLinkValue(rerouteLink.asText(), nodesById, linksById);
                }
            }
            return null;
        }
        return List.of(linkInfo.originId(), linkInfo.originSlot());
    }

    private LinkInfo parseLink(JsonNode linkNode) {
        if (linkNode == null || linkNode.isNull()) {
            return null;
        }
        if (linkNode.isArray() && linkNode.size() >= 5) {
            return new LinkInfo(
                    linkNode.get(0).asText(),
                    linkNode.get(1).asText(),
                    linkNode.get(2).asInt(),
                    linkNode.get(3).asText(),
                    linkNode.get(4).asInt()
            );
        }
        if (linkNode.isObject()) {
            return new LinkInfo(
                    linkNode.path("id").asText(),
                    linkNode.path("origin_id").asText(),
                    linkNode.path("origin_slot").asInt(),
                    linkNode.path("target_id").asText(),
                    linkNode.path("target_slot").asInt()
            );
        }
        return null;
    }

    private Set<String> applyExplicitMappings(
            ObjectNode promptRoot,
            String baseUrl,
            String apiKey,
            Map<String, Object> params,
            Map<String, Object> runtimeInputs
    ) throws Exception {
        Object mappingRaw = parseJsonIfNeeded(params.get("inputMapping"));
        if (!(mappingRaw instanceof Map<?, ?> mapping)) {
            return Collections.emptySet();
        }

        Set<String> explicitlyMappedKeys = new HashSet<>();
        for (Map.Entry<String, Object> entry : runtimeInputs.entrySet()) {
            List<InputBinding> bindings = extractBindings(mapping.get(entry.getKey()));
            if (bindings.isEmpty()) {
                continue;
            }
            explicitlyMappedKeys.add(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Collection<?> collection) {
                List<?> values = new ArrayList<>(collection);
                for (int i = 0; i < bindings.size() && i < values.size(); i++) {
                    applyBinding(promptRoot, baseUrl, apiKey, bindings.get(i), values.get(i));
                }
            } else {
                for (InputBinding binding : bindings) {
                    applyBinding(promptRoot, baseUrl, apiKey, binding, value);
                }
            }
        }
        return explicitlyMappedKeys;
    }

    private void applyHeuristics(
            ObjectNode promptRoot,
            String baseUrl,
            String apiKey,
            Map<String, Object> runtimeInputs,
            Set<String> explicitlyMappedKeys
    ) throws Exception {
        Object prompt = firstNonNull(runtimeInputs.get("prompt"), runtimeInputs.get("text"), runtimeInputs.get("description"));
        boolean promptExplicitlyMapped = hasAnyMappedKey(explicitlyMappedKeys, "prompt", "text", "description");
        if (!promptExplicitlyMapped && prompt != null) {
            replaceAllInputs(promptRoot, "text", prompt);
            replaceAllInputs(promptRoot, "prompt", prompt);
        }

        replaceIfPresent(promptRoot, "width", runtimeInputs.get("width"));
        replaceIfPresent(promptRoot, "height", runtimeInputs.get("height"));
        replaceIfPresent(promptRoot, "duration", runtimeInputs.get("duration"));
        replaceIfPresent(promptRoot, "fps", runtimeInputs.get("fps"));

        List<Object> images = new ArrayList<>();
        appendIfPresent(images, runtimeInputs.get("image"));
        appendCollectionIfPresent(images, runtimeInputs.get("images"));
        appendCollectionIfPresent(images, runtimeInputs.get("refImages"));
        boolean imagesExplicitlyMapped = hasAnyMappedKey(explicitlyMappedKeys, "image", "images", "refImages");
        if (!imagesExplicitlyMapped && !images.isEmpty()) {
            List<String> loadImageNodeIds = findLoadImageNodeIds(promptRoot);
            for (int i = 0; i < loadImageNodeIds.size() && i < images.size(); i++) {
                String uploaded = uploadInputAsset(baseUrl, apiKey, images.get(i), null, "comfy-input");
                if (uploaded != null) {
                    setPromptInput(promptRoot, loadImageNodeIds.get(i), "image", uploaded);
                }
            }
        }
    }

    private boolean hasAnyMappedKey(Set<String> explicitlyMappedKeys, String... keys) {
        if (explicitlyMappedKeys == null || explicitlyMappedKeys.isEmpty()) {
            return false;
        }
        for (String key : keys) {
            if (explicitlyMappedKeys.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private void replaceIfPresent(ObjectNode promptRoot, String inputName, Object value) {
        if (value != null) {
            replaceAllInputs(promptRoot, inputName, value);
        }
    }

    private void replaceAllInputs(ObjectNode promptRoot, String inputName, Object value) {
        JsonNode valueNode = objectMapper.valueToTree(value);
        promptRoot.fields().forEachRemaining(entry -> {
            JsonNode inputs = entry.getValue().path("inputs");
            if (inputs instanceof ObjectNode objectNode && objectNode.has(inputName)) {
                objectNode.set(inputName, valueNode.deepCopy());
            }
        });
    }

    private List<String> findLoadImageNodeIds(ObjectNode promptRoot) {
        List<String> nodeIds = new ArrayList<>();
        promptRoot.fields().forEachRemaining(entry -> {
            JsonNode node = entry.getValue();
            if ("LoadImage".equals(node.path("class_type").asText())) {
                nodeIds.add(entry.getKey());
            }
        });
        return nodeIds;
    }

    private void applyBinding(
            ObjectNode promptRoot,
            String baseUrl,
            String apiKey,
            InputBinding binding,
            Object value
    ) throws Exception {
        if (value == null) {
            return;
        }
        Object finalValue = value;
        if (isImageLikeBinding(binding, value)) {
            finalValue = uploadInputAsset(baseUrl, apiKey, value, binding.subfolder(), binding.filenamePrefix());
        } else if ("int".equalsIgnoreCase(binding.type())) {
            finalValue = Integer.parseInt(String.valueOf(value));
        } else if ("float".equalsIgnoreCase(binding.type()) || "double".equalsIgnoreCase(binding.type())) {
            finalValue = Double.parseDouble(String.valueOf(value));
        } else if ("boolean".equalsIgnoreCase(binding.type())) {
            finalValue = Boolean.parseBoolean(String.valueOf(value));
        }
        setPromptInput(promptRoot, binding.nodeId(), binding.field(), finalValue);
    }

    private boolean isImageLikeBinding(InputBinding binding, Object value) {
        if ("image".equalsIgnoreCase(binding.type()) || "file".equalsIgnoreCase(binding.type())) {
            return true;
        }
        if (value instanceof byte[]) {
            return true;
        }
        return value instanceof String text && (text.startsWith("http://") || text.startsWith("https://") || Files.exists(Paths.get(text)));
    }

    private String uploadInputAsset(
            String baseUrl,
            String apiKey,
            Object value,
            String subfolder,
            String filenamePrefix
    ) throws Exception {
        byte[] bytes;
        String extension = "png";
        if (value instanceof byte[] rawBytes) {
            bytes = rawBytes;
        } else if (value instanceof String text) {
            if (text.startsWith("http://") || text.startsWith("https://")) {
                bytes = httpExecutor.downloadImage(text).block();
                String ext = UrlUtils.extractFileExtension(text);
                if (StringUtils.hasText(ext)) {
                    extension = ext;
                }
            } else {
                Path path = Paths.get(text);
                bytes = Files.readAllBytes(path);
                String ext = UrlUtils.extractFileExtension(path.getFileName().toString());
                if (StringUtils.hasText(ext)) {
                    extension = ext;
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported ComfyUI asset input: " + value.getClass().getName());
        }

        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Failed to load ComfyUI input asset");
        }

        String prefix = StringUtils.hasText(filenamePrefix) ? filenamePrefix : "comfy-input";
        String filename = prefix + "-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + "." + extension;
        ComfyUiClient.UploadedFile uploadedFile = comfyUiClient.uploadImage(baseUrl, apiKey, filename, bytes, subfolder);
        return uploadedFile.asComfyInputValue();
    }

    private void setPromptInput(ObjectNode promptRoot, String nodeId, String field, Object value) {
        JsonNode node = promptRoot.get(nodeId);
        if (node == null || node.isMissingNode()) {
            return;
        }
        JsonNode inputs = node.path("inputs");
        if (inputs instanceof ObjectNode inputsObject) {
            inputsObject.set(field, objectMapper.valueToTree(value));
        }
    }

    private List<InputBinding> extractBindings(Object rawBindings) {
        Object normalized = parseJsonIfNeeded(rawBindings);
        if (normalized instanceof List<?> list) {
            List<InputBinding> bindings = new ArrayList<>();
            for (Object item : list) {
                bindings.addAll(extractBindings(item));
            }
            return bindings;
        }
        if (!(normalized instanceof Map<?, ?> map)) {
            return List.of();
        }

        String field = asString(map.get("field"));
        String type = asString(map.get("type"));
        String subfolder = asString(map.get("subfolder"));
        String filenamePrefix = asString(map.get("filenamePrefix"));

        Object nodeIdsRaw = map.get("nodeIds");
        if (nodeIdsRaw instanceof Collection<?> nodeIds) {
            List<InputBinding> bindings = new ArrayList<>();
            for (Object nodeId : nodeIds) {
                if (nodeId != null) {
                    bindings.add(new InputBinding(String.valueOf(nodeId), field, type, subfolder, filenamePrefix));
                }
            }
            return bindings;
        }

        String nodeId = asString(map.get("nodeId"));
        if (!StringUtils.hasText(nodeId) || !StringUtils.hasText(field)) {
            return List.of();
        }
        return List.of(new InputBinding(nodeId, field, type, subfolder, filenamePrefix));
    }

    private List<String> extractOutputNodeIds(Map<String, Object> params) {
        Object raw = parseJsonIfNeeded(params.get("outputMapping"));
        if (raw instanceof Map<?, ?> map) {
            Object nodeIdsRaw = map.get("nodeIds");
            if (nodeIdsRaw instanceof Collection<?> nodeIds) {
                List<String> result = new ArrayList<>();
                for (Object nodeId : nodeIds) {
                    if (nodeId != null) {
                        result.add(String.valueOf(nodeId));
                    }
                }
                return result;
            }
            String nodeId = asString(map.get("nodeId"));
            if (StringUtils.hasText(nodeId)) {
                return List.of(nodeId);
            }
        }
        if (raw instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String nodeId = asString(map.get("nodeId"));
                    if (StringUtils.hasText(nodeId)) {
                        result.add(nodeId);
                    }
                } else if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        return List.of();
    }

    private Object parseJsonIfNeeded(Object value) {
        if (!(value instanceof String text)) {
            return value;
        }
        String trimmed = text.trim();
        if ((!trimmed.startsWith("{") || !trimmed.endsWith("}")) &&
                (!trimmed.startsWith("[") || !trimmed.endsWith("]"))) {
            return value;
        }
        try {
            return objectMapper.readValue(trimmed, Object.class);
        } catch (Exception ignore) {
            return value;
        }
    }

    private boolean isPseudoNode(String classType) {
        return PSEUDO_NODE_TYPES.contains(classType);
    }

    private Map<String, Object> safeParams(ModelInstance modelInstance) {
        return modelInstance.getParams() == null ? Collections.emptyMap() : modelInstance.getParams();
    }

    private String getStringParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private void appendIfPresent(List<Object> values, Object value) {
        if (value != null) {
            values.add(value);
        }
    }

    private void appendCollectionIfPresent(List<Object> values, Object value) {
        if (value instanceof Collection<?> collection) {
            values.addAll(collection.stream().filter(Objects::nonNull).toList());
        }
    }

    public record PromptBuildResult(Map<String, Object> prompt, List<String> outputNodeIds) {
    }

    private record LinkInfo(String id, String originId, int originSlot, String targetId, int targetSlot) {
    }

    private record InputBinding(String nodeId, String field, String type, String subfolder, String filenamePrefix) {
    }
}
