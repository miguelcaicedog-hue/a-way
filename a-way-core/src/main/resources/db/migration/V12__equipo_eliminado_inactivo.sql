UPDATE team
SET estado = 'INACTIVO',
    fecha_modificacion = CURRENT_TIMESTAMP
WHERE estado = 'ELIMINADO';
