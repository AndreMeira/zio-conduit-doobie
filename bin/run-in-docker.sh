docker run \
  --network zio-conduit-doobie_dev \
  -p 8080:8080 \
  -e OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318 \
  -e JDBC_URL="jdbc:postgresql://database:5432/conduit" \
  com.conduit/1.0 "$@" -main Main