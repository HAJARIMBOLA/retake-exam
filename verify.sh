# 1. Upload: chekc le code retour + thumbnailKey est null au debut
http_code=$(curl -s -o /tmp/resp.json -w '%{http_code}' \
  -F "file=@test-image.png" -F "email=toky@mail.hei.school@example.com" "$BASE_URL/submissions")
[ "$http_code" = "201" ] || echo "ECHEC: attendu 201, reçu $http_code"
jq -e '.id and .email and (.createdAt) and (.thumbnailKey == null)' /tmp/resp.json \
  || echo "ECHEC: schéma de réponse non conforme au contrat"
id=$(jq -r .id /tmp/resp.json)

# 2. Attendre, puis vérifier que le traitement a bien eu lieu en asynchrone
sleep 5
list_code=$(curl -s -o /tmp/list.json -w '%{http_code}' "$BASE_URL/submissions")
[ "$list_code" = "200" ] || echo "ECHEC: GET /submissions a renvoyé $list_code"
key=$(jq -r ".[] | select(.id==\"$id\") | .thumbnailKey" /tmp/list.json)
[ "$key" != "null" ] && echo "OK : traité de façon différée"
