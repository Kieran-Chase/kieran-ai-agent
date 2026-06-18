package pers.kieran.study.kieranaiagent.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/5/25
 */

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.model.tool.ToolExecutionResult;
import pers.kieran.study.kieranaiagent.agent.model.AgentState;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 保存工具调用信息的响应结果（要调用那些工具）
    private ChatResponse toolCallChatResponse;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 使用 DashScope 专用配置传递工具 schema；开启 ProxyToolCalls，避免 DashScopeChatModel 自动 handleToolCalls 后递归请求模型。
        this.chatOptions = DashScopeChatOptions.builder()
                .withFunctionCallbacks(List.of(availableTools))
                .withProxyToolCalls(true)
                .build();

    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        // 1、校验提示词，拼接用户提示词
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        // 2、调用 AI 大模型，获取工具调用结果
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, this.chatOptions);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .call()
                    .chatResponse();
            // 记录响应，用于等下 Act
            this.toolCallChatResponse = chatResponse;
            // 3、解析工具调用结果，获取要调用的工具
            // 助手消息
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 获取要调用的工具列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // 输出提示信息
            String result = assistantMessage.getText();
            log.info(getName() + "的思考：" + result);
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            /*// 如果不需要调用工具，返回 false
            if (toolCallList.isEmpty()) {
                // 只有不调用工具时，才需要手动记录助手消息
                getMessageList().add(assistantMessage);
                return false;
            } else {
                // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
                return true;
            }*/
            //后面的无工具逻辑改一下，避免一直循环 20 次
            if (toolCallList.isEmpty()) {
                getMessageList().add(assistantMessage);
                setState(AgentState.FINISHED);
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题：" + e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到了错误：" + e.getMessage()));
            setState(AgentState.ERROR);
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具需要调用";
        }
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        // Keep compact tool results in model memory to reduce timeout risk.
        // The original tool response is still returned to SSE for frontend rendering.
        setMessageList(compactToolConversationHistory(toolExecutionResult.conversationHistory(), toolResponseMessage));
        // 判断是否调用了终止工具
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled) {
            // 任务结束，更改状态
            setState(AgentState.FINISHED);
        }
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 返回的结果：" + response.responseData())
                .collect(Collectors.joining("\n"));
        log.info(results);
        return results;
    }

    /**
     * Compact large tool outputs before writing them back into model context.
     * This keeps the full SSE payload for the frontend while reducing model timeout risk.
     */
    private List<Message> compactToolConversationHistory(List<Message> conversationHistory,
                                                          ToolResponseMessage originalToolResponseMessage) {
        List<Message> compactHistory = new ArrayList<>(conversationHistory.size());
        for (Message message : conversationHistory) {
            if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
                compactHistory.add(compactAssistantToolCalls(assistantMessage));
            } else {
                compactHistory.add(message);
            }
        }

        List<ToolResponseMessage.ToolResponse> compactResponses = originalToolResponseMessage.getResponses().stream()
                .map(response -> new ToolResponseMessage.ToolResponse(
                        response.id(),
                        response.name(),
                        compactToolResponse(response.name(), response.responseData())
                ))
                .toList();
        ToolResponseMessage compactToolResponseMessage = new ToolResponseMessage(compactResponses);
        compactHistory.set(compactHistory.size() - 1, compactToolResponseMessage);
        return compactHistory;
    }

    private AssistantMessage compactAssistantToolCalls(AssistantMessage assistantMessage) {
        List<AssistantMessage.ToolCall> compactToolCalls = assistantMessage.getToolCalls().stream()
                .map(toolCall -> new AssistantMessage.ToolCall(
                        toolCall.id(),
                        toolCall.type(),
                        toolCall.name(),
                        compactToolArguments(toolCall.name(), toolCall.arguments())
                ))
                .toList();
        return new AssistantMessage(assistantMessage.getText(), assistantMessage.getMetadata(), compactToolCalls,
                assistantMessage.getMedia());
    }

    private String compactToolArguments(String toolName, String arguments) {
        if (arguments == null) {
            return "{}";
        }
        if ("writeFile".equals(toolName) || "generatePDF".equals(toolName)) {
            return "{\"_memoryNote\":\"large file/PDF content omitted after successful tool execution\"}";
        }
        if (arguments.length() > 1200) {
            return "{\"_memoryNote\":\"large tool arguments omitted after successful tool execution\"}";
        }
        return arguments;
    }

    private String compactToolResponse(String toolName, String responseData) {
        if (responseData == null) {
            return "";
        }
        String text = responseData;
        int maxLength = switch (toolName) {
            case "scrapeWebPage" -> 1500;
            case "searchImages" -> 1200;
            case "searchWeb" -> 1800;
            default -> 2500;
        };
        text = stripHtml(text);
        text = compactImageUrls(toolName, text);
        text = text.replaceAll("\\s+", " ").trim();
        if (text.length() > maxLength) {
            return text.substring(0, maxLength) + "... [工具结果过长，已压缩写入上下文]";
        }
        return text;
    }

    private String stripHtml(String text) {
        if (!text.contains("<") || !text.contains(">")) {
            return text;
        }
        return text
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?s)<[^>]+>", " ");
    }

    private String compactImageUrls(String toolName, String text) {
        if (!"searchImages".equals(toolName)) {
            return text;
        }
        Matcher matcher = Pattern.compile("URL:\\s*(https?://\\S+)").matcher(text);
        StringBuilder builder = new StringBuilder();
        int count = 0;
        while (matcher.find() && count < 5) {
            count++;
            builder.append("Image ").append(count).append(": ").append(matcher.group(1)).append("\n");
        }
        return count > 0 ? builder.toString() : text;
    }

}
