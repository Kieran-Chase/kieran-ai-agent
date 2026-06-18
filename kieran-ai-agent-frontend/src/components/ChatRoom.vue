<template>
  <main class="chat-page">
    <section class="chat-shell">
      <header class="chat-header">
        <button class="back-button" type="button" @click="goHome">← 应用中心</button>
        <div>
          <p class="eyebrow">{{ subtitle }}</p>
          <h1>{{ title }}</h1>
        </div>
        <div v-if="chatId" class="session-id" :title="chatId">会话 {{ shortChatId }}</div>
      </header>

      <div v-if="streamDebug.visible" class="stream-debug">
        <span>请求：{{ streamDebug.status }}</span>
        <span>分块：{{ streamDebug.chunkCount }}</span>
        <span>首包：{{ streamDebug.firstChunkMs || '-' }}ms</span>
        <span>最后包：{{ streamDebug.lastChunkSize }} 字符</span>
      </div>

      <div ref="messageListRef" class="message-list">
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-icon">✨</div>
          <h2>{{ welcomeTitle }}</h2>
          <p>{{ welcomeText }}</p>
        </div>

        <article
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="message.role"
        >
          <div class="avatar">{{ message.role === 'user' ? '我' : 'AI' }}</div>
          <div class="message-bubble" :class="message.type">
            <template v-if="message.type === 'agent-step'">
              <div class="step-card-header">
                <span class="step-badge">{{ message.meta.stepLabel }}</span>
                <strong>{{ message.meta.title }}</strong>
              </div>
              <p class="step-summary">{{ message.meta.summary }}</p>
              <div v-if="message.meta.images?.length" class="image-preview-grid">
                <a
                  v-for="image in message.meta.images"
                  :key="image"
                  :href="image"
                  target="_blank"
                  rel="noreferrer"
                >
                  <img :src="image" alt="网络图片预览" loading="lazy" />
                </a>
              </div>
              <details v-if="message.meta.detail" class="step-detail">
                <summary>查看原始结果</summary>
                <pre>{{ message.meta.detail }}</pre>
              </details>
            </template>

            <template v-else-if="message.type === 'pdf-result'">
              <div class="pdf-card">
                <div class="pdf-icon">PDF</div>
                <div class="pdf-info">
                  <strong>任务文档 PDF 已生成</strong>
                  <p>{{ message.meta.fileName }}</p>
                  <small>{{ message.meta.filePath }}</small>
                </div>
              </div>
              <div v-if="message.meta.url" class="pdf-actions">
                <a :href="message.meta.url" target="_blank" rel="noreferrer">下载 PDF</a>
                <a :href="message.meta.url" target="_blank" rel="noreferrer">预览 PDF</a>
              </div>
              <p v-else class="pdf-tip">当前后端返回的是服务器本地路径，若要在网页中下载或预览，需要后端提供文件访问 URL。</p>
            </template>

            <p v-else>{{ message.content || '思考中...' }}</p>
            <span v-if="message.loading" class="typing-dot">正在输入</span>
          </div>
        </article>
      </div>

      <form class="chat-input" @submit.prevent="sendMessage">
        <textarea
          v-model.trim="inputMessage"
          :disabled="isStreaming"
          rows="1"
          placeholder="请输入你的问题，按 Enter 发送，Shift + Enter 换行"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <button type="submit" :disabled="!canSend">
          {{ isStreaming ? '回复中...' : '发送' }}
        </button>
        <button v-if="isStreaming" class="stop-button" type="button" @click="stopStreaming">
          停止
        </button>
      </form>
    </section>
  </main>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    required: true,
  },
  welcomeTitle: {
    type: String,
    required: true,
  },
  welcomeText: {
    type: String,
    required: true,
  },
  chatId: {
    type: String,
    default: '',
  },
  createSseUrl: {
    type: Function,
    required: true,
  },
  responseMode: {
    type: String,
    default: 'single',
    validator: (value) => ['single', 'steps'].includes(value),
  },
})

