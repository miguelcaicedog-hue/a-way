ALTER TABLE league
    ADD COLUMN imagen_plantilla varchar(80) NOT NULL DEFAULT 'fondo-estadio.jpg';

ALTER TABLE league
    ADD CONSTRAINT league_imagen_plantilla_check CHECK (
        imagen_plantilla ~* '^[A-Za-z0-9][A-Za-z0-9._-]{0,70}\.(jpg|jpeg|png|webp)$'
        AND position('..' in imagen_plantilla) = 0
    );

CREATE TABLE formatos_publicacion (
    id_formato_publicacion uuid NOT NULL,
    id_liga uuid NOT NULL REFERENCES league (id),
    nombre_lugar varchar(80) NOT NULL DEFAULT '',
    logo_x integer NOT NULL DEFAULT 22,
    logo_y integer NOT NULL DEFAULT 12,
    logo_ancho integer NOT NULL DEFAULT 160,
    logo_alto integer NOT NULL DEFAULT 180,
    titulo_x integer NOT NULL DEFAULT 196,
    titulo_y integer NOT NULL DEFAULT 18,
    titulo_tamano integer NOT NULL DEFAULT 64,
    titulo_negrita boolean NOT NULL DEFAULT true,
    titulo_cursiva boolean NOT NULL DEFAULT true,
    titulo_familia varchar(40) NOT NULL DEFAULT 'Arial Black',
    titulo_color_inicio varchar(7) NOT NULL DEFAULT '#FFFFFF',
    titulo_color_fin varchar(7) NOT NULL DEFAULT '#C5D0DC',
    titulo_contorno_color varchar(7) NOT NULL DEFAULT '#061428',
    titulo_contorno_grosor integer NOT NULL DEFAULT 3,
    titulo_sombra boolean NOT NULL DEFAULT true,
    subtitulo_x integer NOT NULL DEFAULT 196,
    subtitulo_y integer NOT NULL DEFAULT 96,
    subtitulo_tamano integer NOT NULL DEFAULT 36,
    subtitulo_negrita boolean NOT NULL DEFAULT true,
    subtitulo_cursiva boolean NOT NULL DEFAULT true,
    subtitulo_familia varchar(40) NOT NULL DEFAULT 'Arial',
    subtitulo_color varchar(7) NOT NULL DEFAULT '#1F5EFF',
    lugar_y integer NOT NULL DEFAULT 158,
    lugar_tamano integer NOT NULL DEFAULT 28,
    lugar_cursiva boolean NOT NULL DEFAULT true,
    lugar_familia varchar(40) NOT NULL DEFAULT 'Arial',
    lugar_color varchar(7) NOT NULL DEFAULT '#FFFFFF',
    lugar_raya_ancho integer NOT NULL DEFAULT 90,
    lugar_raya_grosor integer NOT NULL DEFAULT 2,
    fecha_x integer NOT NULL DEFAULT 300,
    fecha_y integer NOT NULL DEFAULT 206,
    fecha_ancho integer NOT NULL DEFAULT 424,
    fecha_alto integer NOT NULL DEFAULT 48,
    fecha_radio integer NOT NULL DEFAULT 24,
    fecha_borde_grosor integer NOT NULL DEFAULT 2,
    fecha_borde_color varchar(7) NOT NULL DEFAULT '#FFFFFF',
    fecha_tamano integer NOT NULL DEFAULT 20,
    fecha_negrita boolean NOT NULL DEFAULT true,
    fecha_familia varchar(40) NOT NULL DEFAULT 'Arial',
    fecha_color varchar(7) NOT NULL DEFAULT '#FFFFFF',
    fecha_icono_tamano integer NOT NULL DEFAULT 26,
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT formatos_publicacion_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT formatos_publicacion_medidas_check CHECK (
        logo_ancho > 0 AND logo_alto > 0 AND logo_x >= 0 AND logo_y >= 0
        AND titulo_tamano > 0 AND titulo_contorno_grosor >= 0
        AND subtitulo_tamano > 0
        AND lugar_tamano > 0 AND lugar_raya_ancho >= 0 AND lugar_raya_grosor >= 0
        AND fecha_ancho > 0 AND fecha_alto > 0 AND fecha_radio >= 0
        AND fecha_borde_grosor >= 0 AND fecha_tamano > 0 AND fecha_icono_tamano > 0
    )
) TABLESPACE ts_away;

CREATE UNIQUE INDEX formatos_publicacion_pkey
    ON formatos_publicacion (id_formato_publicacion) TABLESPACE ts_away;
