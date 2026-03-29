import { ref, reactive, computed } from 'vue'
import { modelInstanceApi } from '@/api'

export const modelTypes = {
  text: { label: '文本模型', icon: 'text', color: 'purple' },
  image: { label: '图像模型', icon: 'image', color: 'cyan' },
  video: { label: '视频模型', icon: 'video', color: 'green' },
  audio: { label: '语音模型', icon: 'audio', color: 'orange' },
  vector: { label: '向量模型', icon: 'vector', color: 'blue' }
}

export const modelProviders = {
  text: [
    { value: 'openai', name: 'OpenAI', description: 'GPT-4o / GPT-4 Turbo' },
    { value: 'anthropic', name: 'Anthropic', description: 'Claude 3.5 Sonnet' },
    { value: 'ali', name: '阿里云', description: '通义千问' },
    { value: 'baidu', name: '百度', description: '文心一言' },
    { value: 'moonshot', name: 'Moonshot', description: 'Kimi' },
    { value: 'deepseek', name: 'DeepSeek', description: 'DeepSeek Chat' }
  ],
  image: [
    { value: 'comfyui', name: 'ComfyUI', description: '本地工作流执行与模型编排' },
    { value: 'openai', name: 'OpenAI', description: 'DALL-E 3' },
    { value: 'midjourney', name: 'Midjourney', description: '艺术风格生成' },
    { value: 'stability', name: 'Stability AI', description: 'Stable Diffusion' },
    { value: 'ali', name: '阿里云', description: '通义万相' },
    { value: 'tencent', name: '腾讯', description: '混元图像生成' },
    { value: 'kuaishou', name: '快手', description: '可图' }
  ],
  video: [
    { value: 'comfyui', name: 'ComfyUI', description: '本地视频工作流与异步轮询' },
    { value: 'kling', name: 'Kling', description: '快手视频生成' },
    { value: 'sora', name: 'Sora', description: 'OpenAI 视频生成' },
    { value: 'pika', name: 'Pika Labs', description: '创意视频生成' },
    { value: 'runway', name: 'Runway', description: 'Gen-2' },
    { value: 'luma', name: 'Luma', description: 'Dream Machine' },
    { value: 'kuaishou', name: '快手', description: '可灵' }
  ],
  audio: [
    { value: 'azure', name: 'Azure', description: 'Azure TTS' },
    { value: 'elevenlabs', name: 'ElevenLabs', description: '语音合成' },
    { value: 'ali', name: '阿里云', description: '语音合成' },
    { value: 'baidu', name: '百度', description: '语音合成' },
    { value: 'openai', name: 'OpenAI', description: 'OpenAI TTS' }
  ],
  vector: [
    { value: 'openai', name: 'OpenAI', description: 'text-embedding-3' },
    { value: 'ali', name: 'Ali', description: 'Embedding model' },
    { value: 'bge', name: 'BGE', description: 'Open source embedding' }
  ]
}

export const sceneCodeOptions = {
  text: [
    { value: 'SCENE_GEN', label: '场景生成' },
    { value: 'INFO_EXTRACT', label: '信息提取' },
    { value: 'STORYBOARD_GEN', label: '分镜生成' },
    { value: 'FIRST_FRAME_GEN', label: '首帧提示词' },
    { value: 'VIDEO_GEN', label: '视频提示词' }
  ],
  image: [
    { value: 'CHARACTER_GEN', label: '角色生成' },
    { value: 'SCENE_GEN', label: '场景生成' },
    { value: 'FIRST_FRAME_GEN', label: '首帧生成' }
  ],
  video: [
    { value: 'VIDEO_GEN', label: '视频生成' }
  ],
  audio: [
    { value: 'AUDIO_GEN', label: '音频生成' }
  ],
  vector: [
    { value: 'INFO_EXTRACT', label: '信息提取' }
  ]
}

