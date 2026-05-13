# Uzivatelska a administratorska prirucka

## 1. Spusteni aplikace

### Docker

```bash
docker compose up --build
```

Po spusteni otevrit:

- aplikace: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- health status: `http://localhost:8080/actuator/health`

Vypnuti:

```bash
docker compose down
```

### Maven

```bash
mvn spring-boot:run
```

## 2. Prihlaseni

Ukazkove ucty:

- administrator: `admin` / `admin123`
- vyucujici: `teacher@example.test` / `teacher123`
- student: `ada@example.test` / `student123`

Po prihlaseni aplikace automaticky zobrazi rozhrani podle role.

## 3. Administrator

Administrator vidi hlavni zalozky:

- Kurzy
- Studenti
- Vyucujici

### 3.1 Vytvoreni studenta

1. Otevrit zalozku Studenti.
2. Vyplnit jmeno a e-mail.
3. Kliknout na `Vytvorit studenta`.

Student se pote muze prihlasit pomoci e-mailu. Pro lokalni ukazkove ucty je heslo nastavene v aplikaci.

### 3.2 Blokace studenta

1. Otevrit zalozku Studenti.
2. Najit studenta pomoci vyhledavani nebo filtru.
3. Kliknout na `Blokovat`.

Blokovany student se nemuze prihlasit do kurzu. Odblokovani se provede tlacitkem `Odblokovat`.

### 3.3 Vytvoreni vyucujiciho

1. Otevrit zalozku Vyucujici.
2. Vyplnit jmeno a e-mail.
3. Kliknout na `Vytvorit vyucujiciho`.

### 3.4 Vytvoreni kurzu

1. Otevrit zalozku Kurzy.
2. Vyplnit nazev kurzu.
3. Vybrat vyucujiciho.
4. Nastavit kapacitu.
5. Pridat alespon jeden termin.
6. Zvolit `Vytvorit koncept` nebo `Vytvorit a publikovat`.

Koncept neni pro studenty dostupny. Publikovany kurz je viditelny studentum a lze se na nej prihlasit.

### 3.5 Sprava kurzu

V seznamu kurzu je videt nazev, vyucujici, stav publikace a obsazenost. Detail se rozbali kliknutim na sipku.

Administrator muze:

- zobrazit detail
- publikovat koncept
- zmenit kapacitu
- zrusit kurz
- zrusit zapis studenta

## 4. Vyucujici

Vyucujici vidi pouze svoje kurzy.

### 4.1 Vytvoreni kurzu

1. Vyplnit nazev kurzu.
2. Nastavit kapacitu.
3. Pridat termin.
4. Zvolit vytvoreni konceptu nebo rovnou publikaci.

Pole vyucujici se nezobrazuje, protoze kurz se automaticky priradi prihlasenemu vyucujicimu.

### 4.2 Sprava vlastnich kurzu

Vyucujici muze:

- publikovat koncept
- menit kapacitu
- zrusit kurz
- sledovat zapsane studenty
- zrusit zapis studenta

## 5. Student

Student vidi dve casti:

- Vsechny kurzy
- Moje kurzy

### 5.1 Vsechny kurzy

Tato cast obsahuje publikovane kurzy. Student zde vidi stav obsazenosti:

- `Volno` - kurz ma volnou kapacitu
- `Plno` - kurz nema volnou kapacitu
- `Ceka` - existuje cekaci listina

Student se prihlasi tlacitkem `Zapsat se na kurz`.

### 5.2 Moje kurzy

Tato cast obsahuje kurzy, kde je student zapsany nebo na cekaci listine.

Student zde vidi stav:

- `Jsi zapsany`
- `Jsi na cekaci listine`

Zapis lze zrusit tlacitkem `Zrusit muj zapis`.

## 6. Filtry a vyhledavani

V seznamu kurzu lze filtrovat podle:

- nazvu nebo vyucujiciho
- stavu publikace
- obsazenosti

V administraci studentu lze filtrovat podle:

- jmena nebo e-mailu
- aktivniho nebo blokovaneho stavu

V administraci vyucujicich lze vyhledavat podle:

- jmena
- e-mailu

## 7. Nejbeznejsi chyby

### Kurz nejde publikovat

Kurz pravdepodobne nema zadny termin. Je nutne vytvorit kurz alespon s jednim terminem.

### Student nevidi kurz

Kurz je pravdepodobne stale koncept. Student vidi pouze publikovane kurzy.

### Student se nemuze prihlasit

Mozne duvody:

- student je blokovany
- kurz neni publikovany
- student uz je v kurzu zapsany

### Kapacita nejde ulozit

Kapacita nesmi byt mensi nez aktualni pocet zapsanych studentu.
