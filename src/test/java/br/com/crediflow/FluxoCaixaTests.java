package br.com.crediflow;

import br.com.crediflow.domain.*;
import br.com.crediflow.service.*;
import br.com.crediflow.repository.LancamentoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FluxoCaixaTests {
    @Autowired FluxoCaixaService service;
    @Autowired LancamentoRepository repository;
    @BeforeEach void limpar() { repository.deleteAll(); }
    private NovoLancamento dados(String tipo, String valor, String categoria) {
        return new NovoLancamento(tipo, new BigDecimal(valor), LocalDate.of(2026,9,1), categoria, "Exemplo ficticio");
    }
    @Test void rascunhoNaoAfetaSaldo() {
        service.preparar(dados("RECEITA","5000","SALARIO"));
        assertEquals(0, service.resumo("2026-09").quantidade());
    }
    @Test void confirmarDuasVezesNaoDuplica() {
        var l = service.preparar(dados("RECEITA","5000","SALARIO"));
        service.confirmar(l.getId()); service.confirmar(l.getId());
        assertEquals(1, service.resumo("2026-09").quantidade());
        assertEquals(new BigDecimal("5000.00"), service.resumo("2026-09").saldo());
    }
    @Test void totaisEComprometimentoCorretos() {
        for (var dado : new NovoLancamento[]{dados("RECEITA","5000","SALARIO"),dados("DESPESA","1000","DIVIDAS"),dados("DESPESA","800","MORADIA")}) {
            service.confirmar(service.preparar(dado).getId());
        }
        var r = service.resumo("2026-09");
        assertEquals(new BigDecimal("3200.00"),r.saldo());
        assertEquals(new BigDecimal("20.00"),r.percentualDividasSobreReceitas());
        assertEquals(new BigDecimal("800.00"),r.despesasPorCategoria().get("MORADIA"));
    }
    @Test void semReceitaPercentualNulo() {
        service.confirmar(service.preparar(dados("DESPESA","50","DIVIDAS")).getId());
        assertNull(service.resumo("2026-09").percentualDividasSobreReceitas());
        assertEquals(new BigDecimal("-50.00"),service.resumo("2026-09").saldo());
    }
    @Test void separaMeses() {
        service.confirmar(service.preparar(dados("RECEITA","5000","SALARIO")).getId());
        assertEquals(0,service.resumo("2026-10").quantidade());
    }
    @Test void dinheiroNaoUsaPontoFlutuante() {
        service.confirmar(service.preparar(dados("RECEITA","0.10","OUTROS")).getId());
        service.confirmar(service.preparar(dados("RECEITA","0.20","OUTROS")).getId());
        assertEquals(new BigDecimal("0.30"),service.resumo("2026-09").saldo());
    }
    @Test void rejeitaValorInvalido() {
        for (String valor : new String[]{"0","-1","0.001","1000000000"})
            assertThrows(IllegalArgumentException.class,()->service.preparar(dados("RECEITA",valor,"SALARIO")));
    }
    @Test void rejeitaTipoECategoriaInvalidos() {
        assertThrows(IllegalArgumentException.class,()->service.preparar(dados("PIX","10","SALARIO")));
        assertThrows(IllegalArgumentException.class,()->service.preparar(dados("RECEITA","10","INVENTADA")));
    }
    @Test void rejeitaMesInvalido() {
        for (String mes : new String[]{"2026-13","2026-9","texto","1999-01"})
            assertThrows(IllegalArgumentException.class,()->service.resumo(mes));
    }
    @Test void confirmarIdInexistenteFalha() {
        assertThrows(java.util.NoSuchElementException.class,()->service.confirmar(UUID.randomUUID()));
    }
    @Test void ferramentaNaoConfirmaLancamento() {
        var tools = new AssistenteService.Ferramentas(service);
        assertEquals("CONFIRMACAO_NECESSARIA",tools.prepararLancamento(dados("RECEITA","5000","SALARIO")).acao());
        assertEquals(0,service.resumo("2026-09").quantidade());
        assertThrows(IllegalArgumentException.class,()->tools.prepararLancamento(dados("RECEITA","5000","SALARIO")));
    }
}
