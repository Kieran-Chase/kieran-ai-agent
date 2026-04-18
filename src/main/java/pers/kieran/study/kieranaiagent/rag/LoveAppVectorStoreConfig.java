package pers.kieran.study.kieranaiagent.rag;


import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/4/3
 */

/**
 * 恋爱大师向量数据库配置(初始化基于内存的向量数据库Bean)
 */
@Configuration
public class LoveAppVectorStoreConfig {
    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingmodel){
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingmodel).build();
        List<Document> documentList = loveAppDocumentLoader.loadMarkdowns();
        simpleVectorStore.add(documentList);
        return simpleVectorStore;
    }
}
