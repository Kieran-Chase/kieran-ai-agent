package pers.kieran.study.kieranaiagent.demo.invoke;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import pers.kieran.study.kieranaiagent.demo.invoke.TestApiKey;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/3/24
 */

/**
 * HTTP 方式调用 AI
 */
public class HttpAiInvoke {

    public static void main(String[] args) {
        // API密钥（确保TestApiKey.API_KEY是静态常量）
        String apiKey = TestApiKey.API_KEY;

        // 构建请求URL
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

        // 构建请求JSON数据
        JSONObject inputJson = new JSONObject();
        // 添加系统消息
        JSONObject systemMessage = new JSONObject();
        systemMessage.set("role", "system");
        systemMessage.set("content", "You are a helpful assistant.");
        // 添加用户消息
        JSONObject userMessage = new JSONObject();
        userMessage.set("role", "user");
        userMessage.set("content", "你是谁？");
        // 组装messages数组
        JSONArray messagesArray = new JSONArray();
        messagesArray.add(systemMessage);
        messagesArray.add(userMessage);
        inputJson.set("messages", messagesArray);

        // 构建参数
        JSONObject parametersJson = new JSONObject();
        parametersJson.set("result_format", "message");

        // 构建完整请求体
        JSONObject requestJson = new JSONObject();
        requestJson.set("model", "qwen-plus");
        requestJson.set("input", inputJson);
        requestJson.set("parameters", parametersJson);

        // 发送请求（使用Hutool Http工具）
        String result = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestJson.toString())
                .execute()
                .body();

        // 输出结果
        System.out.println(result);
    }
}