-- PP-92: Añadir campo categoria_turistica a itinerarios_actividades
-- Permite clasificar cada actividad del itinerario por su categoría de interés turístico
-- para habilitar la búsqueda de alternativas equivalentes.
-- El campo es nullable para compatibilidad con actividades existentes.

ALTER TABLE itinerarios_actividades ADD COLUMN categoria_turistica VARCHAR(30);
