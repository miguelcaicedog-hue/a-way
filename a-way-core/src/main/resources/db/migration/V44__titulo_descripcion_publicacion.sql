ALTER TABLE formatos_publicacion
    ADD COLUMN descripcion varchar(120) NOT NULL DEFAULT '';

ALTER TABLE formatos_publicacion_tabla
    ADD COLUMN titulo varchar(80) NOT NULL DEFAULT '';

UPDATE formatos_publicacion formato
SET descripcion = LEFT(liga.name, 120)
FROM league liga
WHERE liga.id = formato.id_liga
  AND formato.descripcion = '';

UPDATE formatos_publicacion_tabla
SET titulo = CASE tipo
    WHEN 'GOLEADORES' THEN 'GOLEADORES'
    WHEN 'RESULTADOS' THEN 'RESULTADOS'
    WHEN 'POSICIONES' THEN 'TABLA DE POSICIONES'
    WHEN 'HORARIOS' THEN 'HORARIOS'
    ELSE titulo
END
WHERE titulo = '';
