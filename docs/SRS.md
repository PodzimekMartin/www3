# SRS - Software Requirements Specification

## 1. Ucel dokumentu

Tento dokument popisuje funkcni a nefunkcni pozadavky aplikace Course Reservations. Aplikace slouzi ke sprave vyskolskych kurzu, vyucujicich, studentu, terminu a zapisu.

## 2. Rozsah systemu

System umoznuje administratorovi spravovat studenty, vyucujici a kurzy. Vyucujici spravuje svoje kurzy. Student si prohlizi publikovane kurzy a prihlasuje se na ne.

System obsahuje:

- webove uzivatelske rozhrani
- REST API
- databazovou vrstvu
- autentizaci a autorizaci podle role
- automatizovane testy
- konfiguraci pro Docker a Kubernetes

## 3. Uzivatelske role

### Administrator

- vytvari studenty
- blokuje a odblokovava studenty
- vytvari vyucujici
- vytvari kurzy
- prirazuje vyucujiciho ke kurzu
- publikuje koncept kurzu
- meni kapacitu kurzu
- rusi kurz

### Vyucujici

- vidi pouze svoje kurzy
- vytvari svoje kurzy
- publikuje svoje koncepty
- meni kapacitu svych kurzu
- rusi svoje kurzy
- vidi prihlasene studenty u svych kurzu

### Student

- vidi publikovane kurzy
- vidi svoje zapsane kurzy
- prihlasuje se do kurzu
- rusi svuj zapis
- vidi, zda je zapsany nebo na cekaci listine

## 4. Funkcni pozadavky

### FR-01 Prihlaseni

System musi umoznit prihlaseni uzivatele pomoci uzivatelskeho jmena nebo e-mailu a hesla.

### FR-02 Role a opravneni

System musi rozlisovat role administrator, vyucujici a student. Kazda role vidi jen funkce, ke kterym ma opravneni.

### FR-03 Sprava studentu

Administrator musi umet vytvorit studenta, zobrazit seznam studentu, filtrovat studenty a blokovat nebo odblokovat studenta.

### FR-04 Sprava vyucujicich

Administrator musi umet vytvorit vyucujiciho a zobrazit seznam vyucujicich.

### FR-05 Sprava kurzu

Administrator a vyucujici musi umet vytvorit kurz s nazvem, kapacitou a terminem. Administrator navic vybira vyucujiciho, vyucujicimu se prirazeni doplni z prihlaseneho uctu.

### FR-06 Koncept a publikace

Kurz muze byt vytvoren jako koncept nebo rovnou publikovan. Koncept lze pozdeji publikovat.

### FR-07 Zapisy studentu

Student se muze prihlasit do publikovaneho kurzu. Pokud je kurz plny, system ho zaradi na cekaci listinu.

### FR-08 Ruseni zapisu

Student muze zrusit svuj zapis. Administrator a vyucujici mohou zrusit zapis v kurzu, ktery spravuji.

### FR-09 Cekaci listina

Pri uvolneni mista v kurzu musi system automaticky presunout prvniho cekatele mezi zapsane studenty.

### FR-10 Validace vstupu

System musi validovat povinna pole, format e-mailu, kapacitu kurzu a casy terminu.

## 5. Business pravidla

- Kurz nelze publikovat bez terminu.
- Student se nemuze prihlasit do nepublikovaneho kurzu.
- Blokovany student se nemuze prihlasit do kurzu.
- Jeden student nesmi byt ve stejnem kurzu dvakrat.
- Kapacita kurzu musi byt alespon 1.
- Kapacita nesmi byt mensi nez aktualni pocet zapsanych studentu.
- Konec terminu musi byt pozdeji nez zacatek terminu.
- Vyucujici muze spravovat pouze svoje kurzy.
- Student muze menit pouze svoje zapisy.

## 6. Nefunkcni pozadavky

### NFR-01 Spustitelnost

Aplikaci musi byt mozne spustit lokalne pres Maven i pres Docker Compose.

### NFR-02 Databaze

Aplikace musi pouzivat databazi. Pro lokalni vyvoj muze pouzit H2, pro kontejnerovy beh PostgreSQL.

### NFR-03 Testovatelnost

Aplikace musi obsahovat automatizovane testy pro domenova pravidla a API.

### NFR-04 Provozovatelnost

Aplikace musi poskytovat health endpoint pro kontrolu stavu.

### NFR-05 Bezpecnost

Citlive konfiguracni hodnoty nesmi byt ukladany primo do zdrojoveho kodu. Pro Kubernetes se pouziva Secret, pro CI/CD se pouzivaji secrets poskytovane platformou.

### NFR-06 Nasaditelnost

Projekt musi obsahovat Dockerfile, docker-compose konfiguraci a Kubernetes manifesty.

## 7. Externi rozhrani

- Webove UI: `http://localhost:8080`
- Swagger UI: `/swagger-ui/index.html`
- REST API: `/api/...`
- Health endpoint: `/actuator/health`

## 8. Akceptacni scenare

- Administrator vytvori vyucujiciho, studenta a novy kurz.
- Administrator vytvori kurz jako koncept a pote ho publikuje.
- Student se prihlasi do publikovaneho kurzu.
- Druhy student je pri naplnene kapacite zarazen na cekaci listinu.
- Po zruseni zapisu se cekatel automaticky presune mezi zapsane.
- Vyucujici vidi pouze svoje kurzy.
