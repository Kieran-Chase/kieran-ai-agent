package pers.kieran.study.kieranaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * AI 可调用的工具类（函数）
 * 
 */

@Component
public class AITools {

    /**
     * 工具1：查询天气
     * @param city 城市名
     * @return 天气结果
     */
    @Tool(description = "查询指定城市的实时天气")
    public String getWeather(String city) {
        System.out.println("=== AI 调用了工具：查询天气，城市：" + city);
        // 这里可以调用真实天气接口
        return city + "：晴天，25℃，微风";
    }

    /**
     * 工具2：查询订单信息
     * @param orderId 订单号
     * @return 订单状态
     */
    @Tool(description = "根据订单号查询用户订单状态")
    public String getOrderInfo(String orderId) {
        System.out.println("=== AI 调用了工具：查询订单，订单号：" + orderId);
        return "订单" + orderId + "：已发货，预计明天到达";
    }

    /**
     * 工具3：计算加法（演示）
     */
    @Tool(description = "计算两个数字的和")
    public int add(int a, int b) {
        System.out.println("=== AI 调用了工具：计算加法 " + a + "+" + b);
        return a + b;
    }
}