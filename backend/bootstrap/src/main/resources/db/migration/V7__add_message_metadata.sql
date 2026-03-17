-- ============================================================
-- V7 : Métadonnées IA sur les messages
-- ============================================================

ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS metadata_transcription   TEXT,
    ADD COLUMN IF NOT EXISTS metadata_translation     TEXT,
    ADD COLUMN IF NOT EXISTS metadata_target_language VARCHAR(10),
    ADD COLUMN IF NOT EXISTS metadata_intent          VARCHAR(50),
    ADD COLUMN IF NOT EXISTS metadata_emotion         VARCHAR(30);

-- Index partiel pour la recherche par intention (cas d'usage fréquent)
CREATE INDEX IF NOT EXISTS idx_messages_intent
    ON messages (conversation_id, metadata_intent)
    WHERE metadata_intent IS NOT NULL;
