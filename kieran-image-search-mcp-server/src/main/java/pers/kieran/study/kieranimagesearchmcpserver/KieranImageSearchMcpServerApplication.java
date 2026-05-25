package pers.kieran.study.kieranimagesearchmcpserver;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import pers.kieran.study.kieranimagesearchmcpserver.tools.ImageSearchTool;

@SpringBootApplication
public class KieranImageSearchMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KieranImageSearchMcpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider imageSearchTools(ImageSearchTool imageSearchTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(imageSearchTool)
                .build();
    }
}

