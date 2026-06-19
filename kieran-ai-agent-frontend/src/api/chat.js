import http from './http'

export const apiPrefix = http.defaults.baseURL || '/api'

export function createLoveAppSseUrl(message, chatId) {
  const params = new URLSearchParams({ message, chatId })
  return `${apiPrefix}/ai/love_app/chat/sse?${params.toString()}`
}

export function createReplyCoachSseUrl(message, chatId) {
  const params = new URLSearchParams({ message, chatId })
  return `${apiPrefix}/ai/reply_coach/chat/sse?${params.toString()}`
}

export function createManusSseUrl(message) {
  const params = new URLSearchParams({ message })
  return `${apiPrefix}/ai/manus/chat?${params.toString()}`
}

export default http
