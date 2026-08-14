# Abo Verwaltung – Android-App (Java + Firebase)

Uni-Projekt für den Kurs App-Entwicklung mit Android.

## Screens (5)
1. **LoginActivity** – Login/Registrierung mit E-Mail + Passwort (Firebase Auth)
2. **MainActivity (Übersicht)** – Liste aller Abos, Suche, Kategoriefilter, "Genutzt"-Button pro Abo
3. **AddAboActivity** – Formular zum Hinzufügen, Vorlagen-Suche (häufige Abos werden ab 3 Speicherungen als Vorlage in der DB abgelegt)
4. **AboDetailActivity** – Alle Details + berechnete Kündigungsfrist + Löschen
5. **StatistikActivity** – Gesamtausgaben/Monat, Kategorien, Hochrechnungen, Notwendigkeits-Bewertung 1–5

## Funktionalitäten (6)
1. Übersicht mit Suche + Filter + Nutzungs-Button
2. Abo hinzufügen inkl. Vorlagen-System
3. Abo-Details
4. Statistik (Gesamtausgaben, Kategorien, Hochrechnung)
5. Notwendigkeits-Bewertung (Skala 1–5, aus Nutzungsdaten)
6. Kündigungsfrist-Berechnung (nächstes Laufzeitende − Frist in Tagen)

## Firebase einrichten (WICHTIG – ohne das startet die App nicht)
1. Auf https://console.firebase.google.com ein neues Projekt anlegen
2. Android-App registrieren mit Package-Name: `de.hwr.aboverwaltung`
3. Die Datei `google-services.json` herunterladen und in den Ordner `app/` legen
4. In der Firebase Console:
   - **Authentication** → Anmeldemethode "E-Mail/Passwort" aktivieren
   - **Realtime Database** → Datenbank erstellen (Standort z.B. europe-west1), Testmodus-Regeln reichen für die Abgabe:
     ```json
     {
       "rules": {
         ".read": "auth != null",
         ".write": "auth != null"
       }
     }
     ```
5. Projekt in Android Studio öffnen, Gradle sync, starten

## Datenbankstruktur
```
/abos/{userId}/{aboId}     → Abos pro User
/vorlagen/{key}            → globale Vorlagen (ab 3 Speicherungen)
/vorlagenZaehler/{key}     → Zähler pro Abo-Name
```

## Kündigungsfrist-Logik
Startdatum + n × Laufzeit (Monate) = nächstes Laufzeitende in der Zukunft.
Kündigungsdatum = Laufzeitende − Kündigungsfrist (Tage).

## Nutzungs-Bewertung (Skala 1–5)
Zählt "Genutzt"-Bestätigungen der letzten 30 Tage:
0 → 1 | 1–2 → 2 | 3–5 → 3 | 6–10 → 4 | >10 → 5
