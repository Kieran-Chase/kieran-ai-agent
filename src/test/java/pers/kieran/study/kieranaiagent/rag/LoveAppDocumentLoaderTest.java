package pers.kieran.study.kieranaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;
import pers.kieran.study.kieranaiagent.app.LoveApp;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/4/3
 */
@SpringBootTest
class LoveAppDocumentLoaderTest {
    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Test
    void loadMarkdowns() {
        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
        System.out.println("加载到的文档数量：" + documents.size());
        for (Document doc : documents) {
            System.out.println("文件名：" + doc.getMetadata().get("filename"));
            System.out.println("内容片段：" + doc.getText().substring(0, 50)); // 打印前50个字符
        }
    }
}