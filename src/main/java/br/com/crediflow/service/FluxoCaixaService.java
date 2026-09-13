package br.com.crediflow.service;

import br.com.crediflow.domain.*;
import br.com.crediflow.repository.LancamentoRepository;
import jakarta.validation.Validator;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FluxoCaixaService {
    private final LancamentoRepository repository;
    private final Validator validator;
    public FluxoCaixaService(LancamentoRepository repository, Validator validator) {
        this.repository = repository; this.validator = validator;
    }
    @Transactional
    public Lancamento preparar(NovoLancamento dados) {
        if (dados == null || !validator.validate(dados).isEmpty())
            throw new IllegalArgumentException("Lancamento invalido: confira tipo, valor, data, categoria e descricao.");
        if (dados.data().getYear() < 2000 || dados.data().getYear() > 2100)
            throw new IllegalArgumentException("Use uma data entre 2000 e 2100.");
        return repository.save(new Lancamento(dados.tipo(), dados.valor().setScale(2),
            dados.data(), dados.categoria(), dados.descricao().strip()));
    }
    @Transactional
    public Lancamento confirmar(UUID id) {
        Lancamento lancamento = repository.buscarParaConfirmar(id)
            .orElseThrow(() -> new NoSuchElementException("Lancamento nao encontrado."));
        // Repetir a confirmacao do mesmo ID nao duplica o lancamento.
        if (!"CONFIRMADO".equals(lancamento.getEstado())) lancamento.confirmar();
        return lancamento;
    }
    public YearMonth validarMes(String mes) {
        if (mes == null || !mes.matches("\\d{4}-\\d{2}"))
            throw new IllegalArgumentException("Informe o mes no formato AAAA-MM.");
        try {
            YearMonth periodo = YearMonth.parse(mes);
            if (periodo.getYear() < 2000 || periodo.getYear() > 2100) throw new IllegalArgumentException();
            return periodo;
        } catch (RuntimeException e) { throw new IllegalArgumentException("Mes invalido. Use AAAA-MM entre 2000 e 2100."); }
    }
    @Transactional(readOnly=true)
    public List<Lancamento> listar(String mes) {
        YearMonth periodo = validarMes(mes);
        return repository.findByEstadoAndDataGreaterThanEqualAndDataLessThanOrderByDataAscCriadoEmAsc(
            "CONFIRMADO", periodo.atDay(1), periodo.plusMonths(1).atDay(1));
    }
    @Transactional(readOnly=true)
    public Resumo resumo(String mes) {
        List<Lancamento> itens = listar(mes);
        BigDecimal receitas = BigDecimal.ZERO.setScale(2), despesas = receitas, dividas = receitas;
        Map<String, BigDecimal> categorias = new TreeMap<>();
        for (Lancamento item : itens) {
            if ("RECEITA".equals(item.getTipo())) receitas = receitas.add(item.getValor());
            else {
                despesas = despesas.add(item.getValor());
                categorias.merge(item.getCategoria(), item.getValor(), BigDecimal::add);
                if ("DIVIDAS".equals(item.getCategoria())) dividas = dividas.add(item.getValor());
            }
        }
        BigDecimal percentual = receitas.signum() == 0 ? null : dividas.multiply(new BigDecimal("100"))
            .divide(receitas, 2, RoundingMode.HALF_UP);
        return new Resumo(mes, itens.size(), receitas, despesas, receitas.subtract(despesas), dividas,
            percentual, categorias,
            "Somente lancamentos confirmados. Dividas/receitas e um indicador descritivo, nao score, limite ou aprovacao de credito. Sem receitas, o percentual nao e calculado.");
    }
    public record Resumo(String mes, int quantidade, BigDecimal receitas, BigDecimal despesas,
        BigDecimal saldo, BigDecimal pagamentosDividas, BigDecimal percentualDividasSobreReceitas,
        Map<String, BigDecimal> despesasPorCategoria, String ressalva) { }
}
