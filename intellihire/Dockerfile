# Production Dockerfile for IntelliHire AI
FROM python:3.10-slim

# Prevent Python from writing .pyc and buffer stdout/stderr
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV PORT=5000

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Install python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application source code
COPY . /app

# Ensure uploads and data directories exist
RUN mkdir -p /app/uploads /app/data

EXPOSE 5000

# Render sets $PORT dynamically; fall back to 5000 locally.
# Using sh -c so the shell expands $PORT at runtime.
CMD ["sh", "-c", "python app.py"]