export const resolutionOptions = [
  { value: '1024x1024', label: '1024 x 1024 (1:1)' },
  { value: '1280x720', label: '1280 x 720 (16:9)' },
  { value: '720x1280', label: '720 x 1280 (9:16)' },
  { value: '1024x768', label: '1024 x 768 (4:3)' },
  { value: '1920x1080', label: '1920 x 1080 (Full HD)' }
]

export const fpsOptions = [
  { value: 24, label: '24 fps' },
  { value: 30, label: '30 fps' },
  { value: 60, label: '60 fps' }
]

export const voiceOptions = {
  'zh-CN': [
    { value: 'zh-CN-Xiaoxiao', label: '晓晓' },
    { value: 'zh-CN-Yunxi', label: '云希' },
    { value: 'zh-CN-Yunyang', label: '云扬' },
    { value: 'zh-CN-Xiaohan', label: '晓涵' },
    { value: 'zh-CN-Xiaorui', label: '晓睿' }
  ],
  'en-US': [
    { value: 'en-US-Aria', label: 'Aria' },
    { value: 'en-US-Guy', label: 'Guy' },
    { value: 'en-US-Jenny', label: 'Jenny' }
  ]
}

export const durationOptions = [
  { value: 3, label: '3 秒' },
  { value: 5, label: '5 秒' },
  { value: 10, label: '10 秒' }
]

export const qualityOptions = [
  { value: 'standard', label: '标准' },
  { value: 'high', label: '高清' },
  { value: 'ultra', label: '超清' }
]

export const mapTypeToApi = (type) => {
  const mapping = {
    text: 'TEXT',
    image: 'IMAGE',
    video: 'VIDEO',
    audio: 'AUDIO',
    vector: 'VECTOR'
  }
  return mapping[type] || type
}

export const mapApiToType = (apiType) => {
  const mapping = {
    TEXT: 'text',
    IMAGE: 'image',
    VIDEO: 'video',
    AUDIO: 'audio',
    VECTOR: 'vector'
  }
  return mapping[String(apiType || '').toUpperCase()] || String(apiType || 'text').toLowerCase()
}

export const buildDefaultLookupKey = (apiType, sceneCode = '') => {
  const normalizedType = String(apiType || '').toUpperCase()
  return sceneCode ? `${normalizedType}:${sceneCode}` : normalizedType
}

const createDefaultConfig = (type) => {
  const common = {
    provider: '',
    modelDefId: null,
    apiKey: '',
    model: '',
    baseUrl: '',
    path: '',
    sceneCode: 'SCENE_GEN'
  }

  switch (type) {
    case 'text':
      return {
        ...common,
        temperature: 0.7,
        max_tokens: 4000,
        sceneCode: 'SCENE_GEN'
      }
    case 'image':
      return {
        ...common,
        resolution: '1024x1024',
        quality: 'standard',
        workflowMode: 'auto',
        workflowPath: '',
        apiWorkflowPath: '',
        inputMappingText: '',
        outputMappingText: '',
        pollIntervalMs: 3000,
        historyTimeoutMs: 180000,
        sceneCode: 'CHARACTER_GEN'
      }
    case 'video':
      return {
        ...common,
        fps: 30,
        duration: 5,
        workflowMode: 'auto',
        workflowPath: '',
        apiWorkflowPath: '',
        inputMappingText: '',
        outputMappingText: '',
        pollIntervalMs: 3000,
        historyTimeoutMs: 180000,
        sceneCode: 'VIDEO_GEN'
      }
    case 'audio':
      return {
        ...common,
        voice: 'zh-CN-Xiaoxiao',
        speed: 1.0,
        sceneCode: 'AUDIO_GEN'
      }
    case 'vector':
      return {
        ...common,
        dimension: 1024,
        metric: 'cosine',
        topK: 10,
        sceneCode: 'INFO_EXTRACT'
      }
    default:
      return { ...common }
  }
}

const defaultConfigs = {
  text: createDefaultConfig('text'),
  image: createDefaultConfig('image'),
  video: createDefaultConfig('video'),
  audio: createDefaultConfig('audio'),
  vector: createDefaultConfig('vector')
}

