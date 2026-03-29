package com.yihen.core.model.strategy.comfyui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yihen.http.HttpExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ComfyUiClient {

    private static final List<String> IMAGE_KEYS = List.of("images", "result");
    private static final List<String> VIDEO_KEYS = List.of("videos", "gifs", "result");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "bmp");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov", "mkv", "gif");

    private final HttpExecutor httpExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, JsonNode> nodeInfoCache = new ConcurrentHashMap<>();

    public String queuePrompt(String baseUrl, String path, String apiKey, Map<String, Object> prompt) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("prompt", prompt);
        String response = httpExecutor.post(baseUrl, normalizePath(path, "/prompt"), apiKey, body).block();
        JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
        JsonNode promptIdNode = root.get("prompt_id");
        if (promptIdNode != null && StringUtils.hasText(promptIdNode.asText())) {
            return promptIdNode.asText();
        }
        if (root.has("node_errors") && root.get("node_errors").size() > 0) {
            throw new IllegalStateException("ComfyUI workflow validation failed: " + root.get("node_errors"));
        }
        throw new IllegalStateException("ComfyUI did not return prompt_id");
    }

    public JsonNode getHistory(String baseUrl, String apiKey, String promptId) throws Exception {
        String response = httpExecutor.get(
                baseUrl,
                "/history/" + URLEncoder.encode(promptId, StandardCharsets.UTF_8),
                apiKey
        ).block();
        return objectMapper.readTree(response == null ? "{}" : response);
    }

    public JsonNode getNodeInfo(String baseUrl, String apiKey, String classType) throws Exception {
        String cacheKey = baseUrl + "|" + classType;
        JsonNode cached = nodeInfoCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String encoded = UriUtils.encodePathSegment(classType, StandardCharsets.UTF_8);
        String response = httpExecutor.get(baseUrl, "/object_info/" + encoded, apiKey).block();
        JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
        JsonNode info = root.get(classType);
        if (info == null || info.isMissingNode()) {
            throw new IllegalStateException("Cannot resolve ComfyUI node info for class: " + classType);
        }
        nodeInfoCache.put(cacheKey, info);
        return info;
    }

    public UploadedFile uploadImage(String baseUrl, String apiKey, String filename, byte[] bytes, String subfolder) throws Exception {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", MultipartNamedByteArrayResource.of(filename, bytes));
        body.add("type", "input");
        if (StringUtils.hasText(subfolder)) {
            body.add("subfolder", subfolder);
        }
        String response = httpExecutor.postMultipart(baseUrl, "/upload/image", apiKey, body).block();
        JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
        String name = root.path("name").asText(filename);
        String uploadSubfolder = root.path("subfolder").asText(subfolder == null ? "" : subfolder);
        String type = root.path("type").asText("input");
        return new UploadedFile(name, uploadSubfolder, type);
    }

    public JsonNode resolveHistoryItem(JsonNode historyRoot, String promptId) {
        if (historyRoot == null || historyRoot.isMissingNode() || historyRoot.isNull()) {
            return null;
        }
        JsonNode direct = historyRoot.get(promptId);
        if (direct != null && !direct.isMissingNode()) {
            return direct;
        }
        Iterator<JsonNode> iterator = historyRoot.elements();
        return iterator.hasNext() ? iterator.next() : null;
    }

    public Optional<OutputFile> extractImageOutput(JsonNode historyRoot, String promptId, Collection<String> preferredNodeIds) {
        return extractOutput(historyRoot, promptId, preferredNodeIds, MediaKind.IMAGE);
    }

    public Optional<OutputFile> extractVideoOutput(JsonNode historyRoot, String promptId, Collection<String> preferredNodeIds) {
        return extractOutput(historyRoot, promptId, preferredNodeIds, MediaKind.VIDEO);
    }

    public String buildViewUrl(String baseUrl, OutputFile outputFile) {
        StringBuilder builder = new StringBuilder();
        builder.append(stripTrailingSlash(baseUrl)).append("/view?filename=")
                .append(URLEncoder.encode(outputFile.filename(), StandardCharsets.UTF_8));
        builder.append("&subfolder=")
                .append(URLEncoder.encode(outputFile.subfolder() == null ? "" : outputFile.subfolder(), StandardCharsets.UTF_8));
        builder.append("&type=")
                .append(URLEncoder.encode(outputFile.type() == null ? "output" : outputFile.type(), StandardCharsets.UTF_8));
        return builder.toString();
    }

    private Optional<OutputFile> extractOutput(
            JsonNode historyRoot,
            String promptId,
            Collection<String> preferredNodeIds,
            MediaKind mediaKind
    ) {
        JsonNode historyItem = resolveHistoryItem(historyRoot, promptId);
        if (historyItem == null) {
            return Optional.empty();
        }
        JsonNode outputs = historyItem.path("outputs");
        if (outputs.isMissingNode() || outputs.isNull()) {
            return Optional.empty();
        }

        List<String> nodeIds = new ArrayList<>();
        if (preferredNodeIds != null) {
            preferredNodeIds.stream()
                    .filter(StringUtils::hasText)
                    .filter(outputs::has)
                    .forEach(nodeIds::add);
        }
        if (nodeIds.isEmpty()) {
            outputs.fieldNames().forEachRemaining(nodeIds::add);
        }

        for (String nodeId : nodeIds) {
            JsonNode outputNode = outputs.get(nodeId);
            Optional<OutputFile> file = findOutputFile(outputNode, mediaKind);
            if (file.isPresent()) {
                return file;
            }
        }
        return Optional.empty();
    }

    private Optional<OutputFile> findOutputFile(JsonNode outputNode, MediaKind mediaKind) {
        if (outputNode == null || outputNode.isMissingNode() || outputNode.isNull()) {
            return Optional.empty();
        }
        List<String> keys = mediaKind == MediaKind.VIDEO ? VIDEO_KEYS : IMAGE_KEYS;
        for (String key : keys) {
            JsonNode items = outputNode.get(key);
            if (items == null || !items.isArray()) {
                continue;
            }
            for (JsonNode item : items) {
                OutputFile outputFile = toOutputFile(item, key);
                if (outputFile != null && matchesKind(outputFile, mediaKind)) {
                    return Optional.of(outputFile);
                }
            }
        }
        return Optional.empty();
    }

    private OutputFile toOutputFile(JsonNode item, String sourceKey) {
        if (item == null || item.isNull()) {
            return null;
        }
        if (item.isTextual()) {
            String filename = item.asText();
            if (!StringUtils.hasText(filename)) {
                return null;
            }
            return new OutputFile(filename, "", "output", inferMediaType(filename, sourceKey));
        }
        if (!item.isObject()) {
            return null;
        }
        String filename = item.path("filename").asText(null);
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        String subfolder = item.path("subfolder").asText("");
        String type = item.path("type").asText("output");
        String mediaType = item.path("mediaType").asText(null);
        if (!StringUtils.hasText(mediaType)) {
            mediaType = inferMediaType(filename, sourceKey);
        }
        return new OutputFile(filename, subfolder, type, mediaType);
    }

    private boolean matchesKind(OutputFile outputFile, MediaKind mediaKind) {
        String mediaType = outputFile.mediaType() == null ? "" : outputFile.mediaType().toLowerCase(Locale.ROOT);
        return switch (mediaKind) {
            case IMAGE -> "image".equals(mediaType);
            case VIDEO -> "video".equals(mediaType);
        };
    }

    private String inferMediaType(String filename, String sourceKey) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        String ext = dot >= 0 ? lower.substring(dot + 1) : "";
        if (VIDEO_EXTENSIONS.contains(ext) || "videos".equals(sourceKey) || "gifs".equals(sourceKey)) {
            return "video";
        }
        if (IMAGE_EXTENSIONS.contains(ext) || "images".equals(sourceKey)) {
            return "image";
        }
        return "file";
    }

    private String normalizePath(String path, String defaultValue) {
        String finalPath = StringUtils.hasText(path) ? path : defaultValue;
        return finalPath.startsWith("/") ? finalPath : "/" + finalPath;
    }

    private String stripTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private enum MediaKind {
        IMAGE,
        VIDEO
    }

    public record UploadedFile(String name, String subfolder, String type) {
        public String asComfyInputValue() {
            if (!StringUtils.hasText(subfolder)) {
                return name;
            }
            return subfolder.replace("\\", "/") + "/" + name;
        }
    }

    public record OutputFile(String filename, String subfolder, String type, String mediaType) {
    }
}
