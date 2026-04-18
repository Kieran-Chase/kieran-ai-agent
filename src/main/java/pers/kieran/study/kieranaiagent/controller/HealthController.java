package pers.kieran.study.kieranaiagent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/3/24
 */

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public String healthCheck() {
        return "ok";
    }
}