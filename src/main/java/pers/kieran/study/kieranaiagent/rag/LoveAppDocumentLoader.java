package pers.kieran.study.kieranaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/4/1
 */
@Component
@Slf4j
public class LoveAppDocumentLoader {
    private final ResourcePatternResolver resourcePatternResolver;

    LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver){
        this.resourcePatternResolver=resourcePatternResolver;
    }

    /**
     * 加载多篇Markdown文档
     * @return
     */
    public List<Document> loadMarkdowns(){
        List<Document> allDocuments = new ArrayList<>();
        //这里可以修改为你要加载的多个Markdown文件的路径模式
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for(Resource resource:resources){
                String fileName = resource.getFilename();
                //提取文档的倒数第三个和第二个字作为标签
                String status=fileName.substring(fileName.length()-6,fileName.length()-4);
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename",fileName)
                        .withAdditionalMetadata("status",status)
                        .build();
                MarkdownDocumentReader reader= new MarkdownDocumentReader(resource,config);
                allDocuments.addAll(reader.get());
            }
        } catch (IOException e) {
            log.error("Markdown 文档加载失败",e);
        }
        return allDocuments;
    }

}
