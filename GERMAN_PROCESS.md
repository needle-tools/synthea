## 3\. Meta-Modell: Ebenen

Wir unterscheiden **vier Ebenen**, auf denen wir den Prozess modellieren:

1. **Health Events (Patient Journey)**

   * Medizinische Meilensteine / „Health Events“, z. B.:

     * Diagnosesicherung invasives Mammakarzinom,

     * präoperatives Tumorboard mit Therapieplanung,

     * Start neoadjuvante Chemo,

     * Operation,

     * postoperatives Tumorboard,

     * Start Radiotherapie / Endokrintherapie.

2. **Termine (Appointments)**

   * Konkrete zeitliche Einladungen / Slots, z. B.:

     * Termin Befund-/Therapiegespräch,

     * OP-Termin,

     * Termin im Tumorboard,

     * Termine bei Radiologie, Kardiologie, Strahlentherapie.

3. **Tasks (Aufgaben)**

   * Workflowschritte, die einer Rolle zugeordnet sind, z. B.:

     * „Externe Befunde einscannen“ (Pflege),

     * „Tumorboard-Anmeldung ausfüllen“ (Arzt/Pflege),

     * „QS-Bogen ausfüllen“ (Dokumentationsassistenz),

     * „Implantateregister-Eintrag erstellen“ (Doku-Team),

     * „Termin in Strahlentherapie vereinbaren“ (Pflege/Koordination).

4. **Dokumente und Daten**

   * Konkrete Dokumente (Arztbriefe, Befundberichte, Formulare) – intern und extern – sowie die daraus extrahierten Daten.

   * Diese Ebene ist Basis für LLM-Auswertung und später für strukturierte Datensätze/FHIR.

---

## 4\. Patient Journey mit Health Events (Primärtherapie)

### 4.1 Scope & Startpunkt

* **Scope:** Primärtherapie bei **invasivem Mammakarzinom** (cM0; Rezidive, Metastasen, Nachsorge später).

* **Startpunkt:** Sobald ein invasives Mammakarzinom histologisch gesichert ist.

* **Phase 0 (nur leicht modelliert):** Verdachtsabklärung (Screening, BIRADS, Biopsie etc.) – wird mitgedacht, ist aber nicht Kern der ersten Pilotphase.

---

### 4.2 Gemeinsamer Einstieg (für beide Pfade)

**GEN-E01 – Diagnosesicherung invasives Mammakarzinom**

* Eingang Histologie-Befund („invasives Mammakarzinom“).

* Wichtige Daten:

  * Histologischer Typ, Grading, HR/HER2, Ki-67 (wenn vorhanden).

  * Datum der Diagnosesicherung.

* Dokumente:

  * Histologie-Bericht (Pathologie).

* Tasks:

  * Patientin in Brustzentrum-Terminplanung aufnehmen.

  * Unterlagen sammeln (Bildgebung, externe Befunde).

**GEN-E02 – Präoperatives Tumorboard mit Therapieplanung**

* Interdisziplinäres Tumorboard, Ziel: Therapieplanung.

* Input-Dokumente:

  * externe Mammografie-/Bildgebungsbefunde (MG, ggf. MRT/CT),

  * interne Sonografie, klinischer Befund,

  * Histologie (GEN-E01),

  * ggf. kardiologische Befunde, Labor, CHES/PROMs (später).

* Output:

  * Therapieempfehlung:

    * **OP-Pfad:** primär operative Therapie (z. B. BET, Mastektomie ± Axilla).

    * **NACT-Pfad:** neoadjuvante Systemtherapie \+ spätere OP.

  * ggf. Tasks:

    * zusätzliche Bildgebung (CT, MRT, Staging),

    * Empfehlung humangenetische Beratung (Phase 2+),

    * Empfehlung CHES-/PROMs-Einsatz.

* Dokument:

  * Tumorboard-Protokoll (präoperativ, mit Therapieplan).

---

### 4.3 OP-Pfad (Primär operative Therapie)

**OP-E03 – Befund- und Therapiegespräch (operativer Pfad)**

* Gespräch mit Patientin zur:

  * Mitteilung der Diagnose,

  * Erläuterung der Tumorboard-Empfehlung (OP-Pfad),

  * Erklärung der OP-Optionen und Risiken,

  * Einholen der Einwilligung.

* Dokumente:

  * Gesprächsdokumentation, Einverständniserklärungen, ggf. Aufklärungsbögen.

