ALTER TABLE category ADD COLUMN cantidad_grupos integer NOT NULL DEFAULT 1;
ALTER TABLE category ADD COLUMN equipos_por_grupo integer;

ALTER TABLE category ADD CONSTRAINT category_cantidad_grupos_check CHECK (cantidad_grupos > 0);
ALTER TABLE category ADD CONSTRAINT category_equipos_por_grupo_check CHECK (equipos_por_grupo IS NULL OR equipos_por_grupo > 0);
