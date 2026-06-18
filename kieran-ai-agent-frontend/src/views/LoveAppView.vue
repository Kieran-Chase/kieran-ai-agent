<template>
  <ChatRoom
    title="AI 恋爱大师"
    subtitle="Love App"
    welcomeTitle="欢迎来到恋爱咨询聊天室"
    welcomeText="你可以描述当前关系、聊天困惑或约会问题，AI 会实时给出建议。"
    :chat-id="chatId"
    :create-sse-url="createSseUrl"
  />
</template>

<script setup>
import ChatRoom from '../components/ChatRoom.vue'
import { createLoveAppSseUrl } from '../api/chat'
import { createChatId } from '../utils/chatId'

const chatId = createChatId()

function createSseUrl(message) {
  return createLoveAppSseUrl(message, chatId)
}
</script>
