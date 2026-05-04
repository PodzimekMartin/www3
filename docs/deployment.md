# Nasazeni

## Lokalne bez Dockeru

```bash
mvn spring-boot:run
```

UI je dostupne na `http://localhost:8080`.

## Lokalne s Docker Compose

```bash
docker compose up --build
```

Compose spusti aplikaci a PostgreSQL databazi.

## Kubernetes staging

Priprava namespace a secretu:

```bash
kubectl create namespace course-staging
kubectl -n course-staging create secret generic course-reservations-secret-staging \
  --from-literal=SPRING_DATASOURCE_USERNAME=courses \
  --from-literal=SPRING_DATASOURCE_PASSWORD='<doplnit-v-CI-nebo-lokalne>'
kubectl apply -k k8s/staging
```

Pristup:

```bash
kubectl -n course-staging port-forward svc/course-reservations-staging 8080:80
```

## Kubernetes production

```bash
kubectl create namespace course-production
kubectl -n course-production create secret generic course-reservations-secret-production \
  --from-literal=SPRING_DATASOURCE_USERNAME=courses \
  --from-literal=SPRING_DATASOURCE_PASSWORD='<doplnit-v-CI-nebo-lokalne>'
kubectl apply -k k8s/production
```

## Rozdily prostredi

- staging ma jednu repliku a image tag `staging`
- production ma dve repliky a stabilni semver tag `1.0.0`
- konfigurace je pres ConfigMap, citlive hodnoty pres Secret

## CD

Pipeline pro `main` renderuje staging manifesty jako artefakt. V realnem clusteru se posledni krok nahradi prikazem `kubectl apply -k k8s/staging` s kubeconfigem ulozenym v CI secrets.
