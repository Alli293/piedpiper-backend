CREATE TABLE slugs_historicos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL REFERENCES empresas(id),
    slug_anterior VARCHAR(120) NOT NULL UNIQUE,
    fecha_cambio TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_slugs_historicos_slug ON slugs_historicos(slug_anterior);
