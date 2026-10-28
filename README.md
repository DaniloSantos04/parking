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

### Configuração

| Propriedade (`application.yml`) | Variável de ambiente | Padrão | Uso |
|---|---|---|---|
| `estapar.simulator.base-url` | `SIMULATOR_URL` | `http://localhost:3000` | Endereço do simulador (`GET /garage`) |
| `estapar.simulator.retry.max-attempts` | — | `5` | Tentativas da carga inicial |
| `estapar.simulator.retry.interval` | — | `5s` | Espera entre as tentativas |
| `spring.http.client.connect-timeout` | — | `2s` | Timeout de conexão com o simulador |
| `spring.http.client.read-timeout` | — | `5s` | Timeout de leitura da resposta |

Qualquer propriedade pode ser sobrescrita na linha de comando, por exemplo `--estapar.simulator.retry.interval=1s`.

## Arquitetura

O projeto segue a **arquitetura hexagonal** (ports and adapters). O núcleo de regras não conhece HTTP, JSON nem JPA. Ele se comunica com o mundo por interfaces (ports), e as tecnologias ficam nas pontas (adapters).

```
                  ┌──────────────────────────── application ─────────────────────────────┐
  adapter/in      │  port/in (casos de uso)          service             port/out          │   adapter/out
 ──────────────   │  ─────────────────────          ─────────           ─────────────      │  ──────────────
  startup    ───▶ │  LoadGarageUseCase      ◀── GarageSetupService ──▶ FetchGarage...    ──┼─▶ simulator (HTTP)
  (runner)        │                                                    SaveGarage...     ──┼─▶ persistence (JPA)
  web        ───▶ │  RegisterEntryUseCase   ◀── EntryService ────────▶ LockGaragePort    ──┼─▶ persistence (JPA)
  (webhook)       │  RegisterParkedUseCase  ◀── ParkedService ───────▶ SessionPort       ──┼─▶ persistence (JPA)
                  │  RegisterExitUseCase    ◀── ExitService ─────────▶ SpotPort          ──┼─▶ persistence (JPA)
                  │                                  │                 SectorPort        ──┼─▶ persistence (JPA)
                  │                                  │                 AnomalyPort       ──┼─▶ persistence (JPA)
  web        ───▶ │  GetRevenueUseCase      ◀── RevenueService ──────▶ RevenuePort       ──┼─▶ persistence (JPA)
  (revenue)       │                                  │                 SectorPort        ──┼─▶ persistence (JPA)
                  │                                  ▼                                     │
                  │                     domain (modelo e regras puras)                     │
                  └────────────────────────────────────────────────────────────────────────┘
```

| Pacote | Responsabilidade |
|---|---|
| `domain.model` | Modelo puro, sem Spring, JPA ou Jackson: `Sector`, `Spot`, `Coordinates`, `GarageConfiguration`, `ParkingSession`, `Anomaly`, `LicensePlate`. As invariantes ficam nos construtores. |
| `domain.pricing` | Regras de preço: `PricingPolicy`/`OccupancyPricingPolicy`, `Occupancy` e `FeeCalculator`. |
| `application.port.in` | Casos de uso que a aplicação oferece (`LoadGarageUseCase`, `RegisterEntryUseCase`, `RegisterParkedUseCase`, `RegisterExitUseCase`, `GetRevenueUseCase`) e seus comandos e resultados (records). |
| `application.port.out` | O que a aplicação precisa de fora (`FetchGarageConfigurationPort`, `SaveGarageConfigurationPort`, `LockGaragePort`, `SessionPort`, `SpotPort`, `SectorPort`, `AnomalyPort`, `RevenuePort`). Interfaces pequenas e específicas. |
| `application.service` | Implementação dos casos de uso, orquestrando domínio e ports de saída. É onde ficam as transações. |
| `adapter.in.startup` | `GarageStartupRunner`: carga inicial com novas tentativas. |
| `adapter.in.web` | `WebhookController`, DTOs (records), leitura flexível de datas e `ApiExceptionHandler`. |
| `adapter.out.simulator` | Cliente HTTP do simulador e DTOs do `GET /garage`. |
| `adapter.out.persistence` | Entidades JPA, repositórios Spring Data e os adapters dos ports de saída. |
| `config` | Propriedades tipadas (`SimulatorProperties`) e os beans do domínio (`PricingPolicy`, `Clock`). |

**Regras de dependência:**
- `adapter` depende de `application`, que depende de `domain`. Nunca o contrário.
- Entidades JPA e DTOs nunca saem dos adapters: o mapeamento para o domínio acontece dentro deles.

**DTOs como records.** Os DTOs de entrada e saída e as propriedades de configuração são `@JvmRecord data class`, compilados como `java.lang.Record`. São imutáveis, com `equals` e `hashCode` por valor, e deixam explícito que só carregam dados entre camadas.

## Modelo de dados