const cloneDefaultConfig = (type) => JSON.parse(JSON.stringify(defaultConfigs[type] || {}))

const getDefaultSceneCode = (apiType) => {
  const defaults = {
    TEXT: 'SCENE_GEN',
    IMAGE: 'CHARACTER_GEN',
    VIDEO: 'VIDEO_GEN',
    AUDIO: 'AUDIO_GEN',
    VECTOR: 'INFO_EXTRACT'
  }
  return defaults[apiType] || 'SCENE_GEN'
}

const tryParseJson = (text) => {
  if (!text || typeof text !== 'string') return null
  try {
    return JSON.parse(text)
  } catch (err) {
    throw new Error('JSON 配置格式不正确，请检查工作流映射')
  }
}

const stringifyJson = (value) => {
  if (!value) return ''
  return JSON.stringify(value, null, 2)
}

export function useModelConfig() {
  const instances = ref([])
  const instanceTotals = reactive({ text: 0, image: 0, video: 0, audio: 0, vector: 0 })
  const instancesLoading = ref(false)
  const instancesError = ref(null)
  const saving = ref(false)
  const testing = ref(null)

  const currentConfig = reactive({
    id: null,
    type: 'text',
    name: '',
    isDefault: false,
    config: cloneDefaultConfig('text')
  })

  const expandedAdvanced = reactive({})

  const textInstances = computed(() => instances.value.filter(i => i.type === 'text'))
  const imageInstances = computed(() => instances.value.filter(i => i.type === 'image'))
  const videoInstances = computed(() => instances.value.filter(i => i.type === 'video'))
  const audioInstances = computed(() => instances.value.filter(i => i.type === 'audio'))
  const vectorInstances = computed(() => instances.value.filter(i => i.type === 'vector'))
  const defaultInstance = computed(() => instances.value.find(i => i.isDefault))
  const isEditing = computed(() => !!currentConfig.id)

  const transformInstance = (item) => {
    const type = mapApiToType(item.modelType)
    let params = {}
    try {
      params = item.params
        ? (typeof item.params === 'string' ? JSON.parse(item.params) : item.params)
        : {}
    } catch (err) {
      params = {}
    }

    return {
      id: item.id,
      type,
      name: item.instanceName,
      isDefault: false,
      defaultScope: '',
      sceneCode: item.sceneCode || '',
      config: {
        ...cloneDefaultConfig(type),
        provider: item.providerCode || 'custom',
        modelDefId: item.modelDefId || null,
        apiKey: item.apiKey || '',
        model: item.modelCode || '',
        baseUrl: item.baseUrl || '',
        path: item.path || '',
        sceneCode: item.sceneCode || getDefaultSceneCode(mapTypeToApi(type)),
        ...params,
        inputMappingText: stringifyJson(params.inputMapping),
        outputMappingText: stringifyJson(params.outputMapping)
      }
    }
  }

  const transformToApi = (uiData) => {
    const apiType = mapTypeToApi(uiData.type)
    const config = uiData.config || {}
    const params = {}

    if (uiData.type === 'text') {
      params.temperature = Number(config.temperature ?? 0.7)
      params.max_tokens = Number(config.max_tokens ?? 4000)
    }

    if (uiData.type === 'image') {
      params.resolution = config.resolution || '1024x1024'
      params.quality = config.quality || 'standard'
    }

    if (uiData.type === 'video') {
      params.fps = Number(config.fps ?? 30)
      params.duration = Number(config.duration ?? 5)
    }

    if (uiData.type === 'audio') {
      params.voice = config.voice || 'zh-CN-Xiaoxiao'
      params.speed = Number(config.speed ?? 1.0)
    }

    if (uiData.type === 'image' || uiData.type === 'video') {
      if (config.workflowMode) params.workflowMode = config.workflowMode
      if (config.workflowPath) params.workflowPath = config.workflowPath.trim()
      if (config.apiWorkflowPath) params.apiWorkflowPath = config.apiWorkflowPath.trim()
      if (config.pollIntervalMs != null && config.pollIntervalMs !== '') {
        params.pollIntervalMs = Number(config.pollIntervalMs)
      }
      if (config.historyTimeoutMs != null && config.historyTimeoutMs !== '') {
        params.historyTimeoutMs = Number(config.historyTimeoutMs)
      }
      const inputMapping = tryParseJson(config.inputMappingText)
      const outputMapping = tryParseJson(config.outputMappingText)
      if (inputMapping) params.inputMapping = inputMapping
      if (outputMapping) params.outputMapping = outputMapping
    }

    return {
      instanceName: uiData.name,
      modelDefId: config.modelDefId || null,
      modelType: apiType,
      modelCode: config.model || '',
      sceneCode: config.sceneCode || getDefaultSceneCode(apiType),
      apiKey: config.apiKey || '',
      path: config.path || '',
      params,
      status: 1
    }
  }

  const getDefaultIdFromValue = (value) => {
    if (!value) return null
    return value.modelInstanceId ?? value.instanceId ?? value.id ?? null
  }

  const applyDefaultFlags = (defaultMap = {}) => {
    instances.value.forEach((instance) => {
      const apiType = mapTypeToApi(instance.type)
      const sceneCode = instance.config?.sceneCode || instance.sceneCode || ''
      const sceneKey = buildDefaultLookupKey(apiType, sceneCode)
      const typeKey = buildDefaultLookupKey(apiType)
      const sceneDefaultId = getDefaultIdFromValue(defaultMap[sceneKey])
      const typeDefaultId = getDefaultIdFromValue(defaultMap[typeKey])
      instance.isDefault = false
      instance.defaultScope = ''

      if (sceneDefaultId && String(instance.id) === String(sceneDefaultId)) {
        instance.isDefault = true
        instance.defaultScope = sceneCode || apiType
        return
      }

      if (typeDefaultId && String(instance.id) === String(typeDefaultId)) {
        instance.isDefault = true
        instance.defaultScope = apiType
      }
    })
  }

  const loadInstances = async (type = 'text', page = 1, size = 6) => {
    instancesLoading.value = true
    instancesError.value = null
    try {
      const apiType = mapTypeToApi(type)
      const response = await modelInstanceApi.list(apiType, { page, size })
      const records = response.data?.records || response.data?.modelInstances || response.data || []
      instances.value = Array.isArray(records) ? records.map(transformInstance) : []
      instanceTotals[type] = response.data?.total ?? instances.value.length

      try {
        const defaultsRes = await modelInstanceApi.getAllDefaults()
        applyDefaultFlags(defaultsRes?.data || {})
      } catch (err) {
        console.warn('load default models failed:', err)
      }
    } catch (err) {
      console.error('load model instances failed:', err)
      instancesError.value = err.message || '加载失败'
      instances.value = []
      instanceTotals[type] = 0
    } finally {
      instancesLoading.value = false
    }
  }

  const loadInstanceTotals = async () => {
    await Promise.all(['TEXT', 'IMAGE', 'VIDEO', 'AUDIO', 'VECTOR'].map(async (apiType) => {
      try {
        const response = await modelInstanceApi.list(apiType, { page: 1, size: 1 })
        const key = mapApiToType(apiType)
        instanceTotals[key] = response.data?.total ?? (response.data?.records || []).length ?? 0
      } catch (err) {
        instanceTotals[mapApiToType(apiType)] = 0
      }
    }))
  }

  const createInstance = async (data) => {
    saving.value = true
    try {
      return await modelInstanceApi.create(data)
    } finally {
      saving.value = false
    }
  }

  const updateInstance = async (id, data) => {
    saving.value = true
    try {
      return await modelInstanceApi.update(id, data)
    } finally {
      saving.value = false
    }
  }

  const deleteInstance = async (id) => {
    await modelInstanceApi.delete(id)
    instances.value = instances.value.filter(i => i.id !== id)
    if (currentConfig.id === id) {
      resetCurrent()
    }
  }

  const setAsDefault = async (id, sceneCode = null) => {
    const instance = instances.value.find(i => i.id === id)
    if (!instance) return

    await modelInstanceApi.updateDefault({
      modelType: mapTypeToApi(instance.type),
      modelInstanceId: id,
      sceneCode: sceneCode || instance.config?.sceneCode || instance.sceneCode || null
    })

    try {
      const defaultsRes = await modelInstanceApi.getAllDefaults()
      applyDefaultFlags(defaultsRes?.data || {})
    } catch (err) {
      console.warn('reload default models failed:', err)
    }
  }

  const duplicateInstance = async (id) => {
    return await modelInstanceApi.duplicate(id)
  }

  const testConnection = async (id) => {
    testing.value = id
    try {
      const response = await modelInstanceApi.test(id)
      return response.data || { success: true, message: '连接成功' }
    } catch (err) {
      return { success: false, message: err.message || '连接失败' }
    } finally {
      testing.value = null
    }
  }

  const getInstancesByType = (type) => instances.value.filter(i => i.type === type)

  const getProviders = (type) => modelProviders[type] || []

  const getProviderName = (type, providerValue) => {
    const provider = getProviders(type).find(p => p.value === providerValue)
    return provider ? provider.name : providerValue
  }

  const getProviderDesc = () => {
    const provider = getProviders(currentConfig.type).find(p => p.value === currentConfig.config.provider)
    return provider ? provider.description : ''
  }

  const getVoices = (locale = 'zh-CN') => voiceOptions[locale] || []

  const selectInstance = (instance) => {
    currentConfig.id = instance.id
    currentConfig.type = instance.type
    currentConfig.name = instance.name
    currentConfig.isDefault = !!instance.isDefault
    currentConfig.config = {
      ...cloneDefaultConfig(instance.type),
      ...instance.config
    }
  }

  const handleCreateNew = (type = currentConfig.type) => {
    currentConfig.id = null
    currentConfig.type = type
    currentConfig.name = ''
    currentConfig.isDefault = false
    currentConfig.config = cloneDefaultConfig(type)
  }

  const resetCurrent = () => {
    handleCreateNew(currentConfig.type)
  }

  const toggleAdvanced = (type) => {
    expandedAdvanced[type] = !expandedAdvanced[type]
  }

  const saveConfig = async () => {
    if (!currentConfig.name.trim()) {
      throw new Error('请输入实例名称')
    }
    const payload = transformToApi({
      name: currentConfig.name,
      type: currentConfig.type,
      isDefault: currentConfig.isDefault,
      config: currentConfig.config
    })

    if (currentConfig.id) {
      await updateInstance(currentConfig.id, payload)
    } else {
      await createInstance(payload)
    }
  }

  const getDefaultConfig = (type) => cloneDefaultConfig(type)

  const init = async () => {
    await loadInstances('text')
    await loadInstanceTotals()
    Object.keys(modelTypes).forEach((key) => {
      expandedAdvanced[key] = false
    })
  }

  return {
    instances,
    instanceTotals,
    instancesLoading,
    instancesError,
    saving,
    testing,
    currentConfig,
    expandedAdvanced,
    textInstances,
    imageInstances,
    videoInstances,
    audioInstances,
    vectorInstances,
    defaultInstance,
    isEditing,
    loadInstances,
    loadInstanceTotals,
    createInstance,
    updateInstance,
    deleteInstance,
    setAsDefault,
    duplicateInstance,
    testConnection,
    getInstancesByType,
    getProviders,
    getProviderName,
    getProviderDesc,
    getVoices,
    selectInstance,
    handleCreateNew,
    resetCurrent,
    toggleAdvanced,
    saveConfig,
    getDefaultConfig,
    init
  }
}

export default useModelConfig