const router = useRouter()
const inputMessage = ref('')
const isStreaming = ref(false)
const messages = ref([])
const messageListRef = ref(null)
const streamDebug = ref({
  visible: false,
  status: 'idle',
  startTime: 0,
  chunkCount: 0,
  firstChunkMs: 0,
  lastChunkSize: 0,
})
let streamRequest = null
let renderTimer = null
let streamBuffer = ''
let activeAiMessage = null
let streamEnded = false
let pendingSseLine = ''
let isReadingDataLine = false
let currentStepDataLines = []
let currentRequestId = 0

const renderInterval = 16
const charsPerTick = 2

const shortChatId = computed(() => {
  if (!props.chatId) {
    return ''
  }

  return props.chatId.length > 12 ? `${props.chatId.slice(0, 8)}...` : props.chatId
})

const canSend = computed(() => inputMessage.value.length > 0 && !isStreaming.value)
const isStepMode = computed(() => props.responseMode === 'steps')

function goHome() {
  router.push('/')
}

function scrollToBottom() {
  nextTick(() => {
    const messageList = messageListRef.value
    if (messageList) {
      messageList.scrollTop = messageList.scrollHeight
    }
  })
}

function appendMessage(role, content = '', loading = false, options = {}) {
  const message = {
    id: `${role}_${Date.now()}_${Math.random().toString(16).slice(2)}`,
    role,
    content,
    loading,
    type: options.type || 'text',
    meta: options.meta || {},
  }

  messages.value.push(message)
  scrollToBottom()
  return message
}

function normalizeSseData(data) {
  if (!data || data.trim() === '[DONE]') {
    return ''
  }

  return data.replace(/\r?\n/g, '\n')
}

function logStreamDebug(message, detail) {
  const enabled = import.meta.env.DEV || localStorage.getItem('debugSse') === '1'
  if (enabled) {
    console.debug(`[SSE] ${message}`, detail ?? '')
  }
}

function resetStreamDebug() {
  streamDebug.value = {
    visible: true,
    status: '连接中',
    startTime: performance.now(),
    chunkCount: 0,
    firstChunkMs: 0,
    lastChunkSize: 0,
  }
}

function markStreamChunk(chunk) {
  const nextChunkCount = streamDebug.value.chunkCount + 1
  streamDebug.value = {
    ...streamDebug.value,
    status: '接收中',
    chunkCount: nextChunkCount,
    firstChunkMs:
      streamDebug.value.firstChunkMs || Math.round(performance.now() - streamDebug.value.startTime),
    lastChunkSize: chunk.length,
  }
}

function markStreamDone(status = '完成') {
  streamDebug.value = {
    ...streamDebug.value,
    status,
  }
}

function abortRequest() {
  if (streamRequest) {
    streamRequest.abort()
    streamRequest = null
  }
}

function stopTypewriter() {
  if (renderTimer) {
    clearInterval(renderTimer)
    renderTimer = null
  }
}

function finishStreaming(aiMessage) {
  if (!aiMessage) {
    isStreaming.value = false
    activeAiMessage = null
    streamEnded = false
    scrollToBottom()
    return
  }

  if (streamBuffer.length > 0) {
    streamEnded = true
    activeAiMessage = aiMessage
    startTypewriter()
    return
  }

  aiMessage.loading = false
  isStreaming.value = false
  activeAiMessage = null
  streamEnded = false
  scrollToBottom()
}

function startTypewriter() {
  if (renderTimer) {
    return
  }

  renderTimer = setInterval(() => {
    if (!activeAiMessage) {
      stopTypewriter()
      return
    }

    if (streamBuffer.length > 0) {
      activeAiMessage.content += streamBuffer.slice(0, charsPerTick)
      streamBuffer = streamBuffer.slice(charsPerTick)
      activeAiMessage.loading = false
      scrollToBottom()
      return
    }

    stopTypewriter()
    if (streamEnded) {
      finishStreaming(activeAiMessage)
    }
  }, renderInterval)
}

