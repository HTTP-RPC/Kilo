BASE_URL=http://localhost:8080/httprpc-test-1.0/employees
OUTPUT_FORMAT='%{time_starttransfer}, %{time_total}\n'

echo SQL
curl "$BASE_URL" -s -o /dev/null -w "$OUTPUT_FORMAT"

echo SQL Jackson
curl "$BASE_URL?jackson=true" -s -o /dev/null -w "$OUTPUT_FORMAT"

echo SQL stream
curl "$BASE_URL?stream=true" -s -o /dev/null -w "$OUTPUT_FORMAT"

echo SQL Jackson/stream
curl "$BASE_URL?jackson=true&stream=true" -s -o /dev/null -w "$OUTPUT_FORMAT"

echo HQL
curl "$BASE_URL?hql=true" -s -o /dev/null -w "$OUTPUT_FORMAT"

echo HQL Jackson
curl "$BASE_URL?hql=true&jackson=true" -s -o /dev/null -w "$OUTPUT_FORMAT"

echo HQL stream
curl "$BASE_URL?hql=true&stream=true" -s -o /dev/null -w "$OUTPUT_FORMAT"

echo HQL Jackson/stream
curl "$BASE_URL?hql=true&jackson=true&stream=true" -s -o /dev/null -w "$OUTPUT_FORMAT"
