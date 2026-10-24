FROM python:3.11-slim

WORKDIR /app

ENV PYTHONUNBUFFERED=1

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# Cloud Run injects PORT environment variable (default 8080)
ENV PORT=8080
EXPOSE 8080

# Use gunicorn to serve HTTP requests on $PORT (timeout 0 for data calculations)
CMD exec gunicorn --bind :$PORT --workers 1 --threads 8 --timeout 0 server:app
