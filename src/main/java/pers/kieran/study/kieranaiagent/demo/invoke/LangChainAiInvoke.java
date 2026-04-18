package pers.kieran.study.kieranaiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/3/24
 */
public class LangChainAiInvoke {
    public static void main(String[] args) {
        ChatLanguageModel qwenChatModel= QwenChatModel.builder()
                .apiKey(TestApiKey.API_KEY)
                .modelName("qwen-max")
                .build();
        String answer=qwenChatModel.chat("我是Kieran，正在学习langChain4j框架");
        System.out.println(answer);
    }

}
