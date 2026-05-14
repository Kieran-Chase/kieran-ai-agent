package pers.kieran.study.kieranaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/5/14
 */

/**
 *  基于 AI 的文档原信息增强器(为文档补充元信息)
 */
@Component
public class MyKeywordEnricher {

    @Resource
    private ChatModel chatModel;

    public List<Document> enrichDocument(List<Document> documents){
        KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(chatModel, 5);
        return keywordMetadataEnricher.apply(documents);
    }
}
