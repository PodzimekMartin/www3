Feature: Zapisy do kurzu
  Spravce kurzu potrebuje ridit kapacitu a cekaci listinu.

  Scenario: Student je zarazen na cekaci listinu po naplneni kapacity
    Given existuje publikovany kurz s kapacitou 1
    And v systemu jsou dva aktivni studenti
    When prvni student se zapise do kurzu
    And druhy student se zapise do kurzu
    Then prvni student je zapsany
    And druhy student je na cekaci listine

  Scenario: Uvolnene misto automaticky posune cekatele
    Given existuje publikovany kurz s kapacitou 1
    And jeden student je zapsany a druhy ceka
    When zapsany student zrusi ucast
    Then cekajici student je automaticky zapsany
