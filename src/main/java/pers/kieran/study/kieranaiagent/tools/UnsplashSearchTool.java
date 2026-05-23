package pers.kieran.study.kieranaiagent.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/5/22
 */

/**
 * Unsplash 官方高清图库搜索工具
 */
@Component // 标注为 Spring 组件，让 Spring 容器自动管理它并注入属性
public class UnsplashSearchTool {

    // Unsplash 官方图片搜索接口地址
    private static final String UNSPLASH_SEARCH_URL = "https://api.unsplash.com/search/photos";

    // 自动从当前激活的配置文件（本地开发时即为 application-local.yml）读取真实 key
    @Value("${unsplash.access-key}")
    private String accessKey;

    @Tool(description = "Search for high-quality, royalty-free images on Unsplash. This tool returns direct JPG/PNG image download URLs.")
    public String searchImages(
            @ToolParam(description = "Search query keyword in English. (Tip: translate Chinese to English first, e.g. 'starry sky couple')") String query) {

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("query", query);
        paramMap.put("client_id", accessKey); // 使用注入的真实的 Access Key
        paramMap.put("per_page", 5);          // 默认取出前 5 张图片

        try {
            // 发送 GET 请求
            String response = HttpUtil.get(UNSPLASH_SEARCH_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            JSONArray results = jsonObject.getJSONArray("results");

            if (results == null || results.isEmpty()) {
                return "No images found for query: " + query;
            }

            StringJoiner joiner = new StringJoiner("\n");
            for (int i = 0; i < results.size(); i++) {
                JSONObject photo = results.getJSONObject(i);
                String id = photo.getStr("id");
                String altDescription = photo.getStr("alt_description", "No description");

                // 提取 urls 节点
                JSONObject urls = photo.getJSONObject("urls");
                String imageUrl = urls.getStr("regular");

                joiner.add(String.format("[%d] ID: %s, Desc: %s, URL: %s", (i + 1), id, altDescription, imageUrl));
            }
            return joiner.toString();
        } catch (Exception e) {
            return "Error searching Unsplash: " + e.getMessage();
        }
    }
}