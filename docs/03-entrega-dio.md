# Entrega do CrediFlow AI

## Descrição para a DIO

Desenvolvi o CrediFlow AI, um assistente educacional de fluxo de caixa com Spring Boot, Spring AI e modelos locais. A aplicação recebe comandos por texto ou áudio, transcreve a fala com Whisper, interpreta o pedido com Ollama/Qwen3 e disponibiliza ferramentas para preparar receitas/despesas fictícias e consultar o resumo mensal. As respostas podem ser ouvidas com a síntese de voz do Windows.

A evolução do projeto inclui confirmação explícita antes de alterar o saldo, reconhecimento auxiliar de comandos estruturados e datas faladas, persistência local em H2, interface web e indicadores descritivos de receitas, despesas, saldo e pagamentos de dívidas sobre receitas. Os cálculos usam BigDecimal e consideram apenas lançamentos confirmados.

O repositório inclui instruções de execução, exemplos, evidências e 12 testes automatizados de contexto e regras de negócio. O projeto aproxima o aprendizado de IA e Java do meu interesse em crédito e dados, com escopo educacional: não calcula score, não aprova crédito e não acessa bancos ou dados reais. O processamento usa recursos locais, sem API paga.

## GitHub

Nome sugerido: `crediflow-ai`.

Descrição curta: Assistente local de fluxo de caixa por texto e voz com Spring AI, Ollama, Whisper e confirmação de lançamentos fictícios.

Topics sugeridos (adicionar individualmente):

```text
java
spring-boot
spring-ai
ollama
qwen3
tool-calling
faster-whisper
speech-to-text
text-to-speech
cash-flow
financial-education
h2-database
python
dio
portfolio-project
```

## Publicação

1. Crie o repositório no GitHub. Se for enviar os arquivos pela interface, evite gerar outro README inicialmente.
2. Envie o conteúdo do pacote de entrega diretamente para a raiz do repositório. Confira `.mvn/wrapper/maven-wrapper.properties`, `mvnw`, `mvnw.cmd`, `pom.xml`, `src/`, `audio/`, `scripts/`, `docs/`, `evidencias/` e `README.md`.
3. Confira no GitHub se o README e suas imagens abrem e se a árvore contém os arquivos. Publicar o código não hospeda o aplicativo: ele continua sendo executado localmente.
4. Copie o link do repositório para a DIO e use a descrição acima.

O código e a documentação estão preparados localmente. A criação do repositório, a conferência remota e a entrega na DIO são etapas separadas e ainda não estão confirmadas.