* Tasks:

  * OP-Terminierung,

  * Anästhesie-/Narkoseaufklärung organisieren,

  * ggf. präoperative Diagnostik (Labor, EKG, Röntgen).

**OP-E04 – Operation & stationärer Aufenthalt**

* Durchführung der Operation (BET/Mastektomie ± Axilla/SLNE etc.).

* Stationäre Phase: postop Überwachung, Visiten, Pflege.

* Dokumente:

  * OP-Bericht, Narkoseprotokoll, Pflegeberichte.

* Tasks:

  * Labor-/Postop-Kontrollen, Wundkontrollen, ggf. Schmerztherapie-Anpassungen.

**OP-E05 – Entlassung & Entlassungsbrief**

* Patientin wird aus stationärem Aufenthalt entlassen (Histologie liegt in der Regel NOCH NICHT vor).

* Dokumente:

  * Entlassungsbrief (inkl. OP-Verlauf, bisherige Befunde, geplanter weiterer Ablauf).

* Tasks:

  * Termin planen für Besprechung der definitiven Histologie (optional),

  * Weiterleitung an Hausärztin/niedergelassene Gynäkologie.

**OP-E06 – Postoperative Histologie**

* Eingang des definitiven OP-Histologieberichts (mehrere Tage nach OP, Patientin meist schon zu Hause).

* Wichtige Daten:

  * pTNM, R-Status, L-/V-Status, ggf. weitere Marker.

* Dokumente:

  * Pathologie-Befund zum OP-Präparat.

* Tasks:

  * Anmeldung zum postoperativen Tumorboard.

**OP-E07 – Postoperatives Tumorboard**

* Interdisziplinäre Besprechung der definitiven Situation.

* Input:

  * OP-Bericht, postop Histologie (OP-E06), bisherige Befunde.

* Output:

  * Entscheidung zu:

    * Radiotherapie (ja/nein, Art),

    * endokriner Therapie,

    * ggf. Chemo/Antikörper (postop),

    * ergänzende Diagnostik.

* Dokument:

  * Postoperatives Tumorboard-Protokoll.

* Tasks:

  * Termine für Strahlentherapie und endokrine Therapie planen,

  * ggf. humangenetische Beratung empfehlen (Phase 2+).

**OP-E08 – Start Radiotherapie**

* Startdatum, Dauer, ggf. Fraktionierung.

* Dokument:

  * Strahlentherapie-Bescheinigung/Bericht.

**OP-E09 – Start endokrine Therapie**

* Startdatum, Präparat, geplante Dauer.

* Dokument:

  * Arztbrief, Medikationseintrag.

**Tasks aus OP-Pfad, die zu QS/OncoZert/Implantateregister führen (Phase 2):**

* „QS-Bogen Mammachirurgie ausfüllen“ (Dokumentationsassistenz).

* „OncoZert-Tumordatensatz anlegen/ergänzen“.

* Bei Implantaten: „Implantateregister-Eintrag vorbereiten/ausfüllen“.

---

### 4.4 NACT-Pfad (Neoadjuvante Therapie)

**NACT-E03 – Befund- und Therapiegespräch (NACT-Pfad)**

* Gespräch mit Patientin analog OP-E03, aber mit Fokus auf:

  * Empfehlung neoadjuvante Chemo/Antikörper,

  * spätere OP-Planung.

* Tasks:

  * Starttermin NACT planen,

  * ggf. Port-Anlage, prätherapeutische Diagnostik (Echo, EKG, Labor).

**NACT-E04 – Start neoadjuvante Systemtherapie**

* Beginn NACT (Schema, Zyklen).

* Dokumente:

  * Chemo-Protokoll, Medikamentenpläne, Nebenwirkungsdoku.

**NACT-E05 – Response-Staging**

* Sono/MRT nach definierten Zyklen zur Beurteilung des Ansprechens.

* Dokument:

  * Bildgebungsbefund (Response: PR/CR/SD/PD).

* Daten:

  * Tumorgröße vor/nach NACT, ggf. RECIST/ähnliche Kriterien.

**NACT-E06 – Tumorboard nach NACT (TB\_post\_NACT)**

* Bewertung der Response und Planung der OP (Art, Umfang).

* Dokument:

  * Tumorboard-Protokoll (post-NACT).

* Tasks:

  * OP-Termin nach NACT,

  * ggf. weitere Bildgebung/Staging.

**NACT-E07 – Operation nach NACT**

* Analog OP-E04, aber nach Systemtherapie.

