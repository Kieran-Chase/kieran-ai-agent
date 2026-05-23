package pers.kieran.study.kieranaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/5/21
 */
@SpringBootTest
public class WebSearchToolTest {

    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Test
    public void testSearchWeb() {
        WebSearchTool tool = new WebSearchTool(searchApiKey);
        String query = "java介绍";
        String result = tool.searchWeb(query);
        assertNotNull(result);
    }
}
