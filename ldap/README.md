### Команда импорта из файла:
```
docker compose exec -T openldap ldapadd -x -c -D "cn=admin,dc=bionic,dc=com" -w "1234" -f /import/config.ldif
```

### Экспорт настроенного keycloak

```
docker compose exec keycloak /opt/keycloak/bin/kc.sh export --realm reports-realm --file /tmp/realm-export.json
docker compose cp keycloak:/tmp/realm-export.json ./keycloak/realm-export.json
```
