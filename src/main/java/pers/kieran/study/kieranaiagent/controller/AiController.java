package pers.kieran.study.kieranaiagent.controller;


import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pers.kieran.study.kieranaiagent.agent.KieranManus;
import pers.kieran.study.kieranaiagent.app.LoveApp;
import reactor.core.publisher.Flux;
import pers.kieran.study.kieranaiagent.tools.AITools;
import pers.kieran.study.kieranaiagent.tools.WebSearchTool;
import pers.kieran.study.kieranaiagent.tools.UnsplashSearchTool;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/5/26
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private AITools aiTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Resource
    private UnsplashSearchTool unsplashSearchTool;

    @GetMapping("/debug/search")
    public String debugSearch(String query) {
        return new WebSearchTool(searchApiKey).searchWeb(query);
    }

    @GetMapping("/debug/images")
    public String debugImages(String query) {
        return unsplashSearchTool.searchImages(query);
    }


    /**
     * 同步调用 AI 恋爱大师应用
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message,String chatId){
        return loveApp.doChat(message,chatId);
    }


    /**
     *  SSE 流式调用 AI 恋爱大师应用
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value="/love_app/chat/sse",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message,String chatId){
        return loveApp.doChatByStream(message,chatId);
    }

    /**
     *  SSE 流式调用 AI 恋爱大师应用
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value="/love_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithLoveAppServerSentEvent(String message, String chatId){
        return loveApp.doChatByStream(message,chatId)
                .map(chunk-> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     *  SSE 流式调用 AI 恋爱大师应用
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value="/love_app/chat/sse_emitter")
    public SseEmitter doChatWithLoveAppServerSseEmitter(String message, String chatId){
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(180000L);
        // 获取 Flux 响应是数据流并且直接通过订阅推送给 SseEmitter
        loveApp.doChatByStream(message,chatId)
                .subscribe(chunk ->{
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e){
                        sseEmitter.completeWithError(e);
                    }
                },sseEmitter::completeWithError,sseEmitter::complete);
        // 返回
        return sseEmitter;
    }

    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message){
        KieranManus kieranManus = new KieranManus(allTools, dashscopeChatModel);
        return kieranManus.runStream(message);
    }

    /**
     * 聊天接口（自动支持工具调用）
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        ChatResponse response = ChatClient.builder(dashscopeChatModel)
                .build()
                .prompt()
                .user(message)
                .tools(aiTools)
                .call()
                .chatResponse();

        return response.getResult().getOutput().getText();
    }
}
