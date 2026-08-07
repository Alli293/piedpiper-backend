CREATE TABLE slug_historico (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresas(id),
    slug_anterior VARCHAR(120) NOT NULL UNIQUE,
    fecha_cambio TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_slug_historico_slug ON slug_historico(slug_anterior);
