package br.com.crediflow.controller;

import br.com.crediflow.domain.*;
import br.com.crediflow.service.FluxoCaixaService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FluxoCaixaController {
    private final FluxoCaixaService service;
    public FluxoCaixaController(FluxoCaixaService service) { this.service = service; }
    @PostMapping("/lancamentos/rascunhos")
    public Lancamento preparar(@Valid @RequestBody NovoLancamento dados) { return service.preparar(dados); }
    @PostMapping("/lancamentos/{id}/confirmar")
    public Lancamento confirmar(@PathVariable UUID id) { return service.confirmar(id); }
    @GetMapping("/lancamentos")
    public List<Lancamento> listar(@RequestParam String mes) { return service.listar(mes); }
    @GetMapping("/resumo")
    public FluxoCaixaService.Resumo resumo(@RequestParam String mes) { return service.resumo(mes); }
}
