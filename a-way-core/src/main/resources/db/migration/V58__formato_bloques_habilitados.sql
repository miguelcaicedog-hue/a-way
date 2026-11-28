ALTER TABLE formatos_publicacion
    ADD COLUMN logo_habilitado boolean NOT NULL DEFAULT true,
    ADD COLUMN titulo_habilitado boolean NOT NULL DEFAULT true,
    ADD COLUMN subtitulo_habilitado boolean NOT NULL DEFAULT true,
    ADD COLUMN lugar_habilitado boolean NOT NULL DEFAULT true,
    ADD COLUMN fecha_habilitado boolean NOT NULL DEFAULT true;
