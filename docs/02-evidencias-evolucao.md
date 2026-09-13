# Evidências e evolução

## Validação local — 13/09/2026

Os registros abaixo distinguem resultados de testes, capturas fornecidas pelo autor e confirmações manuais. Não são evidência de uso em produção.

| Verificação | Resultado e evidência |
|---|---|
| Testes automáticos após as alterações de datas | 12 testes, 0 falhas, 0 erros e 0 ignorados. Relatórios locais de 13/09/2026 às 14:25 (UTC-3): [contexto Spring](../evidencias/testes/CrediflowAiApplicationTests.txt) e [regras de negócio](../evidencias/testes/FluxoCaixaTests.txt). |
| API e áudio ativos | Verificação HTTP em 13/09/2026: `/api/status` respondeu `Em funcionamento` e o adaptador `/status` respondeu `AUDIO_LOCAL`. Isso verifica disponibilidade, não a qualidade da fala. |
| Integração Spring AI + Ollama | [Captura do diagnóstico](../evidencias/02-ia-local.png) com `IA_LOCAL_CONFIRMADA`. |
| Lançamentos e visão mensal | [Captura](../evidencias/03-lancamentos-confirmados.png): receita de R$ 5.000,00, despesa de R$ 120,00 e saldo de R$ 4.880,00, com dois lançamentos confirmados. |
| Consulta por texto | [Captura](../evidencias/04-resumo-textual.png): o pedido de resumo de setembro de 2026 retornou os mesmos totais. |
| Nova consulta aos dados salvos | [Resposta local de 13/09/2026](../evidencias/05-resumo-verificado.json): os mesmos dois confirmados e saldo de R$ 4.880,00. É uma leitura posterior; não foi realizado um teste automatizado de reinicialização. |
| Confirmação sem duplicar o mesmo ID | Coberta pelo teste `confirmarDuasVezesNaoDuplica`. A mensagem da interface, isoladamente, não comprova esse comportamento. |
| Transcrição de áudio | Captura apresentada durante o desenvolvimento mostrou a transcrição da despesa fictícia. Foi observado erro de ano e de formato da data. |
| Síntese de voz | O autor confirmou ter ouvido a resposta pelo navegador. Confirmação manual, sem arquivo de áudio publicado. |
| Mês falado por nome ou número | Após a ampliação dos formatos, o autor confirmou em 13/09/2026 que o teste funcionou. A confirmação não substitui testes de todas as combinações possíveis. |

A [captura inicial do BUILD SUCCESS](../evidencias/01-testes-build-success.png) é de 12/09/2026. Os relatórios de texto da tabela são da execução posterior de 13/09/2026. A suíte automatizada cobre regras de negócio e contexto Spring; não testa o modelo, a síntese, a transcrição nem todas as variações do reconhecimento por regras.

## Evoluções realizadas

- Execução local com Ollama/Qwen3, Whisper e Windows SAPI, sem chave de API paga.
- Confirmação explícita de rascunhos antes de alterar o saldo.
- Reconhecimento auxiliar de comandos estruturados quando não há resultado de ferramenta.
- Datas brasileiras e faladas, com mês por nome, algarismo ou número por extenso.
- Indicador descritivo de pagamentos classificados como `DIVIDAS` sobre receitas.
- Interface web, formulário manual, armazenamento local e testes das regras de negócio.

## Limitações observadas

- O modelo nem sempre aciona uma ferramenta. O resultado público não identifica o caminho usado (modelo ou reconhecimento auxiliar).
- O Whisper pode transcrever `2026` como `2006`. O código não adivinha o ano pretendido; confira os dados antes de confirmar.
- O reconhecimento auxiliar cobre formatos específicos; não é uma compreensão geral de português nem de todos os valores por extenso.
- Confirmar o mesmo ID é idempotente. Reenviar o comando pode criar um rascunho diferente.
- Não foi registrada uma nova confirmação da despesa de R$ 80,00 por áudio nesta verificação; os totais consultados continuam sendo os do cenário de dois lançamentos.
- Não há edição/exclusão de lançamentos pela interface, autenticação ou operação multiusuário.

## Evoluções relacionadas à área de crédito

Possibilidades futuras, ainda não implementadas:

- exportar lançamentos sintéticos para análise em Python;
- criar consultas SQL por categoria e mês;
- acompanhar indicadores descritivos de uma carteira sintética;
- integrar o CrediPolicy por um contrato explícito;
- documentar regras, período de referência e limitações de cada indicador.

SQL analítico, modelos de score, Spark e Databricks não fazem parte desta entrega. O saldo registrado não é score nem modelo de risco de crédito.

## Arquivos publicados

As capturas usam os dados fictícios do cenário. O banco em `dados-locais/`, o ambiente `.venv/`, o cache de modelos e as gravações de áudio ficam fora do pacote de entrega. Não publique dados pessoais, credenciais ou dados de clientes.
