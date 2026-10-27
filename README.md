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
                 ┌──────────────────────── application ────────────────────────┐
  adapter/in     │  port/in (casos de uso)        service        port/out      │     adapter/out
 ─────────────   │  ─────────────────────        ─────────      ──────────────  │   ───────────────
  startup   ───▶ │  LoadGarageUseCase   ◀──── GarageSetupService ───▶ Fetch... ─┼──▶ simulator (HTTP)
  (runner)       │                                     │            Save...  ─┼──▶ persistence (JPA)
                 │                                     ▼                       │
                 │                            domain (modelo puro)             │
                 └─────────────────────────────────────────────────────────────┘
```

| Pacote | Responsabilidade |
|---|---|
| `domain.model` | Modelo puro, sem Spring, JPA ou Jackson: `Sector`, `Spot`, `Coordinates`, `GarageConfiguration`. As invariantes ficam nos construtores. |
| `application.port.in` | Casos de uso que a aplicação oferece (`LoadGarageUseCase`). |
| `application.port.out` | O que a aplicação precisa de fora (`FetchGarageConfigurationPort`, `SaveGarageConfigurationPort`). Interfaces pequenas e específicas. |
| `application.service` | Implementação dos casos de uso, orquestrando domínio e ports de saída. |
| `adapter.in.startup` | `GarageStartupRunner`: carga inicial com novas tentativas. |
| `adapter.out.simulator` | Cliente HTTP do simulador e DTOs do `GET /garage`. |
| `adapter.out.persistence` | Entidades JPA, repositórios Spring Data e o adapter que grava a configuração. |
| `config` | Propriedades tipadas (`SimulatorProperties`). |

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
| MockK no lugar do Mockito | MockK é a biblioteca de mocks idiomática para Kotlin (classes finais, funções de extensão, DSL). O Mockito é excluído do `spring-boot-starter-test` para não haver duas bibliotecas de mock. | Mockito com `mockito-kotlin`. |

## Branches e commits

Cada bloco funcional tem sua própria branch, criada a partir da `main` atualizada. Cada branch é revisada e mergeada na `main` antes de começar a próxima.
