package de.hwr.aboverwaltung;

import android.os.Bundle;          
import android.view.View;           
import android.widget.ArrayAdapter; 
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;       

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;       
import androidx.appcompat.app.AppCompatActivity; 

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;  
import com.google.firebase.database.DatabaseError; 
import com.google.firebase.database.DatabaseReference; 
import com.google.firebase.database.FirebaseDatabase;   
import com.google.firebase.database.MutableData;  
import com.google.firebase.database.Transaction;  
import com.google.firebase.database.ValueEventListener; 
import java.text.ParseException;
import java.text.SimpleDateFormat; 
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;          

/**
 * Screen 2: Abo hinzufügen.
 * Vorgegebenes Formular für konsistente Daten.
 * Zusätzlich: Vorlagen-Suche. Wird ein Abo häufig gespeichert (>= 3 mal
 * über alle User hinweg), liegt es als Vorlage unter /vorlagen in der DB
 * und kann per Suche übernommen werden.
 */
public class AddAboActivity extends AppCompatActivity {

    private static final int VORLAGEN_SCHWELLE = 3; 

    private EditText editName, editPreis, editStartdatum,
            editLaufzeit, editFrist, editBedingungen;
    private Spinner spinnerKategorie, spinnerIntervall;

    private DatabaseReference aboRef;      
    private DatabaseReference vorlagenRef; 
    private DatabaseReference zaehlerRef;  

