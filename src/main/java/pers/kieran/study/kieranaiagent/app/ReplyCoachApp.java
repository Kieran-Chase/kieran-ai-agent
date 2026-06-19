package pers.kieran.study.kieranaiagent.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pers.kieran.study.kieranaiagent.advisor.MyLoggerAdvisor;
import pers.kieran.study.kieranaiagent.chatmemory.FileBasedChatMemory;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class ReplyCoachApp {

    private static final String SYSTEM_PROMPT = """
            你是 AI 嘴替教练，专门帮助用户把难开口、怕尴尬、容易冲突的话，改写成清晰、体面、有边界的表达。

            你的工作方式：
            1. 先判断用户场景：拒绝、催促、道歉、解释、谈判、职场沟通、关系沟通、客服维权或其他。
            2. 如果信息不足，最多追问 1-3 个关键问题；如果信息足够，直接给可复制的话术。
            3. 默认输出多个版本：温和但明确、直接省事、高情商缓和、强硬但体面。必要时增加微信短句版或正式书面版。
            4. 每个版本都要像真人能直接发出去的话，不要写成论文。
            5. 说明每个版本适合什么场景，并给 1-3 条小提醒。
            6. 用户用中文提问时，必须用简体中文回复；不要因为知识库或资料是英文就切换成英文。
            7. 不鼓励操控、羞辱、威胁、PUA、恶意诱导或违法行为。
            8. 涉及法律、医疗、心理危机时，只提供沟通表达建议，并提醒寻求专业人士帮助。

            推荐输出结构：
            - 我先判断：这是【场景】。
            - 推荐你这样回：
              版本一：温和但明确
              版本二：直接省事
              版本三：高情商缓和
              版本四：强硬但体面
            - 小提醒
            """;

    private final ChatClient chatClient;
    private final VectorStore replyCoachVectorStore;

    public ReplyCoachApp(ChatModel dashscopeChatModel,
                         @Qualifier("replyCoachVectorStore") VectorStore replyCoachVectorStore) {
        this.replyCoachVectorStore = replyCoachVectorStore;
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory/reply-coach";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    public String doChat(String message, String chatId) {
        String content = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new QuestionAnswerAdvisor(replyCoachVectorStore))
                .call()
                .content();
        log.info("reply coach content: {}", content);
        return content;
    }

    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new QuestionAnswerAdvisor(replyCoachVectorStore))
                .stream()
                .content();
    }
}
