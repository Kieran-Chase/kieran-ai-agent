<template>
  <main class="home-page">
    <section class="hero-card">
      <p class="eyebrow">Kieran AI Agent</p>
      <h1>AI 应用中心</h1>
      <p class="hero-desc">选择你想体验的智能应用，开启实时流式对话。</p>

      <div class="app-grid">
        <RouterLink class="app-card reply" to="/love-app">
          <span class="app-icon">💬</span>
          <div>
            <h2>AI 嘴替教练</h2>
            <p>拒绝、催促、道歉、谈判、职场回复，一键生成体面表达。</p>
          </div>
          <strong>进入 →</strong>
        </RouterLink>

        <RouterLink class="app-card agent" to="/manus">
          <span class="app-icon">🤖</span>
          <div>
            <h2>AI 超级智能体</h2>
            <p>调用工具、规划任务并实时反馈进度。</p>
          </div>
          <strong>进入 →</strong>
        </RouterLink>
      </div>
    </section>
  </main>
</template>
