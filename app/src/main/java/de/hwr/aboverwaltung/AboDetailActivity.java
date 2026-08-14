package de.hwr.aboverwaltung;

import android.os.Bundle;              
import android.view.View;              
import android.widget.ArrayAdapter;   
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;           

import androidx.appcompat.app.AlertDialog;      
import androidx.appcompat.app.AppCompatActivity; 

import com.google.firebase.auth.FirebaseAuth;      
import com.google.firebase.auth.FirebaseUser;      
import com.google.firebase.database.DatabaseReference; 
import com.google.firebase.database.FirebaseDatabase;   

import java.text.ParseException;
import java.text.SimpleDateFormat; 
import java.util.Date;             
import java.util.Locale;           

/**
 * Screen 3: Abo-Details.
 * Zeigt alle Details eines Abos inkl. der berechneten Kündigungsfrist
 * (Funktionalität 6). Das Abo wird über den Intent als Extra übergeben.
 * Über "Bearbeiten" lassen sich alle Eigenschaften (Name, Preis, Kategorie, ...)
 * direkt in diesem Screen ändern, statt nur löschen zu können.
 */
public class AboDetailActivity extends AppCompatActivity {

    private Abo abo;                  
    private DatabaseReference aboRef; 

    private EditText detailName, detailPreis, detailStartdatum,
            detailLaufzeit, detailFrist, detailBedingungen;
    private Spinner spinnerDetailKategorie, spinnerDetailIntervall;
    private TextView textMonatspreis, textKuendigung, textNutzung;

    private Button buttonBearbeiten, buttonSpeichern, buttonBearbeitenAbbrechen,
            buttonLoeschen, buttonZurueck;

    /**
     * Liest das per Intent übergebene Abo aus, befüllt die Felder und verdrahtet
     * Bearbeiten-/Löschen-/Zurück-Buttons.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_abo_detail);

        abo = (Abo) getIntent().getSerializableExtra("abo"); 
        if (abo == null) {
            finish(); 
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        String uid = user.getUid();
        aboRef = FirebaseDatabase.getInstance(Config.DATABASE_URL).getReference("abos").child(uid).child(abo.getId());

        detailName = findViewById(R.id.detailName);
        spinnerDetailKategorie = findViewById(R.id.spinnerDetailKategorie);
        detailPreis = findViewById(R.id.detailPreis);
        spinnerDetailIntervall = findViewById(R.id.spinnerDetailIntervall);
        textMonatspreis = findViewById(R.id.textMonatspreis);
        detailStartdatum = findViewById(R.id.detailStartdatum);
        detailLaufzeit = findViewById(R.id.detailLaufzeit);
        detailFrist = findViewById(R.id.detailFrist);
        detailBedingungen = findViewById(R.id.detailBedingungen);
        textKuendigung = findViewById(R.id.detailKuendigung);
        textNutzung = findViewById(R.id.detailNutzung);

        buttonBearbeiten = findViewById(R.id.buttonBearbeiten);
        buttonSpeichern = findViewById(R.id.buttonSpeichern);
        buttonBearbeitenAbbrechen = findViewById(R.id.buttonBearbeitenAbbrechen);
        buttonLoeschen = findViewById(R.id.buttonLoeschen);
        buttonZurueck = findViewById(R.id.buttonZurueck);

        ArrayAdapter<CharSequence> katAdapter = ArrayAdapter.createFromResource(
                this, R.array.kategorien, android.R.layout.simple_spinner_item);
        katAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDetailKategorie.setAdapter(katAdapter);

        ArrayAdapter<CharSequence> intAdapter = ArrayAdapter.createFromResource(
                this, R.array.intervalle, android.R.layout.simple_spinner_item);
        intAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDetailIntervall.setAdapter(intAdapter);

        feldWerteSetzen(abo); 
        anzeigeAktualisieren(); 
        bearbeitenModusUmschalten(false); 

        buttonBearbeiten.setOnClickListener(view -> bearbeitenModusUmschalten(true));

        buttonBearbeitenAbbrechen.setOnClickListener(view -> {
            feldWerteSetzen(abo); 
            bearbeitenModusUmschalten(false);
        });

        buttonSpeichern.setOnClickListener(view -> speichernBearbeitung());

        buttonLoeschen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loeschenBestaetigen(); 
            }
        });

        buttonZurueck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    /**
     * Überträgt die Werte eines Abo-Objekts in die Eingabefelder.
     * Wird initial beim Öffnen genutzt und beim Abbrechen einer Bearbeitung,
     * um ungespeicherte Änderungen zu verwerfen.
     */
    private void feldWerteSetzen(Abo quelle) {
        detailName.setText(quelle.getName());
        detailPreis.setText(String.valueOf(quelle.getPreis()));
        detailStartdatum.setText(quelle.getStartdatum());
        detailLaufzeit.setText(String.valueOf(quelle.getLaufzeitMonate()));
        detailFrist.setText(String.valueOf(quelle.getKuendigungsfristTage()));
        detailBedingungen.setText(quelle.getBedingungen());
        setzeSpinner(spinnerDetailKategorie, quelle.getKategorie());
        setzeSpinner(spinnerDetailIntervall, quelle.getIntervall());
    }

