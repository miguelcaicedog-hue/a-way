ALTER TABLE league
    DROP CONSTRAINT league_imagen_plantilla_check;

ALTER TABLE league
    ADD CONSTRAINT league_imagen_plantilla_check CHECK (
        imagen_plantilla ~* '^(?:[A-Za-z0-9][A-Za-z0-9._-]{0,30}/)?[A-Za-z0-9][A-Za-z0-9._-]{0,40}\.(jpg|jpeg|png|webp)$'
        AND position('..' in imagen_plantilla) = 0
    );

ALTER TABLE league
    ALTER COLUMN imagen_plantilla SET DEFAULT 'lf6cc/fondo-estadio.jpg';

UPDATE league
SET imagen_plantilla = 'lf6cc/fondo-estadio.jpg'
WHERE imagen_plantilla = 'fondo-estadio.jpg';

ALTER TABLE formatos_publicacion_tabla
    DROP CONSTRAINT formatos_publicacion_tabla_fondo_publicacion_check;

ALTER TABLE formatos_publicacion_tabla
    ADD CONSTRAINT formatos_publicacion_tabla_fondo_publicacion_check CHECK (
        fondo_publicacion IS NULL
        OR (
            fondo_publicacion ~* '^(?:[A-Za-z0-9][A-Za-z0-9._-]{0,30}/)?[A-Za-z0-9][A-Za-z0-9._-]{0,40}\.(jpg|jpeg|png|webp)$'
            AND position('..' in fondo_publicacion) = 0
        )
    );

UPDATE formatos_publicacion_tabla
SET fondo_publicacion = 'lf6cc/fondo-estadio.jpg'
WHERE fondo_publicacion = 'fondo-estadio.jpg';