    /**
     * Verknüpft das Formular-Layout, initialisiert die Kategorie-/Intervall-Spinner
     * und verdrahtet Speichern-, Vorlage-suchen- und Abbrechen-Button.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_abo);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        String uid = user.getUid();
        FirebaseDatabase db = FirebaseDatabase.getInstance(Config.DATABASE_URL);
        aboRef = db.getReference("abos").child(uid);   
        vorlagenRef = db.getReference("vorlagen");      
        zaehlerRef = db.getReference("vorlagenZaehler"); 

        editName = findViewById(R.id.editAboName);
        editPreis = findViewById(R.id.editAboPreis);
        editStartdatum = findViewById(R.id.editAboStartdatum);
        editLaufzeit = findViewById(R.id.editAboLaufzeit);
        editFrist = findViewById(R.id.editAboFrist);
        editBedingungen = findViewById(R.id.editAboBedingungen);
        spinnerKategorie = findViewById(R.id.spinnerKategorie);
        spinnerIntervall = findViewById(R.id.spinnerIntervall);

        ArrayAdapter<CharSequence> katAdapter = ArrayAdapter.createFromResource(
                this, R.array.kategorien, android.R.layout.simple_spinner_item);
        katAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
        spinnerKategorie.setAdapter(katAdapter);

        ArrayAdapter<CharSequence> intAdapter = ArrayAdapter.createFromResource(
                this, R.array.intervalle, android.R.layout.simple_spinner_item);
        intAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIntervall.setAdapter(intAdapter);

        Button buttonSpeichern = findViewById(R.id.buttonSpeichern);
        buttonSpeichern.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speichern();
            }
        });

        Button buttonVorlage = findViewById(R.id.buttonVorlageSuchen);
        buttonVorlage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vorlagenSuchen();
            }
        });

        Button buttonAbbrechen = findViewById(R.id.buttonAbbrechen);
        buttonAbbrechen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    /**
     * Formular validieren und Abo in Firebase speichern.
     */
    private void speichern() {
        String name = editName.getText().toString().trim();
        String preisText = editPreis.getText().toString().trim();
        String startdatum = editStartdatum.getText().toString().trim();
        String laufzeitText = editLaufzeit.getText().toString().trim();
        String fristText = editFrist.getText().toString().trim();

        // ---------- Validierung: bei jedem Fehler wird die Methode sofort verlassen ----------
        if (name.isEmpty()) {
            editName.setError("Name fehlt"); 
            return;
        }
        if (preisText.isEmpty()) {
            editPreis.setError("Preis fehlt");
            return;
        }
        if (!startdatum.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) { 
            editStartdatum.setError("Format: TT.MM.JJJJ");
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
        sdf.setLenient(false); 
        try {
            sdf.parse(startdatum);
        } catch (ParseException e) {
            editStartdatum.setError("Ungültiges Datum");
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

        Abo abo = new Abo(name,
                spinnerKategorie.getSelectedItem().toString(),
                preis,
                spinnerIntervall.getSelectedItem().toString(),
                startdatum, laufzeit, frist,
                editBedingungen.getText().toString().trim());

        aboRef.push().setValue(abo)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Abo gespeichert", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Speichern fehlgeschlagen: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
        vorlagenZaehlerErhoehen(abo);
    }

    /**
     * Zählt, wie oft ein Abo-Name gespeichert wurde.
     * Ab der Schwelle wird das Abo als globale Vorlage abgelegt.
     */
    private void vorlagenZaehlerErhoehen(final Abo abo) {
        final String key = abo.getName().toLowerCase()
                .replaceAll("[.#$\\[\\]/ ]", "_");

        zaehlerRef.child(key).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long anzahl = currentData.getValue(Long.class); 
                currentData.setValue(anzahl == null ? 1 : anzahl + 1); 
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (!committed || snapshot == null) return; 
                Long anzahl = snapshot.getValue(Long.class);
                if (anzahl != null && anzahl >= VORLAGEN_SCHWELLE) {
                    Abo vorlage = new Abo(abo.getName(), abo.getKategorie(),
                            abo.getPreis(), abo.getIntervall(),
                            "", abo.getLaufzeitMonate(), 
                            abo.getKuendigungsfristTage(), abo.getBedingungen());
                    vorlagenRef.child(key).setValue(vorlage);
                }
            }
        });
    }

    /**
     * Sucht Vorlagen anhand des eingegebenen Namens und zeigt
     * Treffer in einem Auswahldialog. Bei Auswahl wird das Formular befüllt.
     */
    private void vorlagenSuchen() {
        final String suche = editName.getText().toString().toLowerCase().trim();
        if (suche.isEmpty()) {
            Toast.makeText(this, "Bitte erst einen Namen eingeben", Toast.LENGTH_SHORT).show();
            return;
        }

        vorlagenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final List<Abo> treffer = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Abo vorlage = child.getValue(Abo.class);
                    if (vorlage != null
                            && vorlage.getName().toLowerCase().contains(suche)) {
                        treffer.add(vorlage);
                    }
                }

                if (treffer.isEmpty()) {
                    Toast.makeText(AddAboActivity.this,
                            "Keine Vorlage gefunden", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] namen = new String[treffer.size()];
                for (int i = 0; i < treffer.size(); i++) {
                    namen[i] = treffer.get(i).getName();
                }

                new AlertDialog.Builder(AddAboActivity.this)
                        .setTitle("Vorlage auswählen")
                        .setItems(namen, (dialog, which) ->
                                formularBefuellen(treffer.get(which))) 
                        .show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /**
     * Überträgt eine Vorlage ins Formular. Der User muss nur noch
     * das Startdatum ergänzen.
     */
    private void formularBefuellen(Abo vorlage) {
        editName.setText(vorlage.getName());
        editPreis.setText(String.valueOf(vorlage.getPreis()));
        editLaufzeit.setText(String.valueOf(vorlage.getLaufzeitMonate()));
        editFrist.setText(String.valueOf(vorlage.getKuendigungsfristTage()));
        editBedingungen.setText(vorlage.getBedingungen());

        setzeSpinner(spinnerKategorie, vorlage.getKategorie());
        setzeSpinner(spinnerIntervall, vorlage.getIntervall());

        Toast.makeText(this, "Vorlage übernommen – bitte Startdatum eintragen",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Sucht im Spinner nach einem bestimmten Text-Wert und wählt den passenden Eintrag aus.
     * Wird beim Übernehmen einer Vorlage genutzt, um Kategorie/Intervall vorzubelegen.
     */
    private void setzeSpinner(Spinner spinner, String wert) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(wert)) {
                spinner.setSelection(i);
                return; 
            }
        }
    }
}
