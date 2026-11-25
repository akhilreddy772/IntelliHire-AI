"""IntelliHire AI — Universal SQL Database Adapter.

Priority order:
  1. MySQL   — if MYSQL_HOST (or MYSQL_URL) is set and pymysql is installed
  2. PostgreSQL — if PGHOST (or DATABASE_URL) is set and psycopg2 is installed
  3. SQLite  — always-available fallback (data/intellihire.db)

All public functions accept and return plain Python dicts / values so callers
are completely DB-agnostic.
"""
import json
import os
import sqlite3
import uuid
from datetime import datetime
from werkzeug.security import generate_password_hash

# ---------------------------------------------------------------------------
# Optional driver imports
# ---------------------------------------------------------------------------
HAS_PSYCOPG2 = False
try:
    import psycopg2
    import psycopg2.extras
    HAS_PSYCOPG2 = True
except ImportError:
    pass

HAS_PYMYSQL = False
try:
    import pymysql
    import pymysql.cursors
    HAS_PYMYSQL = True
except ImportError:
    pass


# ---------------------------------------------------------------------------
# Thin DB wrapper — unified placeholder & row-dict interface
# ---------------------------------------------------------------------------
class UniversalDB:
    """Provides a consistent dict/row interface over PostgreSQL, MySQL and SQLite."""

    def __init__(self, conn, dialect: str = "sqlite"):
        """
        dialect: "sqlite" | "postgres" | "mysql"
        """
        self.conn = conn
        self.dialect = dialect          # for public inspection
        self.is_pg = dialect == "postgres"
        self.is_mysql = dialect == "mysql"
        self._use_percent = dialect in ("postgres", "mysql")  # True → translate ? → %s

    # ------------------------------------------------------------------
    def execute(self, query, params=None):
        if params is None:
            params = ()
        cur = self.conn.cursor()
        if self._use_percent:
            query = query.replace("?", "%s")
        cur.execute(query, params)
        return cur

    def commit(self):
        self.conn.commit()

    def close(self):
        try:
            self.conn.close()
        except Exception:
            pass


# ---------------------------------------------------------------------------
# Connection factory
# ---------------------------------------------------------------------------
def get_db_connection(data_dir: str) -> UniversalDB:
    """Return a connected UniversalDB, trying MySQL → PostgreSQL → SQLite."""

    # ── 1. MySQL ──────────────────────────────────────────────────────────
    mysql_url  = os.environ.get("MYSQL_URL", "").strip()
    mysql_host = os.environ.get("MYSQL_HOST", "").strip()

    if HAS_PYMYSQL and (mysql_url or mysql_host):
        try:
            if mysql_url:
                # e.g. mysql+pymysql://user:pass@host:3306/dbname
                # Strip optional scheme prefix so pymysql.connect() can parse it
                url = mysql_url.replace("mysql+pymysql://", "").replace("mysql://", "")
                # url now looks like  user:pass@host:port/dbname
                userpass, hostdb = url.split("@", 1)
                user, password   = (userpass.split(":", 1) + [""])[:2]
                hostport, dbname = (hostdb.split("/", 1) + ["intellihire"])[:2]
                host, port       = (hostport.split(":", 1) + ["3306"])[:2]
                port = int(port)
            else:
                host     = mysql_host
                port     = int(os.environ.get("MYSQL_PORT", "3306"))
                user     = os.environ.get("MYSQL_USER", "root")
                password = os.environ.get("MYSQL_PASSWORD", "")
                dbname   = os.environ.get("MYSQL_DATABASE", "intellihire")

            mysql_conn = pymysql.connect(
                host=host,
                port=port,
                user=user,
                password=password,
                database=dbname,
                connect_timeout=3,
                autocommit=False,
                cursorclass=pymysql.cursors.DictCursor,
                charset="utf8mb4",
            )
            wrapped = UniversalDB(mysql_conn, dialect="mysql")
            init_schema(wrapped)
            print(f"[IntelliHire DB] Connected to MySQL at {host}:{port}/{dbname}")
            return wrapped
        except Exception as e:
            print(f"[IntelliHire DB] MySQL connection failed ({e}). Trying PostgreSQL…")

    # ── 2. PostgreSQL ─────────────────────────────────────────────────────
    db_url    = os.environ.get("DATABASE_URL", "").strip()
    pg_host   = os.environ.get("PGHOST", "").strip()
    pg_user   = os.environ.get("PGUSER", "").strip()
    pg_pass   = os.environ.get("PGPASSWORD", "").strip()
    pg_db     = os.environ.get("PGDATABASE", "intellihire").strip()
    pg_port   = os.environ.get("PGPORT", "5432").strip()

    if HAS_PSYCOPG2 and (db_url or pg_host):
        try:
            if db_url:
                pg_conn = psycopg2.connect(db_url, cursor_factory=psycopg2.extras.RealDictCursor)
            else:
                pg_conn = psycopg2.connect(
                    host=pg_host, user=pg_user, password=pg_pass,
                    dbname=pg_db, port=pg_port, connect_timeout=3,
                    cursor_factory=psycopg2.extras.RealDictCursor,
                )
            pg_conn.autocommit = False
            wrapped = UniversalDB(pg_conn, dialect="postgres")
            init_schema(wrapped)
            print(f"[IntelliHire DB] Connected to PostgreSQL at {pg_host or 'URL'}/{pg_db}")
            return wrapped
        except Exception as e:
            print(f"[IntelliHire DB] PostgreSQL connection failed ({e}). Falling back to SQLite.")

    # ── 3. SQLite fallback ────────────────────────────────────────────────
    sqlite_path = os.path.join(data_dir, "intellihire.db")
    s_conn = sqlite3.connect(sqlite_path)
    s_conn.row_factory = sqlite3.Row
    wrapped = UniversalDB(s_conn, dialect="sqlite")
    init_schema(wrapped)
    print(f"[IntelliHire DB] Using SQLite at {sqlite_path}")
    return wrapped


