-- Linha única que representa a garagem inteira.
-- Guarda a capacidade total (soma do max_capacity dos setores) e serve de alvo do
-- lock pessimista (SELECT ... FOR UPDATE) que serializa entradas, estacionamentos e saídas.
CREATE TABLE garage
(
    id       INT NOT NULL,
    capacity INT NOT NULL,
    CONSTRAINT pk_garage PRIMARY KEY (id),
    CONSTRAINT ck_garage_singleton CHECK (id = 1),
    CONSTRAINT ck_garage_capacity CHECK (capacity >= 0)
);

-- A linha existe desde a criação do schema, para o lock nunca encontrar a tabela vazia.
-- A capacidade real é gravada na carga da configuração do simulador.
INSERT INTO garage (id, capacity)
VALUES (1, 0);

CREATE TABLE sector
(
    code         VARCHAR(10)    NOT NULL,
    base_price   DECIMAL(10, 2) NOT NULL,
    max_capacity INT            NOT NULL,
    CONSTRAINT pk_sector PRIMARY KEY (code),
    CONSTRAINT ck_sector_base_price CHECK (base_price >= 0),
    CONSTRAINT ck_sector_max_capacity CHECK (max_capacity >= 0)
);

-- O id vem do simulador. As coordenadas usam escala fixa de 6 casas para que a busca
-- da vaga no evento PARKED seja por igualdade exata, sem erro de ponto flutuante.
CREATE TABLE spot
(
    id          BIGINT        NOT NULL,
    sector_code VARCHAR(10)   NOT NULL,
    lat         DECIMAL(9, 6) NOT NULL,
    lng         DECIMAL(9, 6) NOT NULL,
    occupied    BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_spot PRIMARY KEY (id),
    CONSTRAINT fk_spot_sector FOREIGN KEY (sector_code) REFERENCES sector (code),
    CONSTRAINT uk_spot_coordinates UNIQUE (lat, lng)
);
