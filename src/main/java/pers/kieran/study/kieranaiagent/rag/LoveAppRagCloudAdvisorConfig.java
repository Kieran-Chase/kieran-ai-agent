package pers.kieran.study.kieranaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/4/18
 */
@Configuration
@Slf4j
public class LoveAppRagCloudAdvisorConfig {

    @Value("${spring.ai.dashscope.api-kay}")
    private String dashScopeApiKey;

    //自定义advisor
}