    /**
     * Sucht im Spinner nach einem bestimmten Text-Wert und wählt den passenden Eintrag aus.
     */
    private void setzeSpinner(Spinner spinner, String wert) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(wert)) {
                spinner.setSelection(i);
                return; // gefunden -> Schleife kann abgebrochen werden
            }
        }
    }

    private final View.OnTouchListener spinnerBlockieren = (v, event) -> true;

    /**
     * Schaltet zwischen Anzeige- und Bearbeitungsmodus um: macht die Eingabefelder
     * fokussierbar und blendet die passenden Buttons ein bzw. aus.
     */
    private void bearbeitenModusUmschalten(boolean bearbeitbar) {
        detailName.setFocusable(bearbeitbar);
        detailName.setFocusableInTouchMode(bearbeitbar);
        detailPreis.setFocusable(bearbeitbar);
        detailPreis.setFocusableInTouchMode(bearbeitbar);
        detailStartdatum.setFocusable(bearbeitbar);
        detailStartdatum.setFocusableInTouchMode(bearbeitbar);
        detailLaufzeit.setFocusable(bearbeitbar);
        detailLaufzeit.setFocusableInTouchMode(bearbeitbar);
        detailFrist.setFocusable(bearbeitbar);
        detailFrist.setFocusableInTouchMode(bearbeitbar);
        detailBedingungen.setFocusable(bearbeitbar);
        detailBedingungen.setFocusableInTouchMode(bearbeitbar);

        spinnerDetailKategorie.setOnTouchListener(bearbeitbar ? null : spinnerBlockieren);
        spinnerDetailIntervall.setOnTouchListener(bearbeitbar ? null : spinnerBlockieren);

        buttonBearbeiten.setVisibility(bearbeitbar ? View.GONE : View.VISIBLE);
        buttonSpeichern.setVisibility(bearbeitbar ? View.VISIBLE : View.GONE);
        buttonBearbeitenAbbrechen.setVisibility(bearbeitbar ? View.VISIBLE : View.GONE);
        buttonLoeschen.setVisibility(bearbeitbar ? View.GONE : View.VISIBLE);
        buttonZurueck.setVisibility(bearbeitbar ? View.GONE : View.VISIBLE);
    }

    /**
     * Validiert das Formular und schreibt
     * die geänderten Werte zurück nach Firebase.
     */
    private void speichernBearbeitung() {
        String name = detailName.getText().toString().trim();
        String preisText = detailPreis.getText().toString().trim();
        String startdatum = detailStartdatum.getText().toString().trim();
        String laufzeitText = detailLaufzeit.getText().toString().trim();
        String fristText = detailFrist.getText().toString().trim();

        if (name.isEmpty()) {
            detailName.setError("Name fehlt");
            return;
        }
        if (preisText.isEmpty()) {
            detailPreis.setError("Preis fehlt");
            return;
        }
        if (!startdatum.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
            detailStartdatum.setError("Format: TT.MM.JJJJ");
            return;
        }
        SimpleDateFormat sdfPruefung = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
        sdfPruefung.setLenient(false);
        try {
            sdfPruefung.parse(startdatum);
        } catch (ParseException e) {
            detailStartdatum.setError("Ungültiges Datum");
            return;
        }

        double preis;
        int laufzeit, frist;
        try {
            preis = Double.parseDouble(preisText.replace(",", "."));
            laufzeit = laufzeitText.isEmpty() ? 1 : Integer.parseInt(laufzeitText);
            frist = fristText.isEmpty() ? 0 : Integer.parseInt(fristText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Bitte gültige Zahlen eingeben", Toast.LENGTH_SHORT).show();
            return;
        }

        abo.setName(name);
        abo.setKategorie(spinnerDetailKategorie.getSelectedItem().toString());
        abo.setPreis(preis);
        abo.setIntervall(spinnerDetailIntervall.getSelectedItem().toString());
        abo.setStartdatum(startdatum);
        abo.setLaufzeitMonate(laufzeit);
        abo.setKuendigungsfristTage(frist);
        abo.setBedingungen(detailBedingungen.getText().toString().trim());

        aboRef.setValue(abo)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(AboDetailActivity.this, "Abo aktualisiert", Toast.LENGTH_SHORT).show();
                    bearbeitenModusUmschalten(false);
                    anzeigeAktualisieren();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(AboDetailActivity.this,
                                "Speichern fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Berechnet Monatspreis, Kündigungsfrist und Nutzungsbewertung neu und zeigt sie an.
     * Wird beim initialen Öffnen sowie nach jeder erfolgreichen Bearbeitung aufgerufen,
     * damit der Screen nicht die alten Werte stehen lässt.
     */
    private void anzeigeAktualisieren() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);

        textMonatspreis.setText(String.format(Locale.GERMANY,
                "entspricht %.2f € pro Monat", abo.getMonatsPreis()));

        Date kuendigung = abo.getKuendigungsdatum();
        Date laufzeitende = abo.getNaechstesLaufzeitende();
        if (kuendigung != null && laufzeitende != null) {
            long diffMillis = kuendigung.getTime() - System.currentTimeMillis();
            long tageBisKuendigung = diffMillis / (1000L * 60 * 60 * 24); 

            String hinweis;
            if (diffMillis < 0) {
                hinweis = "Die Kündigungsfrist für die aktuelle Periode ist bereits abgelaufen.";
            } else {
                hinweis = "Noch " + tageBisKuendigung + " Tage Zeit zum Kündigen.";
            }

            textKuendigung.setText("Nächstes Laufzeitende: " + sdf.format(laufzeitende)
                    + "\nKündigen bis spätestens: " + sdf.format(kuendigung)
                    + "\n" + hinweis);
        } else {
            textKuendigung.setText("Kündigungsfrist konnte nicht berechnet werden "
                    + "(Startdatum prüfen).");
        }

        textNutzung.setText("Nutzungen in den letzten 30 Tagen: "
                + abo.getNutzungenLetzte30Tage()
                + " (Bewertung: " + abo.getNutzungsBewertung() + "/5)");
    }

    /**
     * Zeigt einen Bestätigungsdialog an und löscht das Abo aus Firebase erst,
     * wenn der User das im Dialog ausdrücklich bestätigt.
     */
    private void loeschenBestaetigen() {
        new AlertDialog.Builder(this)
                .setTitle("Abo kündigen/löschen")
                .setMessage("Soll \"" + abo.getName() + "\" wirklich gelöscht werden?")
                .setPositiveButton("Löschen", (dialog, which) -> {
            
                    aboRef.removeValue()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(AboDetailActivity.this,
                                        "Abo gelöscht", Toast.LENGTH_SHORT).show();
                                finish(); 
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(AboDetailActivity.this,
                                            "Löschen fehlgeschlagen: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Abbrechen", null) 
                .show();
    }
}
