"""SQLAlchemy models for the RAG vector database tables."""
from pgvector.sqlalchemy import Vector
from sqlalchemy import Column, Integer, String, Text
from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    pass


class AyahEmbedding(Base):
    __tablename__ = "ayah_embeddings"

    id           = Column(Integer, primary_key=True, autoincrement=True)
    surah_number = Column(Integer, nullable=False)
    ayah_number  = Column(Integer, nullable=False)
    content      = Column(Text, nullable=False)
    embedding    = Column(Vector(384), nullable=False)


class HadithEmbedding(Base):
    __tablename__ = "hadith_embeddings"

    id            = Column(Integer, primary_key=True, autoincrement=True)
    collection    = Column(String(50), nullable=False)
    hadith_number = Column(Integer, nullable=False)
    content       = Column(Text, nullable=False)
    embedding     = Column(Vector(384), nullable=False)


class TafsirEmbedding(Base):
    __tablename__ = "tafsir_embeddings"

    id           = Column(Integer, primary_key=True, autoincrement=True)
    surah_number = Column(Integer, nullable=False)
    ayah_number  = Column(Integer, nullable=False)
    book_name    = Column(String(50), nullable=False)
    content      = Column(Text, nullable=False)
    embedding    = Column(Vector(384), nullable=False)