| Tabela | Colunas principais | Restrições e motivo |
|---|---|---|
| `garage` | `id`, `capacity` | Linha única (`CHECK id = 1`), criada pela própria migration. É o alvo do lock pessimista que serializa os eventos e guarda a capacidade total. |
| `sector` | `code`, `base_price DECIMAL(10,2)`, `max_capacity` | PK no código do setor. `DECIMAL` para dinheiro, nunca ponto flutuante. |
| `spot` | `id`, `sector_code`, `lat`, `lng` (`DECIMAL(9,6)`), `occupied` | PK no id do simulador. FK para `sector`. `UNIQUE (lat, lng)`, porque o evento PARKED identifica a vaga só pelas coordenadas, e o índice único garante a busca exata e rápida. |
| `parking_session` | `license_plate`, `status`, `entry_time`, `price_multiplier`; no PARKED, `spot_id`, `sector_code` e `base_price`; no EXIT, `exit_time` e `amount` | Uma linha por permanência. Em quem saiu sem PARKED, `sector_code` fica `NULL` e `base_price` guarda o menor preço-base usado na cobrança, para o valor ser auditável. `CHECK` nos status válidos. A coluna gerada `active_plate` (a placa enquanto a sessão não terminou, `NULL` depois) tem índice único: o **banco** garante no máximo uma sessão ativa por placa. Índice em `status` para contar a lotação, e em `(status, exit_time, sector_code)` para o faturamento. |
| `anomaly` | `type`, `license_plate`, `detail`, `occurred_at` | Eventos que não puderam ser aplicados. Índice em `(type, occurred_at)` para auditoria. |

## API

### `POST /webhook`

Recebe os eventos do simulador. O tipo é escolhido pelo campo `event_type`.

**ENTRY**: um veículo passou pela cancela.

```json
{
  "license_plate": "ZUL0001",
  "entry_time": "2025-01-01T12:00:00.000Z",
  "event_type": "ENTRY"
}
```

**PARKED**: o veículo estacionou na vaga dessas coordenadas.

```json
{
  "license_plate": "ZUL0001",
  "lat": -23.561684,
  "lng": -46.655981,
  "event_type": "PARKED"
}
```

**Coordenadas.** Normalizadas para 6 casas decimais antes da busca, então `-23.5616840` e `-23.561684` são a mesma vaga. Latitude entre −90 e 90 e longitude entre −180 e 180; fora disso, a resposta é 400.

**EXIT**: o veículo saiu da garagem. O valor é calculado, a sessão é encerrada e a vaga é liberada.

```json
{
  "license_plate": "ZUL0001",
  "exit_time": "2025-01-01T12:00:00.000Z",
  "event_type": "EXIT"
}
```

**Datas.** São aceitas com fuso (`2025-01-01T12:00:00.000Z`, como no enunciado, ou `2025-01-01T09:00:00-03:00`) e sem fuso (`2025-01-01T12:00:00`, como o simulador envia). Datas sem fuso são lidas como UTC.

**Placa.** Normalizada (sem espaços nas pontas, em maiúsculas). Obrigatória, com até 20 caracteres.

| Situação | HTTP | Efeito |
|---|---|---|
| Evento válido e aplicado | 200 | Sessão criada ou atualizada |
| Evento válido recusado pela regra de negócio (garagem cheia, placa já dentro...) | 200 | Nada muda; anomalia registrada |
| JSON malformado, campo ausente ou inválido, `event_type` desconhecido | 400 | Corpo `application/problem+json` (RFC 9457) com o campo a corrigir |

