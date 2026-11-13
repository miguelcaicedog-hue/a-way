WITH duplicados AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY league_id, upper(name)
               ORDER BY CASE estado WHEN 'ACTIVO' THEN 0 WHEN 'INACTIVO' THEN 1 ELSE 2 END,
                        fecha_creacion,
                        id
           ) AS n
    FROM team
)
UPDATE team AS equipo
SET name = upper(equipo.name) || CASE WHEN duplicados.n = 1 THEN '' ELSE ' ' || duplicados.n::text END
FROM duplicados
WHERE equipo.id = duplicados.id
  AND equipo.name IS DISTINCT FROM upper(equipo.name) || CASE WHEN duplicados.n = 1 THEN '' ELSE ' ' || duplicados.n::text END;

UPDATE player
SET name = upper(name)
WHERE name IS DISTINCT FROM upper(name);

UPDATE player
SET alias = upper(alias)
WHERE alias IS DISTINCT FROM upper(alias);

UPDATE inscripcion_equipo
SET nombre_equipo = upper(nombre_equipo)
WHERE nombre_equipo IS DISTINCT FROM upper(nombre_equipo);

UPDATE inscripcion_jugador
SET nombre = upper(nombre)
WHERE nombre IS DISTINCT FROM upper(nombre);

UPDATE inscripcion_jugador
SET alias = upper(alias)
WHERE alias IS DISTINCT FROM upper(alias);
