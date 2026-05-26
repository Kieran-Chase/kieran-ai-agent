package pers.kieran.study.kieranaiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/5/26
 */
@SpringBootTest
class KieranManusTest {
/*(properties = "spring.ai.mcp.client.enabled=false")*/
    @Resource
    private KieranManus kieranManus;

    @Test
    void run() {
        String userPrompt = """  
                我的另一半居住在上海静安区，请帮我找到 5 公里内合适的约会地点，  
                并结合一些网络图片，制定一份详细的约会计划，  
                并以 PDF 格式输出""";
        String answer = kieranManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}