Exemplo de 400:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Campo ausente ou inválido: entry_time",
  "instance": "/webhook"
}
```

### `GET /revenue`

Receita das saídas de um dia, da garagem inteira ou de um setor.

| Parâmetro (query) | Obrigatório | Padrão | Formato |
|---|---|---|---|
| `date` | não | dia atual em São Paulo | `AAAA-MM-DD` |
| `sector` | não | garagem inteira | código do setor (`A`, `B`...); espaços e caixa são ignorados |

```bash
curl "http://localhost:3003/revenue"                          # hoje, garagem inteira
curl "http://localhost:3003/revenue?date=2025-01-01"          # um dia, garagem inteira
curl "http://localhost:3003/revenue?date=2025-01-01&sector=A" # um dia, um setor
```

Resposta 200, no formato do enunciado:

```json
{
  "amount": 109.35,
  "currency": "BRL",
  "timestamp": "2026-10-28T18:56:19.697Z"
}
```

- **`amount`**: soma do valor cobrado nas saídas do dia, sempre com 2 casas (`0.00` quando não há receita).
- **`currency`**: sempre `BRL`.
- **`timestamp`**: o instante da apuração, em UTC com milissegundos.

Regras:

- **Dia da saída.** A receita entra no dia em que o carro **saiu**, no fuso de **São Paulo**. O dia vai de 00:00 a 00:00 em São Paulo, e os horários são guardados em UTC: uma saída às 02:30 UTC do dia 2 conta no dia 1 (23:30 em São Paulo).
- **Até o momento.** Para o dia atual, só as saídas já ocorridas contam. Dias futuros têm receita `0.00`.
- **Saídas sem setor.** Quem saiu sem PARKED entra no total da garagem, mas no de nenhum setor.

| Situação | HTTP | Exemplo de `detail` |
|---|---|---|
| Consulta válida | 200 | — |
| `date` fora do formato `AAAA-MM-DD`, ou data inexistente | 400 | `Parâmetro inválido: date = '28-10-2026' (esperado AAAA-MM-DD)` |
| `sector` que não existe na garagem | 404 | `Setor não encontrado: Z` |

### Eventos e anomalias

| Evento | Situação | Comportamento | Anomalia |
|---|---|---|---|
| ENTRY | Garagem com vaga | Cria sessão `ENTERED` com o multiplicador da lotação antes da entrada | — |
| ENTRY | Garagem 100% ocupada | Recusa a entrada | `GARAGE_FULL` |
| ENTRY | Placa já tem sessão ativa | Ignora o evento; a sessão existente não muda | `DUPLICATE_ENTRY` |
| PARKED | Sessão `ENTERED` e vaga livre nas coordenadas | Sessão vai para `PARKED` com vaga, setor e preço-base do setor; a vaga fica ocupada | — |
| PARKED | Placa sem sessão ativa | Ignora o evento | `PARKED_WITHOUT_ENTRY` |
| PARKED | Placa já estacionada | Ignora o evento; a vaga original é mantida | `ALREADY_PARKED` |
| PARKED | Coordenadas sem vaga correspondente | Ignora o evento; a sessão continua `ENTERED` | `SPOT_NOT_FOUND` |
| PARKED | Vaga já ocupada por outro carro | Ignora o evento; as duas sessões ficam como estavam | `SPOT_OCCUPIED` |
| EXIT | Sessão `PARKED` | Cobra `horas × preço-base do setor × multiplicador`, encerra a sessão (`EXITED`) e libera a vaga | — |
| EXIT | Sessão `ENTERED` (sem PARKED válido) | Cobra pelo **menor preço-base** da garagem, sem setor, e encerra a sessão | `EXIT_WITHOUT_PARKED` |
| EXIT | Placa sem sessão ativa (nunca entrou ou já saiu) | Ignora o evento; nada é cobrado de novo | `EXIT_WITHOUT_ACTIVE_SESSION` |
| EXIT | Horário de saída anterior ao de entrada | Encerra a sessão com valor R$ 0,00 e libera a vaga | `EXIT_BEFORE_ENTRY` |

Uma sessão que continua `ENTERED` (sem PARKED válido) ainda ocupa lugar na lotação até o EXIT. Encerrada a sessão, a lotação cai na hora: se a garagem estava cheia, o próximo ENTRY já é aceito.

Cada anomalia é logada em `WARN` e gravada na tabela `anomaly`:

```bash
docker exec parking-mysql mysql -uparking -pparking parking -e "select * from anomaly order by id desc limit 20"
```

### Concorrência

Todo evento que muda a lotação roda numa transação que começa com `SELECT ... FOR UPDATE` na linha única da `garage` (`LockGaragePort`). Com isso, contar as sessões ativas, decidir o preço e gravar a sessão acontece de forma serializada: duas entradas simultâneas nunca enxergam a mesma vaga livre. Como segunda barreira, o índice único em `active_plate` impede duas sessões ativas da mesma placa mesmo se o código falhar.

O custo é serializar os eventos da garagem. Para o volume de uma garagem (um evento a cada poucos segundos, transações de milissegundos), isso é desprezível e mantém a regra simples e correta.

O `ConcurrencyTest` prova isso com threads simultâneas contra o MySQL real (ver [Testes](#testes)). Com o `FOR UPDATE` removido, 3 dos 4 cenários falham: entram mais carros que vagas, a mesma placa ganha mais de uma sessão e a mesma vaga é ocupada duas vezes.

## Carga inicial da garagem

1. Ao subir, o `GarageStartupRunner` chama `LoadGarageUseCase`.
2. O caso de uso busca `GET /garage` no simulador e valida a configuração no domínio: ao menos um setor, sem setor, id ou coordenada duplicados, e toda vaga em um setor existente.
3. O adapter de persistência grava setores, vagas e capacidade numa única transação.
4. Se a carga falha, há nova tentativa: 5 tentativas com 5 s de intervalo. Se todas falharem, a aplicação **não sobe** e loga a URL e o motivo. Exemplo:

   ```
   Não foi possível carregar a garagem de http://localhost:3000 após 5 tentativas: ResourceAccessException: I/O error on GET request for "http://localhost:3000/garage": null (causa raiz: ClosedChannelException: sem mensagem)
   ```

5. A carga é **idempotente**: subir de novo não duplica nada, atualiza preço e capacidade se mudaram e preserva a ocupação atual das vagas.

## Regras de negócio

### Preço dinâmico e lotação

A garagem tem um único grupo de cancelas, na entrada. Os setores são divisões lógicas. Por isso a lotação é medida para a **garagem inteira**: se o setor A lota e o setor B tem vaga, a entrada continua liberada.

O preço é decidido no **ENTRY**, pela lotação de **antes** da entrada do carro (as sessões ativas já existentes), e fica travado para a permanência inteira. A regra está em `OccupancyPricingPolicy` (`domain.pricing`):

| Lotação antes da entrada | Ajuste | Multiplicador |
|---|---|---|
| abaixo de 25% | −10% | 0,90 |
| de 25% até 50% (inclusive) | 0% | 1,00 |
| acima de 50% até 75% (inclusive) | +10% | 1,10 |
| acima de 75% e abaixo de 100% | +25% | 1,25 |
| 100% | garagem fechada | — |

Exemplos numa garagem de 100 vagas:

| Carros dentro | Lotação | Resultado para o próximo carro |
|---|---|---|
| 24 | 24% | entra com 0,90 |
| 25 | 25% | entra com 1,00 |
| 50 | 50% | entra com 1,00 |
| 51 | 51% | entra com 1,10 |
| 75 | 75% | entra com 1,10 |
| 76 | 76% | entra com 1,25 |
| 99 | 99% | entra com 1,25 (e a garagem fica cheia) |
| 100 | 100% | recusado até uma saída liberar vaga |

Na garagem do simulador (30 vagas), os limites caem entre números inteiros de carros: 7 dentro (23,3%) dá 0,90, 8 dentro (26,7%) dá 1,00, 15 (50%) dá 1,00, 16 (53,3%) dá 1,10, 22 (73,3%) dá 1,10 e 23 (76,7%) dá 1,25.

### Tarifa

O valor é calculado na saída por `FeeCalculator` (`domain.pricing`):

1. Permanência de **até 30 minutos (inclusive)**: R$ 0,00.
2. Acima de 30 minutos: cobra **horas cheias, arredondadas para cima, contando desde a primeira hora**. Hora iniciada é hora cobrada.
3. `valor = horas × preço-base do setor × multiplicador travado no ENTRY`.
4. Só o valor final é arredondado, para 2 casas com `HALF_UP`. Todo o cálculo usa `BigDecimal`.

| Permanência | Horas cobradas | Preço-base | Multiplicador | Valor |
|---|---|---|---|---|
| 30 min | 0 | R$ 10,00 | 1,00 | R$ 0,00 |
| 30 min e 1 s | 1 | R$ 10,00 | 1,00 | R$ 10,00 |
| 31 min | 1 | R$ 10,00 | 1,10 | R$ 11,00 |
| 1 h | 1 | R$ 10,00 | 0,90 | R$ 9,00 |
| 1 h e 1 s | 2 | R$ 10,00 | 1,00 | R$ 20,00 |
| 61 min | 2 | R$ 10,00 | 1,10 | R$ 22,00 |
| 1 h | 1 | R$ 40,50 (setor A) | 1,25 | R$ 50,63 (50,625) |
| 3 h | 3 | R$ 4,10 (setor B) | 1,25 | R$ 15,38 (15,375) |

## Testes

```bash
./gradlew test
```

São 171 testes, que levam cerca de 2 minutos. Exigem o Docker rodando: o Testcontainers sobe um MySQL 8.4 e, no teste ponta a ponta, o próprio simulador. O relatório fica em `build/reports/tests/test/index.html`.

| Nível | Exemplos | O que garante |
|---|---|---|
| **Domínio** (unitário, sem Spring) | `OccupancyPricingPolicyTest`, `FeeCalculatorTest`, `ParkingSessionTest`, `GarageConfigurationTest`, `CoordinatesTest` | As regras do enunciado nos limites exatos: faixas de lotação (24/25/50/51/75/76/99/100 de 100 e os limites da garagem de 30 vagas), 30 minutos grátis (30 min e 30 min + 1 ms), arredondamento de horas e centavos, e as transições ENTERED → PARKED → EXITED. |
| **Aplicação** (unitário, ports com MockK) | `EntryServiceTest`, `ParkedServiceTest`, `ExitServiceTest`, `RevenueServiceTest`, `GarageSetupServiceTest`, `GarageStartupRunnerTest` | Orquestração de cada caso de uso, a ordem lock → consulta → gravação, cada tipo de anomalia, o intervalo de faturamento em São Paulo e as novas tentativas da carga inicial. |
| **Adapters** | `SimulatorGarageAdapterTest` (`@RestClientTest`), `FlexibleInstantDeserializerTest` | Leitura do JSON real do simulador (`base_price`, campos extras) e do formato do enunciado (`basePrice`), e datas com e sem fuso. |
| **Integração** (aplicação completa + MySQL real, simulador falso, relógio fixo) | `WebhookEntryTest`, `WebhookParkedTest`, `WebhookExitTest`, `RevenueControllerTest`, `GarageConfigurationPersistenceAdapterTest`, `LicensePlateColumnTest` | HTTP → serviço → banco, com os códigos 200/400/404 e o corpo das respostas. Cobre a garagem cheia que reabre após uma saída, o faturamento por dia e setor, a carga idempotente e as restrições do schema (sessão ativa única por placa, tamanho das colunas). |
| **Concorrência** | `ConcurrencyTest` | 20 entradas simultâneas com 5 vagas aceitam exatamente 5, com multiplicadores 0,90/0,90/1,00/1,10/1,25. Também: a mesma placa simultânea gera uma sessão; o mesmo PARKED simultâneo ocupa a vaga uma vez; saídas e entradas simultâneas nunca passam da capacidade. |
| **Ponta a ponta com o simulador real** | `SimulatorEndToEndTest` | Sobe a imagem `cfontes0estapar/garage-sim:1.0.0` via Testcontainers e a aplicação completa. Carrega a garagem real (30 vagas) e processa os eventos reais até haver saídas, sem nenhum 400 e sem anomalias. Confere que a ocupação das vagas bate com as sessões e que o `GET /revenue` bate com a soma das saídas gravadas. |

### Roteiro de teste manual com o simulador

1. Suba o banco e o simulador:

   ```bash
   docker compose up -d --wait
   ./scripts/start-simulator.sh          # ou: .\scripts\start-simulator.ps1
   ```

2. Suba a aplicação:

   ```bash
   ./gradlew bootRun
   ```

   O log deve mostrar `Garagem carregada de http://localhost:3000: 2 setores, 30 vagas, capacidade 30`. Em seguida aparecem os eventos (`Evento recebido no webhook: EntryEvent(...)`, `ParkedEvent(...)`, `ExitEvent(...)`) e o processamento de cada um (`ENTRY ... multiplicador 0.90`, `PARKED ... na vaga 6 do setor A`, `EXIT ... valor R$ 0.00`).

