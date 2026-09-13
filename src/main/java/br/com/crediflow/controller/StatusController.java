package br.com.crediflow.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    @GetMapping
    public Map<String, String> consultarStatus() {
        return Map.of(
            "status", "Em funcionamento",
            "projeto", "CrediFlow AI",
            "finalidade", "Assistente educacional de fluxo de caixa",
            "integracaoIA", "Ollama local"
        );
    }
}