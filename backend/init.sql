-- Runs automatically on first postgres container start
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ayah_embeddings (
    id           SERIAL PRIMARY KEY,
    surah_number INTEGER NOT NULL,
    ayah_number  INTEGER NOT NULL,
    content      TEXT NOT NULL,
    embedding    vector(384) NOT NULL
);

CREATE TABLE IF NOT EXISTS hadith_embeddings (
    id            SERIAL PRIMARY KEY,
    collection    TEXT NOT NULL,
    hadith_number INTEGER NOT NULL,
    content       TEXT NOT NULL,
    embedding     vector(384) NOT NULL
);

CREATE TABLE IF NOT EXISTS tafsir_embeddings (
    id           SERIAL PRIMARY KEY,
    surah_number INTEGER NOT NULL,
    ayah_number  INTEGER NOT NULL,
    book_name    TEXT NOT NULL,
    content      TEXT NOT NULL,
    embedding    vector(384) NOT NULL
);

CREATE INDEX IF NOT EXISTS ayah_emb_idx    ON ayah_embeddings    USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS hadith_emb_idx  ON hadith_embeddings  USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS tafsir_emb_idx  ON tafsir_embeddings  USING hnsw (embedding vector_cosine_ops);
