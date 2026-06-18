<template>
  <ChatRoom
    title="AI 超级智能体"
    subtitle="Manus Agent"
    welcomeTitle="欢迎来到超级智能体聊天室"
    welcomeText="输入任务目标，智能体会通过流式输出展示思考与执行过程。"
    responseMode="steps"
    :create-sse-url="createSseUrl"
  />
</template>

<script setup>
import ChatRoom from '../components/ChatRoom.vue'
import { createManusSseUrl } from '../api/chat'

function createSseUrl(message) {
  return createManusSseUrl(message)
}
</script>
