package pers.kieran.study.kieranaiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class ReplyCoachVectorStoreConfig {

    @Resource
    private ReplyCoachDocumentLoader replyCoachDocumentLoader;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Bean
    VectorStore replyCoachVectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        try {
            List<Document> documentList = replyCoachDocumentLoader.loadMarkdowns();
            List<Document> enrichedDocuments = myKeywordEnricher.enrichDocument(documentList);
            if (!enrichedDocuments.isEmpty()) {
                simpleVectorStore.add(enrichedDocuments);
            }
        } catch (Exception e) {
            log.warn("Reply coach vector store initialization skipped, app will run without RAG: {}", e.getMessage(), e);
        }
        return simpleVectorStore;
    }
}