function enqueueStreamText(text, aiMessage) {
  const normalizedText = normalizeSseData(text)
  if (!normalizedText) {
    return
  }

  streamBuffer += normalizedText
  activeAiMessage = aiMessage
  startTypewriter()
}

function findLineBreakIndex(text) {
  const lineFeedIndex = text.indexOf('\n')
  const carriageReturnIndex = text.indexOf('\r')

  if (lineFeedIndex === -1) {
    return carriageReturnIndex
  }

  if (carriageReturnIndex === -1) {
    return lineFeedIndex
  }

  return Math.min(lineFeedIndex, carriageReturnIndex)
}

function consumeLineBreak(text, lineBreakIndex) {
  let nextIndex = lineBreakIndex + 1
  if (text[lineBreakIndex] === '\r' && text[nextIndex] === '\n') {
    nextIndex += 1
  }

  return text.slice(nextIndex)
}

function consumeCurrentDataLine(aiMessage) {
  const lineBreakIndex = findLineBreakIndex(pendingSseLine)
  const dataPart = lineBreakIndex === -1 ? pendingSseLine : pendingSseLine.slice(0, lineBreakIndex)

  enqueueStreamText(dataPart, aiMessage)

  if (lineBreakIndex === -1) {
    pendingSseLine = ''
    return false
  }

  pendingSseLine = consumeLineBreak(pendingSseLine, lineBreakIndex)
  isReadingDataLine = false
  return true
}

function isPartialSseField(text) {
  return ['data:', 'event:', 'id:', 'retry:'].some((field) => field.startsWith(text))
}

function handleSseLine(line, aiMessage) {
  if (!line || line.startsWith(':')) {
    return
  }

  if (!line.startsWith('data:')) {
    return
  }

  const text = normalizeSseData(line.slice(5).replace(/^ /, ''))
  if (text) {
    enqueueStreamText(text, aiMessage)
  }
}

function parseToolResult(content) {
  const match = content.match(/^Step\s+(\d+)[:：]\s*工具\s+([^\s]+)\s+返回的结果[:：]\s*([\s\S]*)$/)
  if (!match) {
    return null
  }

  return {
    step: match[1],
    toolName: match[2],
    result: match[3].trim(),
  }
}

function stripResultQuotes(result) {
  const trimmed = result.trim()
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith('“') && trimmed.endsWith('”'))
  ) {
    try {
      return JSON.parse(trimmed)
    } catch {
      return trimmed.slice(1, -1)
    }
  }

  return trimmed
}

function summarizeJsonSearchResult(result) {
  try {
    const parsed = JSON.parse(stripResultQuotes(result))
    const items = Array.isArray(parsed) ? parsed : [parsed]
    return items
      .slice(0, 3)
      .map((item, index) => `${index + 1}. ${item.title || item.snippet || item.link || '搜索结果'}`)
      .join('\n')
  } catch {
    return ''
  }
}

function extractImageUrls(result) {
  const text = stripResultQuotes(result)
  return [...text.matchAll(/URL:\s*(https?:\/\/\S+)/g)]
    .map((match) => match[1])
    .slice(0, 6)
}
function summarizeImageResult(result) {
  const text = stripResultQuotes(result)
  if (/No images found/i.test(text)) {
    return '未找到匹配图片，智能体会尝试更换关键词继续搜索。'
  }

  const count = (text.match(/URL:/g) || []).length
  return `找到 ${count || '多'} 张候选网络图片，已用于后续任务素材筛选。`
}

function parsePdfResult(result) {
  const text = stripResultQuotes(result)
  try {
    const parsed = JSON.parse(text)
    if (parsed?.type === 'pdf') {
      return {
        filePath: parsed.filePath || '',
        fileName: parsed.fileName || '任务文档.pdf',
        url: parsed.url || '',
      }
    }
  } catch {
    // 兼容旧版后端返回的纯文本路径
  }

  const match = text.match(/PDF generated successfully to:\s*(.+)$/i)
  if (!match) {
    return null
  }

  const filePath = match[1].trim()
  const fileName = filePath.split(/[\\/]/).pop() || '任务文档.pdf'
  return { filePath, fileName, url: '' }
}

