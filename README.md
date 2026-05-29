# Course Reservations

Semestralni aplikace pro spravu vyskolskych kurzu, vyucujicich, studentu, terminu a zapisu. Projekt obsahuje webove rozhrani, REST API, databazovou perzistenci, automatizovane testy, Docker konfiguraci a pripravu pro nasazeni do Kubernetes.

## Hlavni funkce

- prihlaseni podle role: administrator, vyucujici, student
- zabezpecene REST API pres podepsany JWT Bearer token
- sprava studentu vcetne blokace uctu
- sprava vyucujicich
- tvorba kurzu s kapacitou, terminem a prirazenym vyucujicim
- rozliseni konceptu a publikovaneho kurzu
- zapis studentu do publikovanych kurzu
- automaticka cekaci listina pri naplnene kapacite
- automaticky presun prvniho cekatele po uvolneni mista
- prehledne studentske rozdeleni na dostupne kurzy a moje kurzy
- REST API dostupne pres Swagger UI
- vyhledavani kurzu podle nazvu nebo vyucujiciho se strankovanim
- jednotny format chybovych odpovedi API
- logovani API pozadavku, validacnich chyb a domenovych konfliktu
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

## Architektura backendu

Aplikace je rozdelena do vrstev:

- `domain` - entity a domenova pravidla (`Course`, `Student`, `Instructor`, `Enrollment`, `CourseSession`)
- `app` - service vrstva s hlavni business logikou (`CourseService`, `AuthSessionService`, `JwtTokenService`)
- `infra` - repository/DAO vrstva a technicke integrace (`CourseRepository`, `StudentRepository`, `InstructorRepository`)
- `http` - REST controllery, DTO, validace, error handling a OpenAPI konfigurace
- `config` a `resources` - konfigurace aplikace, databaze, profilu a statickeho weboveho rozhrani

Ucelena funkcni vetev pro obhajobu je sprava kurzu a zapisu: entita `Course` a souvisejici entity, repository dotazy, service pravidla, REST endpointy, DTO, validace, logovani a automatizovane testy.

## Zabezpeceni API

Aplikace pouziva Spring Security. Backend je nastaveny jako stateless REST API, takze nevytvari serverovou session a kazdy zabezpeceny pozadavek se overuje pomoci JWT tokenu.

Prihlaseni probiha pres endpoint `POST /api/auth/login`. Hesla se overuji pres `BCryptPasswordEncoder`, nejsou porovnavana jako plaintext. Po uspesnem prihlaseni aplikace vrati podepsany JWT token. Klient ho posila v hlavicce:

```http
Authorization: Bearer <token>
```

Role:

- `ADMIN` muze spravovat studenty, vyucujici a vsechny kurzy
- `INSTRUCTOR` muze spravovat pouze svoje kurzy
- `STUDENT` vidi publikovane kurzy a muze spravovat pouze svoje zapisy

Bezpecnostni pravidla jsou rozdělena do dvou urovni:

- `SecurityConfig` chrani endpointy podle role jeste pred vstupem do controlleru
- `AuthSessionService` resi jemnejsi vlastnicke kontroly, napr. ze vyucujici smi spravovat jen svoje kurzy a student jen svoje zapisy

JWT tokeny nacita `JwtAuthenticationFilter`, ktery z nich vytvari Spring Security autentizaci s roli `ROLE_ADMIN`, `ROLE_INSTRUCTOR` nebo `ROLE_STUDENT`. Neplatny nebo chybejici token vede na odpoved `401`, nedostatecna role na `403`.

Tajny klic pro JWT se bere z promenne prostredi `APP_JWT_SECRET`, aby nemusel byt pevne ulozeny ve zdrojovem kodu. Aplikace ma nastavene zakladni CORS pravidlo pro lokalni webove rozhrani a vypnutou CSRF ochranu, protoze jde o stateless REST API s Bearer tokenem, ne cookie session aplikaci.

## Databaze a dotazy

Projekt pouziva Spring Data JPA. Lokalne bez Dockeru bezi H2 databaze, v Docker Compose se pouziva PostgreSQL.

CRUD operace jsou dostupne pro studenty, vyucujici, kurzy, terminy a zapisy. Slozitejsi dotaz je v `CourseRepository.searchCourses`, kde se hleda pres kurz i prirazeneho vyucujiciho a vysledek je strankovany.

Priklad:

```http
GET /api/courses/search?query=java&page=0&size=5
```

## Validace a chybove odpovedi

Vstupy se validuji pomoci Jakarta Validation anotaci a vlastni validace `@ValidSessionWindow`, ktera hlida, aby konec terminu kurzu byl po zacatku.

Chyby API vraci jednotny JSON format:

```json
{
  "message": "Popis chyby",
  "status": 400
}
```

## Logovani a monitoring

API pozadavky loguje `ApiRequestLoggingFilter`. Domenove chyby, validacni chyby a zakazane akce loguje `ApiExceptionHandler`.

Monitoring je dostupny pres Spring Boot Actuator:

- health check: `http://localhost:8080/actuator/health`
- metriky: `http://localhost:8080/actuator/metrics`

## API dokumentace

Swagger UI je dostupny po spusteni aplikace:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI definice obsahuje Bearer JWT zabezpeceni, takze ve Swaggeru lze vlozit token pres tlacitko Authorize.

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

Testovana funkcnost pokryva zejmena:

- domenova pravidla zapisu a cekaci listiny
- service vrstvu s mockovanou notifikacni branou
- REST API vcetne autorizace, validace, JWT a vyhledavani
- acceptance scenar prihlaseni studentu do kurzu

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
