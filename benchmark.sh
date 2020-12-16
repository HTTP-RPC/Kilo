BASE_URL=http://localhost:8080/httprpc-test-1.0/employees
OUTPUT_FORMAT='%{time_starttransfer}, %{time_total}\n'

echo SQL
curl $BASE_URL/sql -s -o /dev/null -w "$OUTPUT_FORMAT"

echo SQL Jackson
curl $BASE_URL/sql-jackson -s -o /dev/null -w "$OUTPUT_FORMAT"

echo SQL stream
curl $BASE_URL/sql-stream -s -o /dev/null -w "$OUTPUT_FORMAT"

echo HQL
curl $BASE_URL/hql -s -o /dev/null -w "$OUTPUT_FORMAT"

echo HQL Jackson
curl $BASE_URL/hql-jackson -s -o /dev/null -w "$OUTPUT_FORMAT"

echo HQL stream
curl $BASE_URL/hql-stream -s -o /dev/null -w "$OUTPUT_FORMAT"

echo HQL stream/Jackson
curl $BASE_URL/hql-stream-jackson -s -o /dev/null -w "$OUTPUT_FORMAT"
