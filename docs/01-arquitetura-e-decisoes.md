# Arquitetura e decisões

## Escopo

O CrediFlow AI é um protótipo educacional de fluxo de caixa. A aplicação trabalha exclusivamente com dados fictícios em uma instância local e não acessa nenhuma instituição financeira.

## Separação de responsabilidades

- `AssistenteService`: envia contexto ao modelo local, registra as ferramentas disponíveis e tenta o reconhecimento por regras quando a chamada termina sem resultado de ferramenta.
- `FluxoCaixaService`: valida lançamentos, controla rascunho/confirmado e calcula indicadores.
- `LancamentoRepository`: persiste rascunhos e confirmados. A consulta mensal filtra os confirmados.
- `AudioService`: chama exclusivamente o adaptador de áudio em `127.0.0.1`.
- Adaptador Python: transcreve com Whisper e sintetiza com Windows SAPI; não conhece o banco da aplicação.

O modelo não recebe acesso direto ao repositório. Ele apenas solicita uma ferramenta e fornece argumentos. A aplicação valida, executa e decide qual resultado pode ser mostrado.

## Confirmação e idempotência

Registrar uma transação pela IA cria `PENDENTE`. O saldo considera apenas `CONFIRMADO`. A confirmação usa o ID do rascunho e repetir a mesma confirmação mantém um único lançamento.

Essa proteção vale para o mesmo ID. Repetir o comando de criação pode gerar outro rascunho. Não existe deduplicação por conteúdo nem confirmação automática pela ferramenta do modelo.

## Linguagem e datas

O modelo local pode emitir uma chamada de ferramenta, pedir esclarecimento ou terminar sem chamada. Nesse último caso, o reconhecimento auxiliar procura os campos de um comando estruturado e chama as mesmas funções validadas. Um pedido de esclarecimento já retornado pelo modelo não é substituído por esse reconhecimento auxiliar.

As datas podem conter o nome do mês, o algarismo ou o número do mês por extenso: `12 de setembro de 2026`, `12 de 9 de 2026` e `12 de nove de 2026`, além de `12/09/2026` e `2026-09-12`. O ano transcrito é preservado. A pessoa deve revisar a transcrição e o rascunho, pois validar o formato não comprova que a fala foi entendida corretamente.

Os totais e a mensagem pública são produzidos pelas funções da aplicação. Uma resposta final de resumo, sozinha, não comprova se houve Tool Calling pelo modelo ou reconhecimento auxiliar por regras; a interface atual não diferencia esses caminhos.

## Indicadores

`receitas`, `despesas` e `saldo` são somas de valores confirmados no mês solicitado. `percentualDividasSobreReceitas` é calculado somente quando há receita e considera a categoria `DIVIDAS`. A aplicação retorna `null` quando não há receita, evitando divisão por zero.

Esses números não são score, PD, limite, CET, decisão de crédito ou garantia de pagamento. A conexão com o CrediPolicy fica como evolução futura, via contrato, sem misturar responsabilidades.

## Privacidade e operação local

O servidor Java e o adaptador Python fazem bind em loopback. Prompts, transcrições e áudios não são gravados em logs pela aplicação. O banco, modelos e ambientes Python locais são excluídos pelo `.gitignore`.
