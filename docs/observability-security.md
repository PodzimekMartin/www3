# Observabilita a bezpecnost

## Monitoring

Aplikace vystavuje:

- `/actuator/health`
- `/actuator/health/readiness`
- `/actuator/health/liveness`
- `/actuator/prometheus`
- `/actuator/metrics`

Kubernetes anotace v Deploymentu umoznuji scrapovani Prometheem.

## Metriky

Spring Boot Actuator a Micrometer poskytuje hlavne:

- pocty HTTP requestu
- latence HTTP requestu
- JVM memory a GC metriky
- stav datasource

## Alert

Priklad alertu pro Prometheus:

```yaml
groups:
  - name: course-reservations
    rules:
      - alert: CourseReservationsHighErrorRate
        expr: sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: High 5xx error rate in course-reservations
```

## Logy

Notifikace a aplikační udalosti jsou logovane konzistentnim klic-hodnota formatem, napr. `event=enrollment_changed email=... status=...`.

V Kubernetes jsou logy vyhledatelne prikazem:

```bash
kubectl -n course-staging logs deployment/course-reservations-staging
```

## Secrets

Hesla nejsou ulozena v repozitari. Soubor `k8s/base/secret.example.yml` je pouze prazdna sablona. Realne hodnoty se vytvari pres `kubectl create secret` nebo pres CI secrets.

Docker Compose obsahuje jen lokalni vyvojove heslo pro izolovanou databazi, ne produkcni secret.

## Aplikacni zabezpeceni

Aplikace pouziva Spring Security jako hlavni bezpecnostni vrstvu. API je stateless, nepouziva serverove session a kazdy pozadavek na chranene endpointy musi obsahovat JWT token.

Prihlaseni:

- `POST /api/auth/login`
- vstupem je uzivatelske jmeno a heslo
- heslo se overuje pres `BCryptPasswordEncoder`
- vystupem je podepsany JWT token

Token se posila takto:

```http
Authorization: Bearer <token>
```

Role:

- `ADMIN`
- `INSTRUCTOR`
- `STUDENT`

Hrube rozdeleni pristupu je v `SecurityConfig`, kde jsou pravidla nad URL endpointy. Jemnejsi pravidla jsou v `AuthSessionService`, napr. vyucujici muze menit pouze svoje kurzy a student muze rusit pouze svuj zapis.

JWT token zpracovava `JwtAuthenticationFilter`. Pokud je token platny, filtr vytvori Spring Security autentizaci s odpovidajici roli. Pokud token chybi, API vraci `401`. Pokud role nestaci, API vraci `403`.

CSRF je vypnute zamerne, protoze aplikace nepouziva cookie session, ale Bearer token v hlavicce. CORS je omezeny na lokalni webove rozhrani a povolene HTTP metody.
