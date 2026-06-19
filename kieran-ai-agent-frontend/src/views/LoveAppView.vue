<template>
  <ChatRoom
    title="AI 嘴替教练"
    subtitle="Reply Coach"
    welcomeTitle="欢迎来到嘴替教练聊天室"
    welcomeText="把你想说但不好开口的话、聊天场景或尴尬处境发给我，我帮你生成体面又好用的表达。"
    :chat-id="chatId"
    :create-sse-url="createSseUrl"
  />
</template>

<script setup>
import ChatRoom from '../components/ChatRoom.vue'
import { createReplyCoachSseUrl } from '../api/chat'
import { createChatId } from '../utils/chatId'

const chatId = createChatId()

function createSseUrl(message) {
  return createReplyCoachSseUrl(message, chatId)
}
</script>
