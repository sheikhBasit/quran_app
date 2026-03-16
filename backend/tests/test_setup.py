"""Smoke test — verifies pytest is configured correctly."""


def test_environment():
    assert 1 + 1 == 2


def test_pytest_asyncio_available():
    import pytest_asyncio
    assert pytest_asyncio is not None


def test_httpx_available():
    import httpx
    assert httpx is not None
