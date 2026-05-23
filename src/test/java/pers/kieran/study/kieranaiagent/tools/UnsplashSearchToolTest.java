package pers.kieran.study.kieranaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/5/22
 */

/**
 * Unsplash 搜索工具独立测试类
 */

public class UnsplashSearchToolTest {

    @Test
    public void testSearchImages() {
        String accessKey = null;
        try {
            // 1. 使用你项目已有的 SnakeYAML 库，手动读取本地 application-local.yml 配置文件
            Yaml yaml = new Yaml();
            InputStream inputStream = this.getClass()
                    .getClassLoader()
                    .getResourceAsStream("application-local.yml");

            if (inputStream == null) {
                System.err.println("未找到 application-local.yml 配置文件！");
                return;
            }

            Map<String, Object> obj = yaml.load(inputStream);
            // 逐级读取 yml：unsplash -> access-key
            Map<String, Object> unsplash = (Map<String, Object>) obj.get("unsplash");
            if (unsplash != null) {
                accessKey = (String) unsplash.get("access-key");
            }
        } catch (Exception e) {
            System.err.println("读取本地 yml 配置文件失败：" + e.getMessage());
        }

        // 2. 检查密钥是否成功配置
        if (accessKey == null || "your_real_unsplash_access_key".equals(accessKey) || accessKey.trim().isEmpty()) {
            System.err.println("请先在 application-local.yml 中配置真实的 unsplash.access-key！");
            return;
        }

        // 3. 手动实例化 Tool 类（绕过 Spring 容器）
        UnsplashSearchTool searchTool = new UnsplashSearchTool();

        // 4. 使用 Spring 自带的反射测试工具，把获取到的 Key 注入到类中（模拟 @Value 的效果）
        ReflectionTestUtils.setField(searchTool, "accessKey", accessKey);

        // 5. 运行纯接口测试
        String result = searchTool.searchImages("starry sky couple");

        System.out.println("====== Unsplash 搜索结果返回 ======");
        System.out.println(result);
        System.out.println("==================================");

        assertNotNull(result);
        assertFalse(result.contains("Error"), "接口不应该返回异常错误");
        assertFalse(result.contains("No images found"), "应该能检索到符合要求的图片");
    }
}