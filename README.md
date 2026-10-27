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
| Banco | MySQL 8.4 (docker-compose) |
| Migrations | Flyway 11.20 |
| Persistência | Spring Data JPA (Hibernate) |
| Testes | JUnit 5, MockK (springmockk), Testcontainers com MySQL real |

## Pré-requisitos

- **JDK 21.** O Gradle usa toolchain 21. Se o `java` não estiver no `PATH`, aponte o `JAVA_HOME` para um JDK 21 antes de rodar o wrapper. Exemplo no Git Bash:

  ```bash
  export JAVA_HOME="$HOME/.jdks/ms-21.0.11"
  ```

  E no PowerShell:

  ```powershell
  $env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-21.0.11"
  ```

- **Docker Desktop.** Usado pelo MySQL local (docker-compose) e pelos testes de integração (Testcontainers).

## Como executar

```bash
docker compose up -d --wait   # sobe o MySQL e espera o healthcheck
./gradlew test                # roda os testes (Testcontainers sobe o próprio MySQL)
./gradlew bootRun             # sobe a aplicação na porta 3003
```

A aplicação escuta na porta **3003**, porque o simulador envia os eventos para `http://localhost:3003/webhook`.

### Banco de dados

O `docker-compose.yml` sobe um MySQL 8.4 (`parking-mysql`) na porta 3306, com dados em um volume nomeado.

| Variável | Padrão |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/parking?connectionTimeZone=UTC` |
| `DB_USERNAME` | `parking` |
| `DB_PASSWORD` | `parking` |

O schema é criado pelo Flyway a partir de `src/main/resources/db/migration`. O Hibernate roda com `ddl-auto: validate`: ele só confere se as entidades batem com o schema e nunca o altera.

Para apagar os dados e recomeçar do zero:

```bash
docker compose down -v
```

## Decisões de interpretação

Esta seção é preenchida conforme as histórias são implementadas.

| Decisão | Justificativa | Alternativa descartada |
|---|---|---|
| Spring Boot 3.5.x | O enunciado exige Kotlin 2.1.x, e o Spring Boot 4 exige Kotlin 2.2 ou superior. A linha 3.5 é a mais recente compatível com Kotlin 2.1. | Spring Boot 4.x, incompatível com Kotlin 2.1. |
| Gradle 8.14 | O plugin do Kotlin 2.1 é homologado até a linha 8.x do Gradle. | Gradle 9.x, fora da faixa suportada pelo Kotlin 2.1. |
| MySQL 8.4 | É a versão LTS atual do MySQL. A 8.0 chegou ao fim do suporte em abril de 2026. | MySQL 8.0. |
| Flyway 11.20 em vez do 11.7 gerenciado pelo Spring Boot | O Flyway 11.7 avisa que só homologa MySQL até 8.1. A linha 11.20 é a mais recente da mesma versão maior, compatível com a autoconfiguração do Spring Boot 3.5, e suporta o MySQL 8.4. | Manter o 11.7 com o aviso, ou o Flyway 12, que muda de versão maior. |
| Schema só pelo Flyway, Hibernate em `validate` | Migrations versionadas são auditáveis e reproduzíveis. O `validate` faz a aplicação falhar na subida se as entidades divergirem do schema. | `ddl-auto: update`, que altera o schema de forma implícita. |
| Timestamps em UTC | O banco guarda instantes em UTC (`hibernate.jdbc.time_zone` e `connectionTimeZone`). O fuso de São Paulo só é aplicado na hora de definir o "dia" do faturamento. | Gravar no horário local, o que é ambíguo no horário de verão e depende do servidor. |
| `open-in-view: false` | A sessão JPA fica restrita à transação do serviço, sem consultas escondidas na camada web. | O padrão do Spring Boot (`true`), que gera aviso e mantém a conexão aberta durante a resposta. |
| Testcontainers com MySQL real | Os testes de integração rodam no mesmo banco de produção, o que pega diferenças de SQL, de lock e de tipos que o H2 esconderia. | Banco em memória (H2). |
| MockK no lugar do Mockito | MockK é a biblioteca de mocks idiomática para Kotlin (classes finais, funções de extensão, DSL). O Mockito é excluído do `spring-boot-starter-test` para não haver duas bibliotecas de mock. | Mockito com `mockito-kotlin`. |

## Branches e commits

Cada bloco funcional tem sua própria branch, criada a partir da `main` atualizada. Cada branch é revisada e mergeada na `main` antes de começar a próxima.
