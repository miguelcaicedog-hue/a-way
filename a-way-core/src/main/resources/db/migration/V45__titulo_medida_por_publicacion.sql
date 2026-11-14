ALTER TABLE formatos_publicacion
    DROP CONSTRAINT formatos_publicacion_medidas_check;

ALTER TABLE formatos_publicacion
    ADD CONSTRAINT formatos_publicacion_medidas_check CHECK (
        logo_ancho > 0 AND logo_alto > 0 AND logo_x >= 0 AND logo_y >= 0
        AND titulo_contorno_grosor >= 0
        AND subtitulo_tamano > 0
        AND lugar_tamano > 0 AND lugar_raya_ancho >= 0 AND lugar_raya_grosor >= 0
        AND fecha_ancho > 0 AND fecha_alto > 0 AND fecha_radio >= 0
        AND fecha_borde_grosor >= 0 AND fecha_tamano > 0 AND fecha_icono_tamano > 0
    );

ALTER TABLE formatos_publicacion_tabla
    ADD COLUMN titulo_x integer,
    ADD COLUMN titulo_y integer,
    ADD COLUMN titulo_tamano integer;

UPDATE formatos_publicacion_tabla tabla
SET titulo_x = formato.titulo_x,
    titulo_y = formato.titulo_y,
    titulo_tamano = formato.titulo_tamano
FROM formatos_publicacion formato
WHERE formato.id_formato_publicacion = tabla.id_formato_publicacion;

ALTER TABLE formatos_publicacion_tabla
    ALTER COLUMN titulo_x SET DEFAULT 196,
    ALTER COLUMN titulo_y SET DEFAULT 18,
    ALTER COLUMN titulo_tamano SET DEFAULT 64,
    ALTER COLUMN titulo_x SET NOT NULL,
    ALTER COLUMN titulo_y SET NOT NULL,
    ALTER COLUMN titulo_tamano SET NOT NULL;

ALTER TABLE formatos_publicacion_tabla
    DROP CONSTRAINT formatos_publicacion_tabla_medidas_check;

ALTER TABLE formatos_publicacion_tabla
    ADD CONSTRAINT formatos_publicacion_tabla_medidas_check CHECK (
        tabla_ancho > 0 AND tabla_x >= 0 AND tabla_y >= 0
        AND borde_grosor >= 0 AND cabecera_alto > 0 AND cabecera_tamano > 0
        AND fila_alto > 0 AND fila_tamano > 0 AND numero_tamano > 0
        AND titulo_x >= 0 AND titulo_y >= 0 AND titulo_tamano > 0
    );

ALTER TABLE formatos_publicacion
    DROP COLUMN titulo_x,
    DROP COLUMN titulo_y,
    DROP COLUMN titulo_tamano;
