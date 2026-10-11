-- V10: Versao da credencial do usuario. Os tokens emitidos carregam este numero; trocar ou
-- redefinir a senha soma 1 e invalida os tokens anteriores. Tokens emitidos antes desta
-- migracao nao tem o numero e valem como versao 0.
ALTER TABLE usuarios ADD COLUMN versao_credencial INTEGER NOT NULL DEFAULT 0;
