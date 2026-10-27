# Parking: desafio Estapar

Backend de gestão de estacionamento feito para o **Estapar Backend Developer Test (V1.5)**.

O sistema:

- busca a configuração da garagem (setores e vagas) no simulador quando sobe;
- recebe por webhook os eventos de entrada, estacionamento e saída de veículos;
- calcula o valor de cada permanência com preço dinâmico conforme a lotação;
- expõe a receita do dia por setor em `GET /revenue`.

> Este README cresce junto com o código: cada branch atualiza a seção que lhe cabe. As decisões de interpretação, com justificativa, ficam reunidas na seção [Decisões de interpretação](#decisões-de-interpretação).

## Stack

| Item | Escolha |
|---|---|
| Linguagem | Kotlin 2.1.21 |
| JVM | Java 21 (toolchain do Gradle) |
| Framework | Spring Boot 3.5.16 |
| Build | Gradle 8.14 com Kotlin DSL e wrapper |
| Testes | JUnit 5, MockK (springmockk) |

## Pré-requisitos

- **JDK 21.** O Gradle usa toolchain 21. Se o `java` não estiver no `PATH`, aponte o `JAVA_HOME` para um JDK 21 antes de rodar o wrapper. Exemplo no Git Bash:

  ```bash
  export JAVA_HOME="$HOME/.jdks/ms-21.0.11"
  ```

  E no PowerShell:

  ```powershell
  $env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.11"
  ```

## Como executar

```bash
./gradlew test       # roda os testes
./gradlew bootRun    # sobe a aplicação na porta 3003
```

A aplicação escuta na porta **3003**, porque o simulador envia os eventos para `http://localhost:3003/webhook`.

## Decisões de interpretação

Esta seção é preenchida conforme as histórias são implementadas.

| Decisão | Justificativa | Alternativa descartada |
|---|---|---|
| Spring Boot 3.5.x | O enunciado exige Kotlin 2.1.x, e o Spring Boot 4 exige Kotlin 2.2 ou superior. A linha 3.5 é a mais recente compatível com Kotlin 2.1. | Spring Boot 4.x, incompatível com Kotlin 2.1. |
| Gradle 8.14 | O plugin do Kotlin 2.1 é homologado até a linha 8.x do Gradle. | Gradle 9.x, fora da faixa suportada pelo Kotlin 2.1. |
| MockK no lugar do Mockito | MockK é a biblioteca de mocks idiomática para Kotlin (classes finais, funções de extensão, DSL). O Mockito é excluído do `spring-boot-starter-test` para não haver duas bibliotecas de mock. | Mockito com `mockito-kotlin`. |

## Branches e commits

Cada bloco funcional tem sua própria branch, criada a partir da `main` atualizada. Cada branch é revisada e mergeada na `main` antes de começar a próxima.
