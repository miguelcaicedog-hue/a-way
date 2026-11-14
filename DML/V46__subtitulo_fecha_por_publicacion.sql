ALTER TABLE formatos_publicacion
    DROP CONSTRAINT formatos_publicacion_medidas_check;

ALTER TABLE formatos_publicacion
    ADD CONSTRAINT formatos_publicacion_medidas_check CHECK (
        logo_ancho > 0 AND logo_alto > 0 AND logo_x >= 0 AND logo_y >= 0
        AND titulo_contorno_grosor >= 0
        AND lugar_tamano > 0 AND lugar_raya_ancho >= 0 AND lugar_raya_grosor >= 0
        AND fecha_ancho > 0 AND fecha_alto > 0 AND fecha_radio >= 0
        AND fecha_borde_grosor >= 0 AND fecha_icono_tamano > 0
    );

ALTER TABLE formatos_publicacion_tabla
    ADD COLUMN subtitulo_x integer,
    ADD COLUMN subtitulo_y integer,
    ADD COLUMN subtitulo_tamano integer,
    ADD COLUMN fecha_x integer,
    ADD COLUMN fecha_y integer,
    ADD COLUMN fecha_tamano integer;

UPDATE formatos_publicacion_tabla tabla
SET subtitulo_x = formato.subtitulo_x,
    subtitulo_y = formato.subtitulo_y,
    subtitulo_tamano = formato.subtitulo_tamano,
    fecha_x = formato.fecha_x,
    fecha_y = formato.fecha_y,
    fecha_tamano = formato.fecha_tamano
FROM formatos_publicacion formato
WHERE formato.id_formato_publicacion = tabla.id_formato_publicacion;

ALTER TABLE formatos_publicacion_tabla
    ALTER COLUMN subtitulo_x SET DEFAULT 196,
    ALTER COLUMN subtitulo_y SET DEFAULT 96,
    ALTER COLUMN subtitulo_tamano SET DEFAULT 36,
    ALTER COLUMN fecha_x SET DEFAULT 300,
    ALTER COLUMN fecha_y SET DEFAULT 206,
    ALTER COLUMN fecha_tamano SET DEFAULT 20,
    ALTER COLUMN subtitulo_x SET NOT NULL,
    ALTER COLUMN subtitulo_y SET NOT NULL,
    ALTER COLUMN subtitulo_tamano SET NOT NULL,
    ALTER COLUMN fecha_x SET NOT NULL,
    ALTER COLUMN fecha_y SET NOT NULL,
    ALTER COLUMN fecha_tamano SET NOT NULL;

ALTER TABLE formatos_publicacion_tabla
    DROP CONSTRAINT formatos_publicacion_tabla_medidas_check;

ALTER TABLE formatos_publicacion_tabla
    ADD CONSTRAINT formatos_publicacion_tabla_medidas_check CHECK (
        tabla_ancho > 0 AND tabla_x >= 0 AND tabla_y >= 0
        AND borde_grosor >= 0 AND cabecera_alto > 0 AND cabecera_tamano > 0
        AND fila_alto > 0 AND fila_tamano > 0 AND numero_tamano > 0
        AND titulo_x >= 0 AND titulo_y >= 0 AND titulo_tamano > 0
        AND subtitulo_x >= 0 AND subtitulo_y >= 0 AND subtitulo_tamano > 0
        AND fecha_x >= 0 AND fecha_y >= 0 AND fecha_tamano > 0
    );

ALTER TABLE formatos_publicacion
    DROP COLUMN subtitulo_x,
    DROP COLUMN subtitulo_y,
    DROP COLUMN subtitulo_tamano,
    DROP COLUMN fecha_x,
    DROP COLUMN fecha_y,
    DROP COLUMN fecha_tamano;
