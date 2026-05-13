# Testovani a kvalita

Projekt obsahuje automatizovane testy na nekolika urovnich.

## Domenove testy

Domenove testy overuji business pravidla bez webove vrstvy. Kontroluji napriklad kapacitu kurzu, cekaci listinu, validaci terminu a publikaci kurzu.

Umisteni:

- `src/test/java/cz/semester/courseapp/domain`

## Aplikacni testy

Aplikacni testy overuji spolupraci sluzeb, repository a databazove vrstvy.

Umisteni:

- `src/test/java/cz/semester/courseapp/app`

## Integracni testy API

Integracni testy overuji REST API, autentizaci, autorizaci a databazovou integraci.

Umisteni:

- `src/test/java/cz/semester/courseapp/http`

## Coverage

Pokryti testy se meri pomoci JaCoCo.

Spusteni:

```bash
mvn verify
```

Report:

```text
target/site/jacoco/index.html
```

## Staticka kontrola

Soucasti buildu je Checkstyle konfigurace ve slozce `config/checkstyle`.
