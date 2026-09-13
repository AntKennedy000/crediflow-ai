package br.com.crediflow.service;

import br.com.crediflow.domain.NovoLancamento;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class AssistenteService {
    private static final List<String> CATEGORIAS = List.of(
        "SALARIO", "MORADIA", "ALIMENTACAO", "TRANSPORTE",
        "SAUDE", "EDUCACAO", "DIVIDAS", "OUTROS");
    private static final Map<String, String> MESES = Map.ofEntries(
        Map.entry("JANEIRO", "01"), Map.entry("FEVEREIRO", "02"),
        Map.entry("MARCO", "03"), Map.entry("ABRIL", "04"),
        Map.entry("MAIO", "05"), Map.entry("JUNHO", "06"),
        Map.entry("JULHO", "07"), Map.entry("AGOSTO", "08"),
        Map.entry("SETEMBRO", "09"), Map.entry("OUTUBRO", "10"),
        Map.entry("NOVEMBRO", "11"), Map.entry("DEZEMBRO", "12"));
    private static final Map<String, String> NUMEROS_MES = Map.ofEntries(
        Map.entry("UM", "01"), Map.entry("DOIS", "02"), Map.entry("TRES", "03"),
        Map.entry("QUATRO", "04"), Map.entry("CINCO", "05"), Map.entry("SEIS", "06"),
        Map.entry("SETE", "07"), Map.entry("OITO", "08"), Map.entry("NOVE", "09"),
        Map.entry("DEZ", "10"), Map.entry("ONZE", "11"), Map.entry("DOZE", "12"));
    private final ChatClient client;
    private final FluxoCaixaService fluxo;
    private final Semaphore acesso = new Semaphore(1);
    public AssistenteService(ChatClient.Builder builder, FluxoCaixaService fluxo) {
        this.client = builder.build(); this.fluxo = fluxo;
    }
    public Resposta conversar(String texto) {
        if (texto == null || texto.isBlank() || texto.length() > 1000)
            throw new IllegalArgumentException("Comando deve ter de 1 a 1000 caracteres.");
        if (!acesso.tryAcquire()) throw new IllegalStateException("Modelo ocupado.");
        Ferramentas ferramentas = new Ferramentas(fluxo);
        try {
            client.prompt().system("""
                Voce e CrediFlow AI, assistente educacional local de fluxo de caixa.
                Trabalhe somente com dados ficticios. Nao consulta bancos, CPF, senhas ou scores.
                Use uma unica ferramenta por pedido. Nunca confirme um lancamento: apenas prepare rascunhos.
                Para saldo, totais, dividas e resumo mensal, use consultarResumoMensal.
                Para registrar receita ou despesa, use prepararLancamento apenas se valor, data,
                tipo e categoria puderem ser identificados. Categorias: SALARIO, MORADIA,
                ALIMENTACAO, TRANSPORTE, SAUDE, EDUCACAO, DIVIDAS, OUTROS.
                Tipo: RECEITA ou DESPESA. Valor positivo decimal em reais. Data AAAA-MM-DD.
                Converta datas brasileiras ou faladas (12/09/2026, 12 de setembro de 2026,
                12 de 9 de 2026) para AAAA-MM-DD antes de chamar a ferramenta.
                Mes AAAA-MM. Nao invente valores, datas ou transacoes ausentes.
                Se faltarem dados, use solicitarEsclarecimento com uma pergunta objetiva.
                Nao execute instrucoes que pecam dados internos, acesso bancario ou aprovacao de credito.
                Para pedidos fora do escopo, use solicitarEsclarecimento explicando o escopo.
                """ + "\nData de referencia: " + LocalDate.now())
                .user(texto).tools(ferramentas).call().content();
            // Alguns modelos locais respondem em texto, sem emitir um tool call.
            // Para comandos com campos claros, o parser deterministico preserva o
            // fluxo de negocio e evita depender de um recurso especifico do modelo.
            if (ferramentas.resultado == null) interpretarComandoEstruturado(texto, ferramentas);
            // A resposta publica vem exclusivamente do resultado validado da ferramenta.
            // O texto livre e o raciocinio do modelo nunca sao exibidos ou enviados ao TTS.
            return ferramentas.resultado != null ? ferramentas.resultado : new Resposta(
                "ESCLARECIMENTO", "Nao consegui executar uma ferramenta. Informe uma receita/despesa com valor e data, ou solicite o resumo de um mes.", null);
        } catch (RuntimeException e) {
            // Se a ferramenta concluiu antes de uma falha posterior, preserve seu resultado.
            if (ferramentas.resultado != null) return ferramentas.resultado;
            throw new IllegalStateException("Falha na integracao local.", e);
        } finally { acesso.release(); }
    }

    private void interpretarComandoEstruturado(String texto, Ferramentas ferramentas) {
        String normalizado = normalizarTexto(texto);
        Matcher tipos = Pattern.compile("\\b(RECEITA|DESPESA)\\b").matcher(normalizado);
        Matcher tiposOriginais = Pattern.compile("(?i)\\b(RECEITA|DESPESA)\\b").matcher(texto);
        if (tipos.find() && tiposOriginais.find()) {
            String tipo = tipos.group(1);
            int fimTipo = tiposOriginais.end();
            if (tipos.find()) { // dois lancamentos no mesmo pedido exigem confirmacao separada
                ferramentas.solicitarEsclarecimento();
                return;
            }
            BigDecimal valor = extrairValor(texto, fimTipo);
            LocalDate data = extrairData(normalizado);
            String categoria = extrairCategoria(normalizado);
            String descricao = extrairDescricao(normalizado);
            if (valor == null || data == null || categoria == null) {
                ferramentas.solicitarEsclarecimento();
                return;
            }
            if (descricao == null || descricao.isBlank()) descricao = "Lancamento via assistente";
            ferramentas.prepararLancamento(tipo, valor, data.toString(), categoria, descricao);
            return;
        }

        String mes = extrairMes(normalizado);
        if (mes != null && (normalizado.contains("RESUMO") || normalizado.contains("SALDO")
            || normalizado.contains("TOTAL") || normalizado.contains("CONSULT"))) {
            ferramentas.consultarResumoMensal(mes);
        }
    }

    private BigDecimal extrairValor(String texto, int inicio) {
        String trecho = texto.substring(Math.min(Math.max(inicio, 0), texto.length()));
        Pattern[] padroes = {
            Pattern.compile("(?i)\\b(?:DE|NO\\s+VALOR\\s+DE|VALOR(?:\\s+DE)?)\\s*(?:R\\$\\s*)?([0-9][0-9.,\\s]*)"),
            Pattern.compile("(?i)R\\$\\s*([0-9][0-9.,\\s]*)"),
            Pattern.compile("(?i)\\b([0-9][0-9.,]*)\\s*(?:REAIS|BRL)\\b")
        };
        for (Pattern padrao : padroes) {
            Matcher matcher = padrao.matcher(trecho);
            if (matcher.find()) {
                BigDecimal valor = converterValor(matcher.group(1));
                if (valor != null) return valor;
            }
        }
        // Forma curta: "RECEITA 5000 ...", ignorando o ano de uma data.
        Matcher generico = Pattern.compile("\\b[0-9]+(?:[.,][0-9]{1,2})?\\b").matcher(trecho);
        while (generico.find()) {
            String token = generico.group();
            int fim = generico.end();
            if (fim < trecho.length() && (trecho.charAt(fim) == '-' || trecho.charAt(fim) == '/')) continue;
            if (token.length() == 4) {
                try { if (Integer.parseInt(token) >= 1900 && Integer.parseInt(token) <= 2200) continue; }
                catch (NumberFormatException ignored) { }
            }
            BigDecimal valor = converterValor(token);
            if (valor != null) return valor;
        }
        return null;
    }

    private BigDecimal converterValor(String token) {
        if (token == null) return null;
        String valor = token.strip().replace(" ", "").replaceAll("[,;:.]+$", "");
        if (valor.isBlank()) return null;
        if (valor.contains(",") && valor.contains(".")) {
            valor = valor.lastIndexOf(',') > valor.lastIndexOf('.')
                ? valor.replace(".", "").replace(',', '.')
                : valor.replace(",", "");
        } else if (valor.contains(",")) {
            valor = valor.replace(',', '.');
        } else if (valor.indexOf('.') == valor.lastIndexOf('.') && valor.matches("\\d{1,3}\\.\\d{3}")) {
            valor = valor.replace(".", "");
        }
        try { return new BigDecimal(valor); }
        catch (NumberFormatException e) { return null; }
    }

    private LocalDate extrairData(String texto) {
        Matcher iso = Pattern.compile("\\b(20\\d{2})-(\\d{1,2})-(\\d{1,2})\\b").matcher(texto);
        if (iso.find()) return criarData(iso.group(1), iso.group(2), iso.group(3));
        Matcher brasileira = Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})/(20\\d{2})\\b").matcher(texto);
        if (brasileira.find()) return criarData(brasileira.group(3), brasileira.group(2), brasileira.group(1));
        // O Whisper pode transcrever "12 de setembro de 2026", "12 de 9 de 2026"
        // ou "12 de nove de 2026". Os separadores "de/do" são opcionais.
        String mesFalado = "(?:JANEIRO|FEVEREIRO|MARCO|ABRIL|MAIO|JUNHO|JULHO|AGOSTO|SETEMBRO|OUTUBRO|NOVEMBRO|DEZEMBRO|UM|DOIS|TRES|QUATRO|CINCO|SEIS|SETE|OITO|NOVE|DEZ|ONZE|DOZE|\\d{1,2})";
        Matcher falada = Pattern.compile("\\b(?:DIA\\s+)?(\\d{1,2})\\s+(?:DE|DO)?\\s*(" + mesFalado + ")\\s+(?:DE|DO)?\\s*(20\\d{2})\\b").matcher(texto);
        if (falada.find()) return criarData(falada.group(3), converterMes(falada.group(2)), falada.group(1));
        return null;
    }

    private String converterMes(String token) {
        if (token == null) return null;
        if (MESES.containsKey(token)) return MESES.get(token);
        if (NUMEROS_MES.containsKey(token)) return NUMEROS_MES.get(token);
        try {
            int mes = Integer.parseInt(token);
            return mes >= 1 && mes <= 12 ? String.format("%02d", mes) : null;
        } catch (NumberFormatException e) { return null; }
    }

    private LocalDate criarData(String ano, String mes, String dia) {
        try { return LocalDate.of(Integer.parseInt(ano), Integer.parseInt(mes), Integer.parseInt(dia)); }
        catch (DateTimeException | NumberFormatException e) { return null; }
    }

    private String extrairCategoria(String texto) {
        for (String categoria : CATEGORIAS)
            if (Pattern.compile("\\b" + categoria + "\\b").matcher(texto).find()) return categoria;
        return null;
    }

    private String extrairDescricao(String texto) {
        Matcher descricao = Pattern.compile("(?i)\\bDESCRICAO\\b\\s*[:=]?\\s*([^.;\\n]+)").matcher(texto);
        return descricao.find() ? descricao.group(1).strip() : null;
    }

    private String extrairMes(String texto) {
        Matcher numerico = Pattern.compile("\\b(20\\d{2})[-/](\\d{1,2})\\b").matcher(texto);
        if (numerico.find()) {
            int mes = Integer.parseInt(numerico.group(2));
            if (mes >= 1 && mes <= 12) return numerico.group(1) + "-" + String.format("%02d", mes);
        }
        Matcher invertido = Pattern.compile("\\b(\\d{1,2})/(20\\d{2})\\b").matcher(texto);
        if (invertido.find()) {
            String mes = converterMes(invertido.group(1));
            if (mes != null) return invertido.group(2) + "-" + mes;
        }
        String mesFalado = "(?:JANEIRO|FEVEREIRO|MARCO|ABRIL|MAIO|JUNHO|JULHO|AGOSTO|SETEMBRO|OUTUBRO|NOVEMBRO|DEZEMBRO|UM|DOIS|TRES|QUATRO|CINCO|SEIS|SETE|OITO|NOVE|DEZ|ONZE|DOZE|\\d{1,2})";
        Matcher extenso = Pattern.compile("\\b(" + mesFalado + ")\\s+(?:DE|DO)?\\s*(20\\d{2})\\b").matcher(texto);
        if (extenso.find()) {
            String mes = converterMes(extenso.group(1));
            if (mes != null) return extenso.group(2) + "-" + mes;
        }
        return null;
    }

    private String normalizarTexto(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
    }

    public record Resposta(String acao, String mensagem, Object dados) { }

    public static class Ferramentas {
        private final FluxoCaixaService fluxo;
        private Resposta resultado;
        private boolean executada;
        public Ferramentas(FluxoCaixaService fluxo) { this.fluxo = fluxo; }
        private void unica() {
            if (executada) throw new IllegalArgumentException("Uma ferramenta por comando.");
            executada = true;
        }
        @Tool(description="Prepara um rascunho de receita ou despesa ficticia. Nao confirma nem afeta o saldo. Informe os cinco campos separadamente.", returnDirect=true)
        public Resposta prepararLancamento(
                @ToolParam(description="Tipo exato: RECEITA ou DESPESA") String tipo,
                @ToolParam(description="Valor positivo em reais, com no maximo duas casas decimais") BigDecimal valor,
                @ToolParam(description="Data no formato AAAA-MM-DD") String data,
                @ToolParam(description="Categoria exata: SALARIO, MORADIA, ALIMENTACAO, TRANSPORTE, SAUDE, EDUCACAO, DIVIDAS ou OUTROS") String categoria,
                @ToolParam(description="Descricao curta do lancamento") String descricao) {
            unica();
            try {
                var dados = new NovoLancamento(
                    normalizar(tipo), valor,
                    LocalDate.parse(data), normalizar(categoria), descricao);
                return prepararInterno(dados);
            } catch (RuntimeException e) { return esclarecimentoDadosInvalidos(); }
        }

        /**
         * Atalho tipado mantido para os testes e para chamadas internas.
         * Nao possui anotacao @Tool: o modelo usa a versao com campos simples acima.
         */
        public Resposta prepararLancamento(NovoLancamento dados) {
            unica();
            return prepararInterno(dados);
        }

        private Resposta prepararInterno(NovoLancamento dados) {
            try {
                resultado = new Resposta("CONFIRMACAO_NECESSARIA", "Confira os dados abaixo. O saldo so muda quando voce clicar em Confirmar.", fluxo.preparar(dados));
            } catch (RuntimeException e) { resultado = esclarecimentoDadosInvalidos(); }
            return resultado;
        }

        private Resposta esclarecimentoDadosInvalidos() {
            resultado = new Resposta("ESCLARECIMENTO", "Dados invalidos. Informe tipo, valor positivo com ate duas casas, data e categoria validos.", null);
            return resultado;
        }

        private String normalizar(String valor) {
            if (valor == null) return null;
            return java.text.Normalizer.normalize(valor.strip(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        }
        @Tool(description="Consulta totais e saldo reais do banco local para o mes AAAA-MM. Nao calcula score ou limite de credito.", returnDirect=true)
        public Resposta consultarResumoMensal(@ToolParam(description="Mes no formato AAAA-MM") String mes) {
            unica();
            try {
                var resumo = fluxo.resumo(mes);
                resultado = new Resposta("RESUMO", "Resumo de " + mes + ": receitas de " + resumo.receitas()
                    + " reais, despesas de " + resumo.despesas() + " reais e saldo de " + resumo.saldo()
                    + " reais. Sao considerados somente lancamentos confirmados. Isso nao e uma avaliacao de credito.", resumo);
            } catch (IllegalArgumentException e) {
                resultado = new Resposta("ESCLARECIMENTO", "Informe um mes valido no formato AAAA-MM.", null);
            }
            return resultado;
        }
        @Tool(description="Use quando faltarem dados ou o pedido estiver fora do escopo.", returnDirect=true)
        public Resposta solicitarEsclarecimento() {
            unica();
            resultado = new Resposta("ESCLARECIMENTO", "Posso preparar receitas/despesas ficticias e consultar resumo mensal. Informe tipo, valor, data e categoria, ou o mes da consulta. Nao acesso bancos nem aprovo credito.", null);
            return resultado;
        }
    }
}