3. Acompanhe o estado no banco (horários em UTC):

   ```bash
   docker exec parking-mysql mysql -uparking -pparking parking -e "select status, count(*) from parking_session group by status; select count(*) vagas_ocupadas from spot where occupied; select * from anomaly"
   ```

   O número de vagas ocupadas deve ser igual ao de sessões `PARKED`, e a tabela `anomaly` deve continuar vazia.

4. Consulte o faturamento:

   ```bash
   curl "http://localhost:3003/revenue"
   ```

   Com o simulador, as permanências costumam durar segundos ou poucos minutos, dentro dos 30 minutos grátis, então o valor tende a ser `0.00`.

5. Para ver uma cobrança, simule uma permanência de 2h30 com horários de hoje (troque as datas). Escolha uma vaga livre do setor A (`select id, lat, lng from spot where not occupied and sector_code = 'A'`):

   ```bash
   curl -H 'Content-Type: application/json' -d '{"license_plate":"TST0001","entry_time":"2026-10-28T12:00:00.000Z","event_type":"ENTRY"}' http://localhost:3003/webhook
   curl -H 'Content-Type: application/json' -d '{"license_plate":"TST0001","lat":-23.561684,"lng":-46.655981,"event_type":"PARKED"}' http://localhost:3003/webhook
   curl -H 'Content-Type: application/json' -d '{"license_plate":"TST0001","exit_time":"2026-10-28T14:30:00.000Z","event_type":"EXIT"}' http://localhost:3003/webhook
   curl "http://localhost:3003/revenue?date=2026-10-28&sector=A"
   ```

   São 3 horas cobradas × R$ 40,50 × o multiplicador da lotação na entrada. Com menos de 25% de lotação: R$ 109,35.

