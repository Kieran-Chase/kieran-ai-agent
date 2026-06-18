export function createChatId() {
  if (window.crypto?.randomUUID) {
    return window.crypto.randomUUID()
  }

  return `chat_${Date.now()}_${Math.random().toString(16).slice(2)}`
}