function createStepMessage(content) {
  const toolResult = parseToolResult(content)
  if (!toolResult) {
    return {
      content,
      options: {
        type: 'agent-step',
        meta: {
          stepLabel: '思考',
          title: '智能体进展',
          summary: content,
          detail: '',
        },
      },
    }
  }

  const { step, toolName, result } = toolResult
  const pdfInfo = toolName === 'generatePDF' ? parsePdfResult(result) : null

  if (pdfInfo) {
    return {
      content: stripResultQuotes(result),
      options: {
        type: 'pdf-result',
        meta: pdfInfo,
      },
    }
  }

  const summaries = {
    searchWeb: summarizeJsonSearchResult(result) || '已完成网页搜索，获取到候选资料和参考信息。',
    scrapeWebPage: '已读取网页详情，提取点位、地址、开放信息和参考资料。',
    searchImages: summarizeImageResult(result),
    writeFile: '已整理任务文档草稿，并保存到服务器临时文件。',
    readFile: '已读取任务文档草稿，准备生成最终 PDF。',
    getWeather: `已获取天气信息：${stripResultQuotes(result)}`,
    doTerminate: '任务已完成。',
    generatePDF: stripResultQuotes(result),
  }

  return {
    content: stripResultQuotes(result),
    options: {
      type: 'agent-step',
      meta: {
        stepLabel: `Step ${step}`,
        title: `工具：${toolName}`,
        summary: summaries[toolName] || stripResultQuotes(result),
        detail: stripResultQuotes(result),
        images: toolName === 'searchImages' ? extractImageUrls(result) : [],
      },
    },
  }
}

function emitStepMessage() {
  const text = normalizeSseData(currentStepDataLines.join('\n'))
  currentStepDataLines = []

  if (text) {
    const stepMessage = createStepMessage(text)
    appendMessage('assistant', stepMessage.content, false, stepMessage.options)
  }
}

function handleStepLine(line) {
  if (!line) {
    emitStepMessage()
    return
  }

  if (line.startsWith(':')) {
    return
  }

  if (line.startsWith('data:')) {
    currentStepDataLines.push(line.slice(5).replace(/^ /, ''))
    return
  }

  if (line.startsWith('event:') || line.startsWith('id:') || line.startsWith('retry:')) {
    return
  }

  currentStepDataLines.push(line)
}

function handleStepStreamChunk(chunk) {
  pendingSseLine += chunk

  while (pendingSseLine) {
    const lineBreakIndex = findLineBreakIndex(pendingSseLine)
    if (lineBreakIndex === -1) {
      return
    }

    const line = pendingSseLine.slice(0, lineBreakIndex)
    pendingSseLine = consumeLineBreak(pendingSseLine, lineBreakIndex)
    handleStepLine(line)
  }
}

function handleStreamChunk(chunk, aiMessage) {
  logStreamDebug(`chunk ${chunk.length} chars`, chunk)
  markStreamChunk(chunk)

  if (isStepMode.value) {
    handleStepStreamChunk(chunk)
    return
  }

  pendingSseLine += chunk

  while (pendingSseLine) {
    if (isReadingDataLine) {
      const completed = consumeCurrentDataLine(aiMessage)
      if (!completed) {
        return
      }
      continue
    }

    if (pendingSseLine.startsWith('\n') || pendingSseLine.startsWith('\r')) {
      pendingSseLine = consumeLineBreak(pendingSseLine, 0)
      continue
    }

    if (pendingSseLine.startsWith(':')) {
      const lineBreakIndex = findLineBreakIndex(pendingSseLine)
      if (lineBreakIndex === -1) {
        return
      }
      pendingSseLine = consumeLineBreak(pendingSseLine, lineBreakIndex)
      continue
    }

    if (pendingSseLine.startsWith('data:')) {
      pendingSseLine = pendingSseLine.slice(5).replace(/^ /, '')
      isReadingDataLine = true
      continue
    }

    if (
      pendingSseLine.startsWith('event:') ||
      pendingSseLine.startsWith('id:') ||
      pendingSseLine.startsWith('retry:')
    ) {
      const lineBreakIndex = findLineBreakIndex(pendingSseLine)
      if (lineBreakIndex === -1) {
        return
      }
      pendingSseLine = consumeLineBreak(pendingSseLine, lineBreakIndex)
      continue
    }

    if (isPartialSseField(pendingSseLine)) {
      return
    }

    enqueueStreamText(pendingSseLine, aiMessage)
    pendingSseLine = ''
  }
}

