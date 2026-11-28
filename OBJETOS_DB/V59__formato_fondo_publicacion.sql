ALTER TABLE formatos_publicacion
    ADD COLUMN fondo_publicacion varchar(80);

ALTER TABLE formatos_publicacion
    ADD CONSTRAINT formatos_publicacion_fondo_publicacion_check CHECK (
        fondo_publicacion IS NULL
        OR (
            fondo_publicacion ~* '^[A-Za-z0-9][A-Za-z0-9._-]{0,70}\.(jpg|jpeg|png|webp)$'
            AND position('..' in fondo_publicacion) = 0
        )
    );
