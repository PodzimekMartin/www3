# Course Reservations

Semestralni aplikace pro spravu vyskolskych kurzu, vyucujicich, studentu, terminu a zapisu. Projekt obsahuje webove rozhrani, REST API, databazovou perzistenci, automatizovane testy, Docker konfiguraci a pripravu pro nasazeni do Kubernetes.

## Hlavni funkce

- prihlaseni podle role: administrator, vyucujici, student
- sprava studentu vcetne blokace uctu
- sprava vyucujicich
- tvorba kurzu s kapacitou, terminem a prirazenym vyucujicim
- rozliseni konceptu a publikovaneho kurzu
- zapis studentu do publikovanych kurzu
- automaticka cekaci listina pri naplnene kapacite
- automaticky presun prvniho cekatele po uvolneni mista
- prehledne studentske rozdeleni na dostupne kurzy a moje kurzy
- REST API dostupne pres Swagger UI
- Docker Compose pro lokalni spusteni aplikace s PostgreSQL
- Kubernetes manifesty pro staging a production prostredi

## Role

### Administrator

Administrator muze vytvaret studenty, vyucujici a kurzy. Pri vytvareni kurzu prirazuje vyucujiciho, nastavuje kapacitu, pridava termin a rozhoduje, zda bude kurz ulozen jako koncept nebo rovnou publikovan.

### Vyucujici

Vyucujici vidi pouze svoje kurzy. Muze vytvaret vlastni kurzy, publikovat koncepty, menit kapacitu a zrusit kurz.

### Student

Student vidi publikovane kurzy a svoje zapisy. Muze se prihlasit do kurzu, zrusit svuj zapis a sledovat, zda je zapsany nebo na cekaci listine.

## Prihlasovaci udaje pro lokalni beh

Po spusteni aplikace jsou k dispozici ukazkove ucty:

- administrator: `admin` / `admin123`
- vyucujici: `teacher@example.test` / `teacher123`
- student: `ada@example.test` / `student123`

## Business pravidla

- Kurz nelze publikovat bez alespon jednoho terminu.
- Student se muze prihlasit pouze do publikovaneho kurzu.
- Blokovany student se nemuze prihlasit do kurzu.
- Student nesmi byt v jednom kurzu zapsan vicekrat.
- Pokud je kapacita kurzu naplnena, dalsi student je zarazen na cekaci listinu.
- Pri zruseni zapisu se prvni student z cekaci listiny automaticky presune mezi zapsane.
- Kapacita kurzu nesmi byt snizena pod aktualni pocet zapsanych studentu.
- Vyucujici muze spravovat pouze svoje kurzy.
- Student muze vytvaret nebo rusit pouze svuj vlastni zapis.

## Spusteni pres Docker

```bash
docker compose up --build
```

Aplikace bude dostupna na:

- webove rozhrani: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- health check: `http://localhost:8080/actuator/health`

Vypnuti:

```bash
docker compose down
```

## Spusteni bez Dockeru

```bash
mvn spring-boot:run
```

Vychozi konfigurace pouziva H2 databazi. Produkcni profil pocita s PostgreSQL.

## Testy a kvalita

```bash
mvn verify
```

Prikaz provede build, automatizovane testy, statickou kontrolu stylu kodu a vygeneruje report pokryti testy.

Vystupy:

- test reporty: `target/surefire-reports`
- coverage report: `target/site/jacoco/index.html`

## CI/CD

Pipeline je definovana v `.github/workflows/ci.yml` a obsahuje:

- sestaveni aplikace
- spusteni testu
- statickou kontrolu kodu
- vygenerovani test a coverage reportu
- sestaveni Docker image
- publikaci image do GitHub Container Registry
- pripravu staging manifestu jako artefaktu pipeline

## Kubernetes

Kubernetes konfigurace je ve slozce `k8s/`.

- `k8s/base` obsahuje spolecne manifesty
- `k8s/staging` obsahuje staging overlay
- `k8s/production` obsahuje production overlay
- `k8s/canary` obsahuje ukazku canary varianty

Zakladni objekty:

- Deployment
- Service
- Ingress
- ConfigMap
- Secret example
- PostgreSQL manifest
- resource requests a limity

## Dokumentace

- [SRS - specifikace pozadavku](docs/SRS.md)
- [SDD - navrh systemu](docs/SDD.md)
- [Uzivatelska a administratorska prirucka](docs/USER_ADMIN_GUIDE.md)
- [Architektura](docs/architecture.md)
- [Nasazeni](docs/deployment.md)
- [Observabilita a bezpecnost](docs/observability-security.md)
- [Release strategie](docs/release-strategy.md)
- [Prace s Gitem](docs/git-workflow.md)

## Technologie

- Java 21
- Spring Boot
- Spring Data JPA
- H2 / PostgreSQL
- Maven
- HTML, CSS, JavaScript
- Docker, Docker Compose
- Kubernetes, Kustomize
- GitHub Actions
