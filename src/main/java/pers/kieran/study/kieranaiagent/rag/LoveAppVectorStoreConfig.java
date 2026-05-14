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

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingmodel){
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingmodel).build();
        //加载文档
        List<Document> documentList = loveAppDocumentLoader.loadMarkdowns();
        //自主切分文档
        //List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documentList);

        //自动补充关键词元信息
        List<Document> enrichedDocuments=myKeywordEnricher.enrichDocument(documentList);
        simpleVectorStore.add(enrichedDocuments);
        return simpleVectorStore;
    }
}
