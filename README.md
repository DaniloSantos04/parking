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

### Simulador

O simulador do enunciado (`cfontes0estapar/garage-sim:1.0.0`) expõe `GET /garage` na porta **3000** e envia os eventos para `http://localhost:3003/webhook`.

**Ordem de subida:**

1. MySQL (`docker compose up -d --wait`).
2. Simulador.
3. Aplicação.

A aplicação chama `GET /garage` ao subir. O simulador só começa a enviar eventos depois dessa primeira chamada.

```bash
./scripts/start-simulator.sh          # Git Bash, Linux ou Mac
```

```powershell
.\scripts\start-simulator.ps1         # PowerShell
```

Os scripts têm dois modos:

| Modo | Comando | Quando usar |
|---|---|---|
| `host` (padrão) | `docker run -d --network="host" ...`, igual ao enunciado | Linux, ou Docker Desktop com **Enable host networking** ligado (Settings → Resources → Network). Validado no Docker Desktop 4.86 no Windows. |
| `port` (plano B) | `-p 3000:3000 -e CLIENT_WEBHOOK_URL=http://host.docker.internal:3003/webhook` | Docker Desktop sem host networking. |

Exemplos: `./scripts/start-simulator.sh port` ou `.\scripts\start-simulator.ps1 -Mode port`.

Para acompanhar e parar:

```bash
docker logs -f garage-sim
```

```bash
docker rm -f garage-sim
```

#### Comportamento observado do simulador

Para não depender só do exemplo do enunciado, o simulador foi executado com um servidor de captura no lugar da aplicação. O que foi observado:

- **Formato do `GET /garage`.** O preço vem como `base_price` (snake_case), e não como `basePrice` do exemplo do enunciado. Vêm também campos que o enunciado não cita: `open_hour`, `close_hour` e `duration_limit_minutes` nos setores, e `occupied` nas vagas. A configuração padrão tem os setores A (R$ 40,50, 10 vagas) e B (R$ 4,10, 20 vagas), com 30 vagas no total.
- **Formato das datas.** `entry_time` e `exit_time` chegam **sem fuso e sem milissegundos** (`2026-10-27T22:00:24`), e não como no exemplo do enunciado (`2025-01-01T12:00:00.000Z`). O relógio do container está em UTC.
- **PARKED.** O `lat` e o `lng` correspondem exatamente às coordenadas de uma vaga do `GET /garage`.
- **Ritmo.** Há uma entrada a cada ~5 s, e o PARKED chega logo depois do ENTRY. Os carros ficam de segundos a poucos minutos, então quase todas as permanências caem nos 30 minutos grátis. Para ver receita diferente de zero, envie eventos manuais com horários controlados.
- **Sequência de eventos.** Numa execução de vários minutos, todo carro seguiu ENTRY → PARKED → EXIT. Não houve placa duplicada, PARKED sem ENTRY, EXIT sem PARKED nem vaga ocupada duas vezes. Os casos anômalos tratados pela aplicação são defensivos e ficam cobertos por testes.
- **Garagem cheia.** Com as 30 vagas ocupadas, o próprio simulador deixa de enviar ENTRY e loga `No available spots for entry.` até que uma saída libere vaga. O bloqueio de entradas a 100% na aplicação é, portanto, verificado nos testes automatizados, não pelo simulador.
- **Capacidade.** A soma do `max_capacity` dos setores (10 + 20) é igual ao número de vagas listadas (30).
- **Receita no log do simulador.** O simulador loga uma "receita por setor" própria a cada 15 s (por exemplo `{"A": 162.0, "B": 41.00000000000001}`). Nela, cada saída soma o `base_price` cheio, mesmo em permanências de segundos, sem os 30 minutos grátis, sem preço dinâmico e com aritmética de ponto flutuante. Esse número **não** é referência para o `GET /revenue`, que segue as regras do enunciado.
- **Simulação única.** Um novo `GET /garage` não reinicia a simulação. Se uma entrega ao webhook falha (aplicação fora do ar), o evento se perde e não há reenvio.
- **Variáveis de ambiente.** O binário lê `CLIENT_WEBHOOK_URL`, `SIMULATOR_PORT`, `ENTRY_INTERVAL_SECS` e `EXIT_INTERVAL_SECS`. A imagem declara `EXTERNAL_API_URL`, mas o binário **ignora** essa variável, e por isso o plano B usa `CLIENT_WEBHOOK_URL`.

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
| Simulador com `--network="host"` como padrão e plano B com `-p` | O modo `host` é o comando do enunciado e funciona no Docker Desktop com host networking ligado. O plano B atende quem não pode ligar essa opção, sem mudar nada na aplicação. | Só o comando do enunciado, que não funciona no Docker Desktop padrão. |
| `CLIENT_WEBHOOK_URL` no plano B | É a variável que o binário do simulador lê de fato, o que foi confirmado com o envio para `host.docker.internal`. A `EXTERNAL_API_URL` declarada na imagem não tem efeito. | `EXTERNAL_API_URL`, que mantém o envio para `localhost:3003` e falha no plano B. |
| Scripts em Bash e PowerShell | O ambiente de desenvolvimento é Mac ou Windows, e cada sistema roda o seu script sem dependências extras. | Só Bash, que exigiria Git Bash ou WSL no Windows. |
| MockK no lugar do Mockito | MockK é a biblioteca de mocks idiomática para Kotlin (classes finais, funções de extensão, DSL). O Mockito é excluído do `spring-boot-starter-test` para não haver duas bibliotecas de mock. | Mockito com `mockito-kotlin`. |

## Branches e commits

Cada bloco funcional tem sua própria branch, criada a partir da `main` atualizada. Cada branch é revisada e mergeada na `main` antes de começar a próxima.
