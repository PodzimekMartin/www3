# Testovaci strategie

## TDD

Hlavni business logika vznikala od domenovych testu:

- `CourseDomainTest` pokryva pravidla kapacity, publikace, duplicit, blokace a cekaci listiny.
- Testy jsou psane stylem AAA: arrange, act, assert.
- Pravidla jsou v modelu `Course`, ne v controlleru, aby sla testovat rychle bez Spring kontextu.

## Unit testy

Unit testy overuji domenovy model bez databaze a HTTP vrstvy. Jsou rychle a tvori zaklad red-green-refactor cyklu.

## Integracni testy

`CourseControllerIntegrationTest` spousti Spring kontext, controller, service a H2 databazi. Overuje realisticke API toky vcetne chybovych odpovedi.

## Test doubles

`CourseServiceTest` pouziva Mockito mock pro `NotificationGateway`. Notifikace je externi hranice systemu, proto dava smysl ji v testu nahradit.

## BDD/ATDD

`src/test/resources/features/course-enrollment.feature` popisuje akceptacni scenare v Gherkin stylu. `CourseEnrollmentAcceptanceTest` overuje klicovy scenar cekaci listiny.

## Coverage

JaCoCo bezi v `mvn verify`. Nastavene quality gates:

- line coverage nejmene 70 %
- branch coverage nejmene 50 %

Coverage report je v CI publikovan jako artefakt `jacoco-report`.
