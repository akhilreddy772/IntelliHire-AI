#!/usr/bin/env bash
# ── IntelliHire AI — Java Desktop App Launcher (Linux / macOS) ─────────────
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"
echo "Starting IntelliHire AI Java Desktop App…"
echo "Login credentials:  srm / ap   OR   recruiter / recruiter123"
java -cp ".:sqlite-jdbc-3.49.1.0.jar" JavaApp