function flushPendingStream(aiMessage) {
  if (isStepMode.value) {
    if (pendingSseLine) {
      handleStepLine(pendingSseLine)
      pendingSseLine = ''
    }
    emitStepMessage()
    return
  }

  if (pendingSseLine) {
    if (isReadingDataLine) {
      consumeCurrentDataLine(aiMessage)
    } else {
      handleSseLine(pendingSseLine, aiMessage)
    }
    pendingSseLine = ''
    isReadingDataLine = false
  }
}

function stopStreaming() {
  streamEnded = true
  abortRequest()
  if (activeAiMessage) {
    activeAiMessage.loading = false
  }
  isStreaming.value = false
}

function requestStream(url, requestId, aiMessage) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    let receivedLength = 0

    streamRequest = xhr
    xhr.open('GET', url, true)
    xhr.timeout = 0
    xhr.setRequestHeader('Accept', 'text/event-stream')
    xhr.overrideMimeType?.('text/event-stream; charset=utf-8')

    xhr.onprogress = () => {
      if (requestId !== currentRequestId) {
        xhr.abort()
        return
      }

      const chunk = xhr.responseText.slice(receivedLength)
      receivedLength = xhr.responseText.length

      if (chunk) {
        handleStreamChunk(chunk, aiMessage)
      }
    }

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        flushPendingStream(aiMessage)
        resolve()
        return
      }

      reject(new Error(`请求失败：${xhr.status}`))
    }

    xhr.onerror = () => reject(new Error('网络连接异常，请稍后重试。'))
    xhr.ontimeout = () => reject(new Error('请求超时，请稍后重试。'))
    xhr.onabort = () => resolve()
    xhr.send()
  })
}

async function sendMessage() {
  if (isStreaming.value || inputMessage.value.length === 0) {
    return
  }

  const requestId = currentRequestId + 1
  currentRequestId = requestId
  const currentMessage = inputMessage.value
  inputMessage.value = ''
  appendMessage('user', currentMessage)
  const aiMessage = isStepMode.value ? null : appendMessage('assistant', '', true)
  isStreaming.value = true
  streamBuffer = ''
  activeAiMessage = aiMessage
  streamEnded = false
  pendingSseLine = ''
  isReadingDataLine = false
  currentStepDataLines = []
  stopTypewriter()
  abortRequest()
  resetStreamDebug()
  const requestUrl = props.createSseUrl(currentMessage)
  logStreamDebug(`request ${requestId} start`, requestUrl)

  try {
    await requestStream(requestUrl, requestId, aiMessage)
    streamEnded = true
    markStreamDone()
    finishStreaming(aiMessage)
  } catch (error) {
    logStreamDebug(`request ${requestId} error`, error)
    markStreamDone('异常')
    if (isStepMode.value) {
      appendMessage('assistant', error.message || '连接中断，请稍后重试。', false)
    } else if (!aiMessage.content && streamBuffer.length === 0) {
      aiMessage.content = error.message || '连接中断，请稍后重试。'
    }
    streamEnded = true
    finishStreaming(aiMessage)
  } finally {
    streamRequest = null
  }
}

onBeforeUnmount(() => {
  stopTypewriter()
  abortRequest()
})
</script>