ALTER TABLE formatos_publicacion
    ADD CONSTRAINT formatos_publicacion_pkey PRIMARY KEY USING INDEX formatos_publicacion_pkey;

CREATE UNIQUE INDEX formatos_publicacion_liga_key
    ON formatos_publicacion (id_liga) TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

CREATE TABLE formatos_publicacion_tabla (
    id_formato_publicacion_tabla uuid NOT NULL,
    id_formato_publicacion uuid NOT NULL REFERENCES formatos_publicacion (id_formato_publicacion),
    id_liga uuid NOT NULL REFERENCES league (id),
    tipo varchar(20) NOT NULL,
    tabla_x integer NOT NULL DEFAULT 28,
    tabla_y integer NOT NULL DEFAULT 276,
    tabla_ancho integer NOT NULL DEFAULT 968,
    borde_grosor integer NOT NULL DEFAULT 2,
    borde_color varchar(7) NOT NULL DEFAULT '#FFFFFF',
    cabecera_alto integer NOT NULL DEFAULT 44,
    cabecera_fondo varchar(7) NOT NULL DEFAULT '#1A4FB8',
    cabecera_color varchar(7) NOT NULL DEFAULT '#FFFFFF',
    cabecera_tamano integer NOT NULL DEFAULT 18,
    fila_alto integer NOT NULL DEFAULT 64,
    fila_fondo varchar(7) NOT NULL DEFAULT '#071833',
    fila_color varchar(7) NOT NULL DEFAULT '#FFFFFF',
    fila_tamano integer NOT NULL DEFAULT 22,
    numero_color varchar(7) NOT NULL DEFAULT '#2F6BFF',
    numero_tamano integer NOT NULL DEFAULT 32,
    destacada_fondo varchar(7) NOT NULL DEFAULT '#1A56C9',
    columnas_tabla varchar(120) NOT NULL DEFAULT '',
    fecha_creacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_creacion varchar(80) NOT NULL,
    usuario_modificacion varchar(80) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT formatos_publicacion_tabla_estado_check CHECK (estado IN ('ACTIVO', 'INACTIVO', 'ELIMINADO')),
    CONSTRAINT formatos_publicacion_tabla_tipo_check CHECK (tipo IN ('GOLEADORES', 'RESULTADOS', 'POSICIONES')),
    CONSTRAINT formatos_publicacion_tabla_medidas_check CHECK (
        tabla_ancho > 0 AND tabla_x >= 0 AND tabla_y >= 0
        AND borde_grosor >= 0 AND cabecera_alto > 0 AND cabecera_tamano > 0
        AND fila_alto > 0 AND fila_tamano > 0 AND numero_tamano > 0
    )
) TABLESPACE ts_away;

CREATE UNIQUE INDEX formatos_publicacion_tabla_pkey
    ON formatos_publicacion_tabla (id_formato_publicacion_tabla) TABLESPACE ts_away;
ALTER TABLE formatos_publicacion_tabla
    ADD CONSTRAINT formatos_publicacion_tabla_pkey PRIMARY KEY USING INDEX formatos_publicacion_tabla_pkey;

CREATE UNIQUE INDEX formatos_publicacion_tabla_tipo_key
    ON formatos_publicacion_tabla (id_liga, tipo) TABLESPACE ts_away
    WHERE estado <> 'ELIMINADO';

CREATE INDEX formatos_publicacion_tabla_formato_idx
    ON formatos_publicacion_tabla (id_formato_publicacion) TABLESPACE ts_away;

WITH nuevos AS (
    INSERT INTO formatos_publicacion (id_formato_publicacion, id_liga, usuario_creacion, usuario_modificacion)
    SELECT gen_random_uuid(), id, 'sistema', 'sistema'
    FROM league
    RETURNING id_formato_publicacion, id_liga
)
INSERT INTO formatos_publicacion_tabla (
    id_formato_publicacion_tabla, id_formato_publicacion, id_liga, tipo, columnas_tabla,
    usuario_creacion, usuario_modificacion
)
SELECT gen_random_uuid(), nuevos.id_formato_publicacion, nuevos.id_liga, tipos.tipo, tipos.columnas_tabla,
       'sistema', 'sistema'
FROM nuevos
CROSS JOIN (VALUES
    ('GOLEADORES', ''),
    ('RESULTADOS', ''),
    ('POSICIONES', 'POSICION,EQUIPO,PJ,PTS,DIF')
) AS tipos(tipo, columnas_tabla);