6. Confira os erros:

   ```bash
   curl -i "http://localhost:3003/revenue?sector=Z"           # 404
   curl -i "http://localhost:3003/revenue?date=28-10-2026"    # 400
   curl -i -H 'Content-Type: application/json' -d '{"license_plate":"X","event_type":"ENTRY"}' http://localhost:3003/webhook   # 400 entry_time
   ```

7. Para recomeçar do zero:

   ```bash
   docker rm -f garage-sim
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
| Timestamps em UTC | O banco guarda instantes em UTC (`hibernate.jdbc.time_zone` e `connectionTimeZone`). O fuso de São Paulo só é aplicado na hora de definir o "dia" do faturamento. | Gravar no horário local, o que deixa o dado dependente do fuso do servidor e ambíguo se o fuso mudar de regra. |
| `open-in-view: false` | A sessão JPA fica restrita à transação do serviço, sem consultas escondidas na camada web. | O padrão do Spring Boot (`true`), que gera aviso e mantém a conexão aberta durante a resposta. |
| Testcontainers com MySQL real | Os testes de integração rodam no mesmo banco de produção, o que pega diferenças de SQL, de lock e de tipos que o H2 esconderia. | Banco em memória (H2). |
| Simulador com `--network="host"` como padrão e plano B com `-p` | O modo `host` é o comando do enunciado e funciona no Docker Desktop com host networking ligado. O plano B atende quem não pode ligar essa opção, sem mudar nada na aplicação. | Só o comando do enunciado, que não funciona no Docker Desktop padrão. |
| `CLIENT_WEBHOOK_URL` no plano B | É a variável que o binário do simulador lê de fato, o que foi confirmado com o envio para `host.docker.internal`. A `EXTERNAL_API_URL` declarada na imagem não tem efeito. | `EXTERNAL_API_URL`, que mantém o envio para `localhost:3003` e falha no plano B. |
| Scripts em Bash e PowerShell | O ambiente de desenvolvimento é Mac ou Windows, e cada sistema roda o seu script sem dependências extras. | Só Bash, que exigiria Git Bash ou WSL no Windows. |
| Arquitetura hexagonal | Isola as regras de negócio de HTTP, JSON e banco. Cada regra pode ser testada sem infraestrutura, e trocar o simulador ou o banco afeta só um adapter. | Camadas tradicionais (controller → service → repository), em que o serviço depende direto do JPA. |
| Aceitar `base_price` e `basePrice` no `GET /garage` | O simulador real envia `base_price`, e o exemplo do enunciado mostra `basePrice`. Com `@JsonAlias`, a aplicação funciona com os dois sem código condicional. | Aceitar só um formato, o que quebraria com o simulador real ou com o contrato do enunciado. |
| Ignorar `open_hour`, `close_hour`, `duration_limit_minutes` e `occupied` do `GET /garage` | O enunciado não define regra para horário de funcionamento nem para limite de permanência (YAGNI). A ocupação é estado da aplicação, derivado dos eventos, e não configuração. | Implementar regras que o enunciado não pede. |
| Capacidade da garagem = soma do `max_capacity` dos setores | O enunciado define a capacidade por setor. Como a lotação é controlada para a garagem inteira, a capacidade total é a soma. No simulador, ela é igual ao número de vagas listadas (30). | Contar as vagas listadas, o que ignoraria o `max_capacity` declarado. |
| Garagem como linha única na tabela `garage` | Dá um ponto único para o lock pessimista (`SELECT ... FOR UPDATE`) que serializa os eventos e guarda a capacidade sem recalcular a soma a cada entrada. A linha nasce na migration, então o lock nunca encontra a tabela vazia. | Lock em tabela (`LOCK TABLES`) ou recalcular a capacidade a cada evento. |
| Coordenadas em `DECIMAL(9,6)`, normalizadas no domínio | O PARKED identifica a vaga só por lat/lng. Com escala fixa e `BigDecimal`, a comparação é por igualdade exata, sem erro de ponto flutuante. | `DOUBLE` com comparação por tolerância. |
| Carga idempotente por upsert, preservando `occupied` | A aplicação pode reiniciar com carros dentro da garagem. Regravar a configuração não pode apagar o estado de ocupação. | Apagar e recriar setores e vagas a cada subida. |
| Setores e vagas ausentes numa recarga não são apagados | Eles podem estar referenciados por sessões existentes. Apagar exigiria regras de migração de carros que o enunciado não define. | Remover o que não veio na nova configuração. |
| Validar a configuração no domínio | Setor, id ou coordenada duplicados, ou vaga de setor inexistente, tornariam a lotação e a busca por coordenada ambíguas. Falhar cedo, na subida, é melhor que corromper dados depois. | Confiar no simulador e gravar o que vier. |
| Retry com laço simples, falha na subida | São 5 tentativas com 5 s de intervalo, configuráveis. O laço explícito é fácil de ler e testar, sem dependência extra. Sem garagem não há como processar eventos, então a aplicação não sobe e loga URL e causa raiz. | `spring-retry`, ou subir sem garagem e falhar a cada evento. |
| Sem transação durante a chamada HTTP | O serviço de carga busca a configuração fora de transação. Só a gravação é transacional, no adapter de persistência, para que uma resposta lenta do simulador não segure conexão do banco. | `@Transactional` em todo o caso de uso. |
| Timeouts no cliente HTTP | Uma tentativa não pode ficar pendurada indefinidamente: o padrão é 2 s para conectar e 5 s para ler. | Timeouts infinitos do cliente HTTP padrão. |
| Concorrência testada nos casos de uso, com threads reais | Chamar os casos de uso direto, sem HTTP, isola o que está sendo provado: transação, lock e banco. Um `CountDownLatch` solta todas as threads ao mesmo tempo para maximizar a disputa. | Teste de carga externo, mais lento e menos determinístico. |
| Teste ponta a ponta com o simulador real via Testcontainers | Prova que o contrato real do simulador (formatos, ordem dos eventos, porta) funciona com a aplicação inteira, sem depender de alguém subir o simulador à mão. O simulador não tem semente fixa, por isso o teste verifica **invariantes** (nenhum 400, nenhuma anomalia, vagas ocupadas = sessões PARKED, receita = soma das saídas), e não valores exatos. | Só teste manual com o simulador. |
| Simulador falso nos testes de integração | Os testes de integração substituem só a busca no simulador (`@Primary`) e usam o resto da aplicação real, inclusive MySQL. O cliente HTTP é testado à parte com `MockRestServiceServer` e o JSON real capturado do simulador. | Depender do simulador rodando para executar os testes. |
| Lotação da garagem inteira, não do setor | O enunciado diz que há um único grupo de cancelas na entrada e que os setores são divisões lógicas. A cancela não sabe para qual setor o carro vai, então só pode fechar quando a garagem toda está cheia. | Lotação por setor, que bloquearia entradas com vagas livres em outro setor. |
| Lotação medida antes da entrada do carro | O preço é cobrado de quem entra pela situação que encontrou. Assim, o carro que leva a garagem a 25% ainda paga −10%, e com 99 de 100 vagas ocupadas o último carro entra. | Contar o próprio carro, o que deslocaria as faixas em um carro e impediria ocupar a última vaga. |
| Preço travado no ENTRY | O enunciado liga o ajuste à lotação; o momento coerente é o da entrada, quando a cancela decide. O multiplicador fica gravado na sessão e não muda com a lotação até a saída. | Recalcular na saída pela lotação daquele momento, o que torna o preço imprevisível para o cliente. |
| Limites das faixas: `< 25%`, `≤ 50%`, `≤ 75%`, `< 100%` | O enunciado diz "menor que 25%" e "menor até 50%/75%/100%". Isso foi lido como 25% exatos já fora do desconto, e 50% e 75% exatos ainda na faixa de baixo. 100% fecha a garagem. Os testes cobrem cada limite (24, 25, 50, 51, 75, 76, 99 e 100 de 100). | Outras leituras de "menor até", que movem os limites em um carro. |
| Comparação de lotação com aritmética inteira | `ocupados × 100 < capacidade × 25` é exato. Dividir antes (por exemplo 7/30 = 0,2333…) introduziria arredondamento justamente nos limites. | Calcular a porcentagem em `Double`. |
| `PricingPolicy` como interface no domínio | A regra de preço é a parte do sistema com mais chance de mudar. Os serviços dependem da abstração, e uma nova política entra como nova implementação, sem alterar quem a usa (aberto/fechado e inversão de dependência). | Regra embutida no serviço de entrada. |
| 30 minutos grátis, inclusive | O enunciado diz "primeiros 30 minutos são grátis". Exatamente 30 minutos ainda está dentro dos primeiros 30, então a cobrança começa no primeiro instante depois deles (30 min e 1 ms já paga). | Cobrar a partir de 30 minutos exatos. |
| Primeira hora cobrada inteira | O enunciado diz "inclusive a primeira hora". Quem passa dos 30 minutos paga a primeira hora cheia, sem descontar os 30 minutos grátis. | Descontar os 30 minutos e cobrar só o excedente. |
| Hora iniciada é hora cobrada, com precisão de milissegundos | "Arredonde para cima" aplicado à duração exata: 1 h e 1 s são 2 horas. As datas do simulador têm segundos, as do enunciado têm milissegundos, e o cálculo trata ambas sem truncar para minutos. | Arredondar por minutos inteiros, o que perde segundos. |
| Horas arredondadas antes do multiplicador | O ajuste dinâmico incide sobre o preço da permanência cobrada. Arredondar a duração primeiro mantém a regra "hora cheia" independente do ajuste. | Aplicar o multiplicador à duração fracionária. |
| Arredondamento só no valor final, `HALF_UP` | Evita acumular erro de arredondamento por hora (3 h × R$ 4,10 × 1,25 dá R$ 15,38, não R$ 15,39). `HALF_UP` é o arredondamento comercial usual. | Arredondar o preço de cada hora, ou `HALF_EVEN`. |
| `BigDecimal` para dinheiro | Valores monetários exatos. Com ponto flutuante, R$ 4,10 × 10 vira 41.00000000000001, como mostra o próprio log do simulador. | `Double`. |
| Saída anterior à entrada é rejeitada pelo cálculo | É dado inconsistente: o domínio não inventa um valor. Quem chama (o processamento do EXIT) decide como registrar a anomalia. | Calcular com duração negativa ou zerar em silêncio. |
| `FeeCalculator` como `object` | A tarifa é uma função pura e fixa do enunciado, sem estado nem variação prevista. | Interface com implementação, sem necessidade concreta (YAGNI). |
| Webhook responde 200 para recusas de negócio | O contrato do enunciado define `HTTP 200` para os eventos. Uma garagem cheia ou um ENTRY repetido não são erros do cliente: o evento foi entendido e tratado. Responder 4xx faria o simulador registrar falhas que não existem. | 409 ou 422 para recusas. |
| Webhook responde 400 para payload malformado | JSON inválido, campo ausente ou `event_type` desconhecido não podem ser processados. 400 sinaliza o problema a quem enviou, sem criar dados inconsistentes. | Responder 200 e descartar em silêncio. |
| Erros em Problem Details (RFC 9457) | Formato padrão de erro HTTP, já suportado pelo Spring, com `detail` dizendo qual campo corrigir. | Formato de erro próprio. |
| Datas sem fuso lidas como UTC | O simulador envia `2026-10-27T22:00:24`, sem fuso, e o relógio do container está em UTC: isso foi conferido comparando com o relógio do host. Datas com fuso, como no enunciado, são respeitadas. | Recusar datas sem fuso (quebraria com o simulador real) ou lê-las no fuso de São Paulo (deslocaria tudo em 3 horas). |
| Placa normalizada (trim e maiúsculas) | A placa identifica o carro entre os eventos. Diferenças de caixa ou espaços não podem criar dois carros. | Comparar a placa exatamente como chegou. |
| ENTRY repetido é ignorado e a sessão original é mantida | O primeiro ENTRY é o que abriu a cancela. Um segundo ENTRY sem EXIT é evento duplicado ou fora de ordem; sobrescrever mudaria o horário e o preço já travados. | Substituir a sessão pelo ENTRY mais recente. |
| Uma sessão ativa por placa garantida também no banco | O lock já serializa os eventos. A coluna gerada `active_plate` com índice único é uma segunda barreira, no próprio MySQL, contra duplicidade, que custa uma coluna. | Confiar só na verificação do código. |
| Lock pessimista na linha da garagem | A decisão de entrada depende de uma contagem (lotação) e de uma escrita (sessão) que precisam ser atômicas. O lock numa linha única é simples, explícito e correto sob concorrência. | Lock otimista com `@Version`, que exigiria novas tentativas no webhook, ou nível de isolamento `SERIALIZABLE` em todas as transações. |
| Anomalias em tabela própria | O webhook responde 200 mesmo quando recusa um evento, então a recusa precisa ficar visível. Uma tabela consultável permite auditar divergências entre a lotação do sistema e a física. | Só log. |
| `Clock` injetado | O horário das anomalias e o "agora" do faturamento vêm de um `Clock` bean em UTC. Nos testes de integração, um relógio fixo torna "hoje" e "até o momento" determinísticos. | `Instant.now()` espalhado pelo código. |
| `GET /revenue` com parâmetros na query string | O enunciado mostra `date` e `sector` num corpo JSON, mas corpo em `GET` não tem semântica definida (RFC 9110), e proxies e clientes HTTP podem descartá-lo. Query string é o padrão REST para filtros de leitura, funciona em qualquer cliente e pode ir para cache. | Corpo JSON no `GET`, como no exemplo do enunciado. |
| `date` e `sector` opcionais | Sem `date`, vale o dia atual ("até o momento para o dia atual", como pede o enunciado). Sem `sector`, a receita é da garagem inteira. | Exigir os dois parâmetros. |
| Receita no dia da **saída**, no fuso de São Paulo | O valor só existe quando o carro sai, e o "dia" de uma garagem em São Paulo é o dia local. Os horários continuam guardados em UTC e só a consulta converte o intervalo. | Dia da entrada, ou dia em UTC (que deslocaria 3 horas do faturamento para o dia seguinte). |
| "Até o momento" limita o intervalo ao instante atual | O enunciado pede a receita "até o momento para o dia atual". O intervalo do dia termina no "agora": uma saída com horário futuro (relógio do simulador adiantado, por exemplo) não entra antes de acontecer. Dias futuros têm receita zero sem consultar o banco. | Somar o dia inteiro, inclusive saídas com horário futuro. |
| Saídas sem setor só no total | Quem saiu sem PARKED não tem setor. Somar essa receita a um setor qualquer distorceria o faturamento daquele setor. Ela continua no total da garagem. | Atribuir ao setor de menor preço-base. |
| 404 para setor inexistente, 400 para data inválida | Setor que não existe é recurso não encontrado. Data fora de `AAAA-MM-DD`, ou inexistente como `2025-02-30`, é parâmetro malformado. As duas respostas vêm em Problem Details com `detail` explicando o erro. | Responder 200 com `amount: 0.00`, o que esconderia o erro do cliente. |
| Setor normalizado; em branco equivale a ausente | `?sector=a` e `?sector=A` são o mesmo setor, como acontece com a placa. `?sector=` vazio é tratado como "sem filtro", porque não identifica nenhum setor. | Diferenciar maiúsculas e minúsculas, ou responder 404 para setor vazio. |
| `timestamp` é o instante da apuração | O exemplo do enunciado não diz o que o campo representa. O instante em que a receita foi calculada deixa claro até quando o valor "até o momento" vale. Formato fixo `AAAA-MM-DDTHH:mm:ss.SSSZ` em UTC, como no exemplo. | O início do dia consultado. |
| Vaga identificada pelas coordenadas exatas | O PARKED só traz lat/lng. Com a normalização para 6 casas no domínio e o índice único `(lat, lng)`, a busca é por igualdade, e o simulador envia exatamente as coordenadas do `GET /garage`, como foi verificado. | Buscar a vaga mais próxima por distância, o que mascararia coordenadas erradas. |
| Setor e preço-base definidos no PARKED | O ENTRY não diz para qual setor o carro vai. O setor só é conhecido quando ele estaciona, e é dele que vem o `basePrice` usado na saída. O multiplicador continua o travado no ENTRY. | Definir o setor no ENTRY, o que é impossível sem coordenadas. |
| Vaga ocupada é anomalia e nada muda | Se a vaga já tem outro carro, não há como saber qual dos dois eventos está errado. Manter o estado e registrar a anomalia não corrompe o dado existente e deixa a divergência visível. | Mover o outro carro, ou aceitar duas sessões na mesma vaga. |
| PARKED repetido é anomalia separada (`ALREADY_PARKED`) | Distingue "carro que nunca entrou" (`PARKED_WITHOUT_ENTRY`) de "carro já estacionado que mandou PARKED de novo", o que facilita a auditoria. A vaga original é mantida pelo mesmo motivo do ENTRY repetido. | Um único tipo para os dois casos, ou trocar de vaga. |
| PARKED também sob o lock da garagem | Dois PARKED simultâneos para a mesma vaga não podem ambos vê-la livre. O mesmo lock do ENTRY serializa a checagem e a ocupação. | Lock na linha da vaga, que exigiria uma ordem de locks entre garagem e vaga para evitar deadlock com o EXIT. |
| Coordenadas fora da faixa geográfica respondem 400 | Latitude fora de −90..90 ou longitude fora de −180..180 não é uma posição: é payload inválido, não uma anomalia de negócio. | Tratar como `SPOT_NOT_FOUND`. |
| Sessão sem PARKED é cobrada na saída pelo menor preço-base da garagem | O carro passou pela cancela e ocupou lugar na garagem, então a permanência é cobrada normalmente. Sem PARKED não há setor, e por isso o preço-base é o menor da garagem (favorável ao cliente, já que não se sabe onde ele parou). A receita fica sem setor: entra no faturamento total, não no de nenhum setor. O preço-base usado fica gravado na sessão e o caso é registrado como anomalia. | Não cobrar, ou cobrar pelo maior preço-base. |
| EXIT repetido não cobra de novo | Depois do primeiro EXIT a sessão está encerrada, então um segundo EXIT da mesma placa não encontra sessão ativa: é ignorado e vira `EXIT_WITHOUT_ACTIVE_SESSION`. O valor e o horário da primeira saída ficam como estão. | Recalcular o valor com o horário do último EXIT. |
| Saída anterior à entrada encerra com R$ 0,00 | O carro saiu (o evento diz isso), então a sessão precisa ser encerrada para liberar a vaga e a lotação. Mas uma duração negativa não tem valor a cobrar: o domínio cobra zero e o caso vira `EXIT_BEFORE_ENTRY` para auditoria. O `exit_time` é gravado como recebido. | Ignorar o EXIT e deixar o carro "preso" na lotação, ou trocar os horários. |
| EXIT sob o lock da garagem, com a vaga liberada na mesma transação | Encerrar a sessão, liberar a vaga e reabrir a cancela são uma operação atômica. Um ENTRY concorrente nunca vê a vaga liberada com a sessão ainda ativa, nem o contrário. | Liberar a vaga numa transação separada. |
| A placa pode entrar de novo depois de sair | A unicidade é só de sessão **ativa** (`active_plate` vira `NULL` no EXIT). O histórico de permanências da placa fica preservado, uma linha por visita. | Reaproveitar a mesma linha a cada visita, o que apagaria o histórico do faturamento. |
| `spot.occupied` gravado na vaga | A pergunta "esta vaga está livre?" é respondida direto pela vaga, sob o lock, sem varrer sessões. O EXIT libera a vaga na mesma transação em que encerra a sessão. | Derivar a ocupação das sessões `PARKED` a cada consulta. |
| MockK no lugar do Mockito | MockK é a biblioteca de mocks idiomática para Kotlin (classes finais, funções de extensão, DSL). O Mockito é excluído do `spring-boot-starter-test` para não haver duas bibliotecas de mock. | Mockito com `mockito-kotlin`. |

## Branches e commits

Cada bloco funcional tem sua própria branch, criada a partir da `main` atualizada. Cada branch é revisada e mergeada na `main` antes de começar a próxima.