* Dokumente:

  * OP-Bericht, Narkoseprotokoll.

**NACT-E08 – Postoperative Histologie inkl. pCR-Bewertung**

* Pathologie-Befund mit:

  * pTNM, Residualtumor vs. pCR, R-Status etc.

* Dokument:

  * postop Pathologie-Bericht.

**NACT-E09 – Postoperatives Tumorboard nach NACT**

* Finalisierung der adjuvanten Strategie:

  * Radiatio, endokrine Therapie, ggf. weitere Systemtherapie.

* Dokument:

  * Post-NACT/Postop Tumorboard-Protokoll.

**NACT-E10 – Start Radiotherapie**  
**NACT-E11 – Start endokrine Therapie**

* wie im OP-Pfad, mit entsprechendem Zusammenhang zu NACT.

**Tasks analog OP-Pfad:**

* QS-Bogen, OncoZert, Implantateregister (falls relevant) durch Dokumentationsteam.

---

## 5\. Rollenmodell

**1\. Ärztliches Team**

* Aufgaben:

  * Klinische Untersuchung, OP-Indikation, Therapiewahl.

  * Teilnahme am Tumorboard, Formulierung der Therapieempfehlungen.

  * Befund-/Therapiegespräche mit Patientinnen.

  * Erstellung/Freigabe von Arztbriefen.

* Interaktion mit LLM:

  * Nutzung von LLM für Tumorboard-Zusammenfassungen.

  * Nutzung von LLM-Arztbriefentwürfen als Vorlage.

  * Fachliche Kontrolle der LLM-Ausgaben.

**2\. Pflegerisches Team**

* Aufgaben:

  * Scannen und Einlesen externer Befunde.

  * Unterstützung bei Terminorganisation (OP, Radiologie, Strahlentherapie).

  * Pflege-Dokumentation.

* Interaktion mit LLM:

  * ggf. Bereitstellung gescannter Dokumente als Input (später, Phase 3).

**3\. Dokumentationsassistenz / Dokumentationsteam**

* Aufgaben:

  * QS-Bogen Mammachirurgie ausfüllen.

  * OncoZert-Tumordatensatz pflegen.

  * Implantateregister-Einträge erstellen.

* Interaktion mit LLM (Phase 2+):

  * Nutzung strukturierter LLM-Outputs, um Formulare effizient auszufüllen.

**4\. IT-/KI-Team** (z. B. Christoph Demus, Julia Grey, Jan, Max Schütz, Markus Heise  z FSJlerin (Marie) als Unterstützung bei Vorbereitung von Fällen, Upload/Strukturierung von Dokumenten im Pilot.

u. a.)

* Aufgaben:

  * Betrieb des lokalen LLM-Systems.

  * Schnittstellen (Upload, Logging, Datenschutz).

  * Weiterentwicklung Richtung strukturierte Daten/FHIR.

z FSJlerin (Marie) als Unterstützung bei Vorbereitung von Fällen, Upload/Strukturierung von Dokumenten im Pilot.

---

## 6\. Dokumenttypen (intern/extern)

### 6.1 Externe Dokumente

* Mammografie-/Bildgebungsbefunde:

  * Papier (Scan), PDF, ggf. DICOM-Bilder im PACS.

* Weitere externe Befunde:

  * Kardiologie, frühere Operationen, Vorbehandlungen.

* Externer Medikationsplan:

  * Papier-BMP, Scan oder strukturierter eMP-Import.

LLM-Sicht:  
Diese Unterlagen dienen als Input für Tumorboard-Summaries, Arztbriefe, Belegungsmanagement-Abschnitt.

### 6.2 Interne Dokumente

* Sonografie-Befund Brust.

* Klinischer Untersuchungsbogen.

* Biopsiebericht / Prozedur-Doku.

* Histologie (präoperativ, postoperativ).

* Tumorboard-Protokolle:

  * präoperativ, post-NACT, postoperativ.

* OP-Bericht.

* Narkoseprotokoll.

* Pflege-Dokumentation.

* Entlassungsbrief(e).

* Systemtherapie-Dokumentation (Chemo, Antikörper, endokrine Therapie).

* QS-Bogen Mammachirurgie (im KIS-Formular).

* OncoZert-Tumordatensatz (durch Krebsregister/Doku-Team).

* Implantateregister (bei Implantateingriffen).

* Optional:

  * CHES-Reports (Anamnese, PROMs wie EORTC QLQ-C30, F-PREG).