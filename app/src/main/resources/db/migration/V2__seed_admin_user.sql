-- V2: Seed usuario administrador padrao
-- Senha: admin123 (BCrypt hash)
-- IMPORTANTE: Troque a senha apos o primeiro login em producao

INSERT INTO usuarios (id, nome, email, senha_hash, perfil, ativo, criado_em, atualizado_em)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Administrador',
    'admin@rgm.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMINISTRADOR',
    true,
    now(),
    now()
)
ON CONFLICT (email) DO NOTHING;
