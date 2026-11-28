ALTER TABLE formatos_publicacion_tabla
    ADD COLUMN grupo_familia varchar(40) NOT NULL DEFAULT 'Arial',
    ADD COLUMN grupo_tamano integer NOT NULL DEFAULT 22,
    ADD COLUMN grupo_alto integer NOT NULL DEFAULT 64,
    ADD COLUMN grupo_fondo varchar(7) NOT NULL DEFAULT '#071833',
    ADD COLUMN grupo_color varchar(7) NOT NULL DEFAULT '#FFFFFF';

UPDATE formatos_publicacion_tabla
SET grupo_tamano = fila_tamano,
    grupo_alto = fila_alto,
    grupo_fondo = fila_fondo,
    grupo_color = fila_color;
