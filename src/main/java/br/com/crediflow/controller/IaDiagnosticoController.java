package br.com.crediflow.controller;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ia")
public class IaDiagnosticoController {

    private final ChatClient chatClient;

    public IaDiagnosticoController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/diagnostico")
    public ResponseEntity<Map<String, Object>> diagnosticar() {
        try {
            String resposta = chatClient.prompt()
                .system("""
                    Voce esta executando um teste tecnico de conectividade.
                    Responda somente com CREDIFLOW_OK.
                    Nao inclua explicacoes, formatacao ou outros textos.
                    """)
                .user("Execute o teste de conectividade.")
                .call()
                .content();

            boolean recebeuTexto =
                resposta != null && !resposta.isBlank();

            boolean formatoCorreto =
                recebeuTexto && "CREDIFLOW_OK".equals(resposta.strip());

            if (!formatoCorreto) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.<String, Object>of(
                        "status", "RESPOSTA_FORA_DO_PADRAO",
                        "recebeuTexto", recebeuTexto,
                        "respostaNoFormatoEsperado", false,
                        "mensagem",
                        "A chamada terminou, mas a resposta precisa de ajuste."
                    ));
            }

            return ResponseEntity.ok(
                Map.<String, Object>of(
                    "status", "IA_LOCAL_CONFIRMADA",
                    "recebeuTexto", true,
                    "respostaNoFormatoEsperado", true,
                    "mensagem",
                    "Spring AI recebeu a resposta esperada do Ollama local."
                )
            );

        } catch (RuntimeException excecao) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.<String, Object>of(
                    "status", "FALHA_NA_CHAMADA_IA",
                    "mensagem",
                    "Nao foi possivel concluir o teste com o Ollama local."
                ));
        }
    }
}