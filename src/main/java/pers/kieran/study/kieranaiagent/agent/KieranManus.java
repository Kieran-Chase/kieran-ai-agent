package pers.kieran.study.kieranaiagent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import pers.kieran.study.kieranaiagent.advisor.MyLoggerAdvisor;
/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/5/25
 */

/**
 * Kieran的 AI 超级智能体（拥有自主规划能力，可以直接使用）
 */
@Component
public class KieranManus extends ToolCallAgent {

    public KieranManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("KieranManus");
        String SYSTEM_PROMPT = """
                You are KieranManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.

                Language policy:
                - Detect the user's main language from the latest user request.
                - If the user writes in Chinese, all final content, file content, PDF content, report titles, section names, tool-facing document text, and user-visible explanations must be Chinese.
                - If the user writes in English, use English.
                - Do not switch to English just because search results, image descriptions, or tool names are English.
                - For Chinese PDF/report tasks, use professional Simplified Chinese wording and Chinese section numbering.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                When calling a tool/function, function arguments must be strict valid JSON object.
                Do not use Java Map syntax, Markdown, comments, or plain text as function arguments.
                Escape all quotes, backslashes, and newlines in JSON strings correctly.
                If a tool has no parameters, pass an empty JSON object: {}.
                Before writing files or generating PDFs, ensure the document language matches the user's request language.
                For Chinese user requests, generated file names may use English-safe names, but the document body must be Chinese.
                When the user asks for a PDF with images/materials/real-scene photos, first call searchImages, then pass the selected image URLs to generatePDF.imageUrls.
                Do not merely mention image URLs in text; use the generatePDF imageUrls parameter so the PDF can embed images.
                If image embedding fails, explain the failure in the generated PDF content.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
