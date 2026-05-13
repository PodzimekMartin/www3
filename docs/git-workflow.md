# Prace s Gitem

Projekt je veden v Git repozitari s prubeznymi commity. Commit zpravy pouzivaji kratke popisne prefixy.

## Doporuceny workflow

- `main` - stabilni hlavni vetev
- `feature/...` - vetev pro novou funkcionalitu
- `fix/...` - vetev pro opravu chyby

## Typy commitu

- `feat:` nova funkcionalita
- `fix:` oprava chyby
- `docs:` dokumentace
- `test:` testy
- `style:` vzhled nebo formatovani
- `chore:` technicka udrzba

## Prace s vetvemi

```bash
git switch -c feature/nazev-upravy
git add .
git commit -m "feat: popis zmeny"
git push
```

Po dokonceni zmeny je vhodne vytvorit pull request a nechat projit CI pipeline.
