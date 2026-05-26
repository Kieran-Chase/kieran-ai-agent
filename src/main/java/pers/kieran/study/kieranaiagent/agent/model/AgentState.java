package pers.kieran.study.kieranaiagent.agent.model;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/5/25
 */

/**
 * 代理执行状态的枚举类
 */
public enum AgentState {

    /**
     * 空闲状态
     */
    IDLE,

    /**
     * 运行中状态
     */
    RUNNING,

    /**
     * 已完成状态
     */
    FINISHED,

    /**
     * 错误状态
     */
    ERROR
}
