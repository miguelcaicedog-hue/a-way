ALTER TABLE formatos_publicacion
    DROP CONSTRAINT formatos_publicacion_medidas_check;

ALTER TABLE formatos_publicacion
    ADD CONSTRAINT formatos_publicacion_medidas_check CHECK (
        logo_ancho > 0 AND logo_alto > 0 AND logo_x >= 0 AND logo_y >= 0
        AND titulo_contorno_grosor >= 0
        AND lugar_raya_ancho >= 0 AND lugar_raya_grosor >= 0
        AND fecha_ancho > 0 AND fecha_alto > 0 AND fecha_radio >= 0
        AND fecha_borde_grosor >= 0 AND fecha_icono_tamano > 0
    );

ALTER TABLE formatos_publicacion_tabla
    ADD COLUMN lugar_y integer,
    ADD COLUMN lugar_tamano integer;

UPDATE formatos_publicacion_tabla tabla
SET lugar_y = formato.lugar_y,
    lugar_tamano = formato.lugar_tamano
FROM formatos_publicacion formato
WHERE formato.id_formato_publicacion = tabla.id_formato_publicacion;

ALTER TABLE formatos_publicacion_tabla
    ALTER COLUMN lugar_y SET DEFAULT 158,
    ALTER COLUMN lugar_tamano SET DEFAULT 28,
    ALTER COLUMN lugar_y SET NOT NULL,
    ALTER COLUMN lugar_tamano SET NOT NULL;

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
        AND lugar_y >= 0 AND lugar_tamano > 0
    );

ALTER TABLE formatos_publicacion
    DROP COLUMN lugar_y,
    DROP COLUMN lugar_tamano;
