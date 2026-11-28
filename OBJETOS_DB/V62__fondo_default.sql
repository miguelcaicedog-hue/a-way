ALTER TABLE league
    ALTER COLUMN imagen_plantilla SET DEFAULT 'fondo-default.jpg';

UPDATE league
SET imagen_plantilla = 'fondo-default.jpg'
WHERE imagen_plantilla IN ('fondo-estadio.jpg', 'lf6cc/fondo-estadio.jpg');

UPDATE formatos_publicacion_tabla
SET fondo_publicacion = 'fondo-default.jpg'
WHERE fondo_publicacion IN ('fondo-estadio.jpg', 'lf6cc/fondo-estadio.jpg');