# ---------------------------------------------------------------------------
# Schema initialisation
# ---------------------------------------------------------------------------
_TABLES_SQLITE = {
    "users": """
        CREATE TABLE IF NOT EXISTS users (
            id   TEXT PRIMARY KEY,
            username TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            role TEXT NOT NULL,
            created TEXT NOT NULL
        )
    """,
    "candidates": """
        CREATE TABLE IF NOT EXISTS candidates (
            id       TEXT PRIMARY KEY,
            name     TEXT NOT NULL,
            email    TEXT,
            filename TEXT,
            score    INTEGER,
            years    INTEGER,
            top_job  TEXT,
            top_match INTEGER,
            data     TEXT NOT NULL,
            created  TEXT NOT NULL
        )
    """,
    "custom_jobs": """
        CREATE TABLE IF NOT EXISTS custom_jobs (
            id          TEXT PRIMARY KEY,
            title       TEXT NOT NULL,
            company     TEXT NOT NULL,
            location    TEXT NOT NULL,
            type        TEXT NOT NULL,
            salary      TEXT NOT NULL,
            description TEXT NOT NULL,
            skills      TEXT NOT NULL,
            created     TEXT NOT NULL
        )
    """,
}

# MySQL & PostgreSQL share the same DDL (VARCHAR, INT, TEXT are standard SQL)
_TABLES_SQL = {
    "users": """
        CREATE TABLE IF NOT EXISTS users (
            id            VARCHAR(64)  PRIMARY KEY,
            username      VARCHAR(100) UNIQUE NOT NULL,
            password_hash TEXT         NOT NULL,
            role          VARCHAR(50)  NOT NULL,
            created       VARCHAR(50)  NOT NULL
        )
    """,
    "candidates": """
        CREATE TABLE IF NOT EXISTS candidates (
            id        VARCHAR(64)  PRIMARY KEY,
            name      VARCHAR(255) NOT NULL,
            email     VARCHAR(255),
            filename  VARCHAR(255),
            score     INT,
            years     INT,
            top_job   VARCHAR(255),
            top_match INT,
            data      TEXT         NOT NULL,
            created   VARCHAR(50)  NOT NULL
        )
    """,
    "custom_jobs": """
        CREATE TABLE IF NOT EXISTS custom_jobs (
            id          VARCHAR(64)  PRIMARY KEY,
            title       VARCHAR(255) NOT NULL,
            company     VARCHAR(255) NOT NULL,
            location    VARCHAR(255) NOT NULL,
            type        VARCHAR(50)  NOT NULL,
            salary      VARCHAR(100) NOT NULL,
            description TEXT         NOT NULL,
            skills      TEXT         NOT NULL,
            created     VARCHAR(50)  NOT NULL
        )
    """,
}


def init_schema(db: UniversalDB):
    """Create tables (if they do not exist) and seed default users."""
    tables = _TABLES_SQLITE if db.dialect == "sqlite" else _TABLES_SQL
    for ddl in tables.values():
        db.execute(ddl)
    db.commit()
    seed_users(db)


def seed_users(db: UniversalDB):
    """Insert the two demo accounts on a brand-new installation."""
    cur = db.execute("SELECT COUNT(*) AS cnt FROM users")
    res = cur.fetchone()
    count = res["cnt"] if isinstance(res, dict) else res[0]
    if count == 0:
        now = datetime.now().strftime("%Y-%m-%d %H:%M")
        for username, raw_password, role in [
            ("recruiter", "recruiter123", "recruiter"),
            ("candidate", "candidate123", "candidate"),
        ]:
            db.execute(
                "INSERT INTO users VALUES (?, ?, ?, ?, ?)",
                (uuid.uuid4().hex[:8], username,
                 generate_password_hash(raw_password), role, now),
            )
        db.commit()
