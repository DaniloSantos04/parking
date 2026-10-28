-- Uma linha por permanência de um veículo, do ENTRY ao EXIT.
-- Timestamps em UTC. Valores em DECIMAL, nunca em ponto flutuante.
CREATE TABLE parking_session
(
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    license_plate    VARCHAR(20)   NOT NULL,
    status           VARCHAR(10)   NOT NULL,
    entry_time       DATETIME(6)   NOT NULL,
    -- Multiplicador do preço dinâmico, travado no ENTRY pela lotação de antes da entrada.
    price_multiplier DECIMAL(4, 2) NOT NULL,
    -- Preenchidos no PARKED.
    spot_id          BIGINT        NULL,
    sector_code      VARCHAR(10)   NULL,
    base_price       DECIMAL(10, 2) NULL,
    -- Preenchidos no EXIT.
    exit_time        DATETIME(6)   NULL,
    amount           DECIMAL(10, 2) NULL,
    -- Placa enquanto a sessão está ativa; NULL depois do EXIT. O índice único sobre ela
    -- garante, no próprio banco, no máximo uma sessão ativa por placa.
    active_plate     VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN status <> 'EXITED' THEN license_plate END) STORED,
    CONSTRAINT pk_parking_session PRIMARY KEY (id),
    CONSTRAINT uk_parking_session_active_plate UNIQUE (active_plate),
    CONSTRAINT fk_parking_session_spot FOREIGN KEY (spot_id) REFERENCES spot (id),
    CONSTRAINT fk_parking_session_sector FOREIGN KEY (sector_code) REFERENCES sector (code),
    CONSTRAINT ck_parking_session_status CHECK (status IN ('ENTERED', 'PARKED', 'EXITED'))
);

-- Contagem de sessões ativas (lotação) a cada ENTRY.
CREATE INDEX ix_parking_session_status ON parking_session (status);

-- Faturamento: sessões encerradas por intervalo de saída, com ou sem filtro de setor.
CREATE INDEX ix_parking_session_revenue ON parking_session (status, exit_time, sector_code);

-- Eventos que não puderam ser aplicados (garagem cheia, placa duplicada, vaga inexistente...).
-- O webhook responde 200 mesmo nesses casos; esta tabela os torna auditáveis.
CREATE TABLE anomaly
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    type          VARCHAR(40)  NOT NULL,
    license_plate VARCHAR(20)  NULL,
    detail        VARCHAR(500) NOT NULL,
    occurred_at   DATETIME(6)  NOT NULL,
    CONSTRAINT pk_anomaly PRIMARY KEY (id)
);

CREATE INDEX ix_anomaly_type_occurred_at ON anomaly (type, occurred_at);
