-- V2: Remover a tabela maquinas e a relação em modelos

-- Drop index associated with maquina_id in modelos
DROP INDEX IF EXISTS idx_modelos_maquina_id;

-- Drop the maquina_id column (which also drops the constraint referencing maquinas)
ALTER TABLE modelos DROP COLUMN IF EXISTS maquina_id;

-- Drop the maquinas table
DROP TABLE IF EXISTS maquinas;

-- Add the new string field 'maquina' to modelos
ALTER TABLE modelos ADD COLUMN maquina VARCHAR(255) NOT NULL DEFAULT 'MANUAL';
