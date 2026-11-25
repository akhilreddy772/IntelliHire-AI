# ── IntelliHire AI — Root Dockerfile (for Render.com deployment) ─────────────
# Render clones the repo and looks for Dockerfile at the root by default.
# The Flask app lives in the intellihire/ subdirectory.

FROM python:3.10-slim

# Prevent .pyc files and enable stdout/stderr logging
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

# Install system build deps
RUN apt-get update && apt-get install -y --no-install-recommends gcc \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies from intellihire/requirements.txt
COPY intellihire/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the entire Flask app into /app
COPY intellihire/ /app/

# Ensure runtime directories exist
RUN mkdir -p /app/uploads /app/data

# Render sets $PORT at runtime; default to 5000 locally
EXPOSE 5000

CMD ["python", "app.py"]
