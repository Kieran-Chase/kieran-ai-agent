package pers.kieran.study.kieranaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/3/30
 */
@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        //第一轮
        String message="你好，我是Kieran";
        String answer = loveApp.doChat(message, chatId);
        //第二轮
        message="我想让我的另一半（java）更爱我";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        //第三轮
        message="我的另一半叫啥来着，我刚说过，你帮我回忆一遍";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message="你好，我是Kieran,我想让另一半（java）更爱我，但我不知道怎么去做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }

    @Test
    void doChatWithRag() {
        String chatId=UUID.randomUUID().toString();
        String message="我已经结婚了，但是婚后关系不太亲密，怎么办？";
        String answer=loveApp.doChatWithRag(message,chatId);
        Assertions.assertNotNull(answer);
    }
}