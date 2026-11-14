ALTER TABLE formatos_publicacion_tabla
    DROP CONSTRAINT formatos_publicacion_tabla_tipo_check;

ALTER TABLE formatos_publicacion_tabla
    ADD CONSTRAINT formatos_publicacion_tabla_tipo_check
    CHECK (tipo IN ('GOLEADORES', 'RESULTADOS', 'POSICIONES', 'HORARIOS'));

INSERT INTO formatos_publicacion_tabla (
    id_formato_publicacion_tabla, id_formato_publicacion, id_liga, tipo, columnas_tabla,
    usuario_creacion, usuario_modificacion
)
SELECT gen_random_uuid(), formato.id_formato_publicacion, formato.id_liga, 'HORARIOS', '',
       'sistema', 'sistema'
FROM formatos_publicacion formato
WHERE formato.estado <> 'ELIMINADO'
  AND NOT EXISTS (
      SELECT 1
      FROM formatos_publicacion_tabla tabla
      WHERE tabla.id_liga = formato.id_liga
        AND tabla.tipo = 'HORARIOS'
        AND tabla.estado <> 'ELIMINADO'
  );
