ALTER TABLE formatos_publicacion_tabla
    ADD COLUMN fondo_publicacion varchar(80);

UPDATE formatos_publicacion_tabla AS tabla
SET fondo_publicacion = formato.fondo_publicacion
FROM formatos_publicacion AS formato
WHERE tabla.id_formato_publicacion = formato.id_formato_publicacion
    AND formato.fondo_publicacion IS NOT NULL;

ALTER TABLE formatos_publicacion
    DROP CONSTRAINT formatos_publicacion_fondo_publicacion_check;

ALTER TABLE formatos_publicacion
    DROP COLUMN fondo_publicacion;

ALTER TABLE formatos_publicacion_tabla
    ADD CONSTRAINT formatos_publicacion_tabla_fondo_publicacion_check CHECK (
        fondo_publicacion IS NULL
        OR (
            fondo_publicacion ~* '^[A-Za-z0-9][A-Za-z0-9._-]{0,70}\.(jpg|jpeg|png|webp)$'
            AND position('..' in fondo_publicacion) = 0
        )
    );
