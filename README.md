# Course Reservations

Semestralni full-stack aplikace pro spravu kurzu, zapisu, kapacit a cekaci listiny.

Projekt kombinuje dve zadani:

- vyvoj aplikace s TDD/BDD/ATDD, testy, coverage a CI/CD
- DevOps workflow vcetne Dockeru, Kubernetes manifestu, prostredi staging/production, observability a secrets

## Funkce

- sprava studentu a jejich blokace
- sprava kurzu, kapacity a publikace
- pridavani terminu kurzu
- zapisy studentu do kurzu
- automaticka cekaci listina pri naplneni kapacity
- automaticke posunuti prvniho cekatele po uvolneni mista
- prihlaseni s rolemi admin/student
- REST API a webovy frontend

## Prihlaseni

Po spusteni aplikace je nutne se prihlasit.

- admin: `admin` / `admin123`
- student: napr. `ada@example.test` / `student123`

Admin muze vytvaret studenty, kurzy, terminy, publikovat kurzy, menit kapacitu a blokovat studenty.
Student vidi publikovane kurzy, muze zapsat pouze sam sebe a vidi svoje zapisy.

## Business pravidla

- kurz nelze publikovat bez alespon jednoho terminu
- student se nemuze zapsat do nepublikovaneho kurzu
- blokovany student se nemuze zapsat
- student nesmi byt v jednom kurzu duplicitne
- pri naplnene kapacite jde student na cekaci listinu
- pri uvolneni mista se prvni cekatel automaticky zapise
- kapacita nesmi klesnout pod pocet aktivnich zapisu

## Spusteni lokalne

```bash
mvn spring-boot:run
```

Frontend: `http://localhost:8080`

## Testy a coverage

```bash
mvn verify
```

JaCoCo report: `target/site/jacoco/index.html`

Quality gate:

- line coverage >= 70 %
- branch coverage >= 50 %

## Docker

```bash
docker compose up --build
```

Compose spousti aplikaci a PostgreSQL databazi.

## Kubernetes

Manifesty jsou v `k8s/` a jsou rozdelene pres Kustomize:

- `k8s/staging`
- `k8s/production`

Detailni postup je v [docs/deployment.md](docs/deployment.md).

## CI/CD

GitHub Actions pipeline:

- Maven build
- unit, integration a acceptance testy
- Checkstyle
- JaCoCo coverage report
- upload test a coverage artefaktu
- Docker image build
- publikace image do GHCR mimo pull requesty
- render staging manifestu jako CD artefakt

Konfigurace: [.github/workflows/ci.yml](.github/workflows/ci.yml)

## Dokumentace

- [Architektura](docs/architecture.md)
- [Testovaci strategie](docs/testing-strategy.md)
- [Nasazeni](docs/deployment.md)
- [Release strategie](docs/release-strategy.md)
- [Observabilita a bezpecnost](docs/observability-security.md)

## Git workflow

Vyvoj probiha na vetvi `codex/semester-course-devops` s prubeznymi commity. Historie je zamerne clenena na domenovy model, testy, API, frontend a DevOps casti, aby byl dolozitelny postup vyvoje.
