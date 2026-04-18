package pers.kieran.study.kieranaiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/3/24
 */

/**
 *Spring AI 框架调用AI大模型
 */
@Component
public class SpringAiAiInvoke implements CommandLineRunner {
    @Resource
    private ChatModel dashscopeChatModel;

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage assistantMessage = dashscopeChatModel.call(new Prompt("你好，我是Kieran"))
                .getResult()
                .getOutput();
        System.out.println(assistantMessage.getText());
    }
}
