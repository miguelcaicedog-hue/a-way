ALTER TABLE reglas_suspension
    ADD COLUMN amarillas_momento varchar(30) NOT NULL DEFAULT 'NEXT_MATCHDAY',
    ADD COLUMN rojas_momento varchar(30) NOT NULL DEFAULT 'NEXT_MATCHDAY',
    ADD CONSTRAINT reglas_suspension_amarillas_momento_check
        CHECK (amarillas_momento IN ('NEXT_MATCHDAY', 'FIRST_MATCHDAY_OF_DAY')),
    ADD CONSTRAINT reglas_suspension_rojas_momento_check
        CHECK (rojas_momento IN ('NEXT_MATCHDAY', 'FIRST_MATCHDAY_OF_DAY'));
