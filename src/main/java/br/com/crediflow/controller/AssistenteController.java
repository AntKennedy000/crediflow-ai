package br.com.crediflow.controller;

import br.com.crediflow.service.AssistenteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistente")
public class AssistenteController {
    private final AssistenteService service;
    public AssistenteController(AssistenteService service) { this.service = service; }
    public record Comando(@NotBlank @Size(max=1000) String texto) { }
    @PostMapping("/texto")
    public AssistenteService.Resposta texto(@Valid @RequestBody Comando comando) {
        return service.conversar(comando.texto());
    }
}
