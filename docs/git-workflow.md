# Git workflow

## Vetve

- `main` reprezentuje stabilni stav.
- `feature/*` slouzi pro nove funkcionality.
- `hotfix/*` slouzi pro rychle opravy produkcni chyby.
- `codex/semester-course-devops` je aktualni pracovni vetev semestralni prace.

## Commit styl

Projekt pouziva conventional commits:

- `feat:` nova funkcionalita
- `test:` testy nebo akceptacni scenare
- `refactor:` zmena struktury bez zmeny chovani
- `ci:` pipeline
- `docs:` dokumentace
- `chore:` infrastruktura a udrzba

## TDD historie

V historii jsou oddelene commity pro domenovy model, testy domeny, service test s mockem, integracni API test a acceptance scenar. Pri obhajobe je mozne ukazat, jak testy dokumentuji pravidla a chrani refaktoring.
