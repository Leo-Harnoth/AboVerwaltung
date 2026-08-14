package de.hwr.aboverwaltung;

import android.os.Bundle;             
import android.view.View;            
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;         

import androidx.annotation.NonNull;            
import androidx.appcompat.app.AppCompatActivity; 

import com.google.firebase.auth.FirebaseAuth;     
import com.google.firebase.auth.FirebaseUser;      
import com.google.firebase.database.DataSnapshot; 
import com.google.firebase.database.DatabaseError; 
import com.google.firebase.database.DatabaseReference; 
import com.google.firebase.database.FirebaseDatabase;   
import com.google.firebase.database.ValueEventListener; 

import java.util.ArrayList; 
import java.util.HashMap;   
import java.util.List;
import java.util.Locale;    
import java.util.Map;      

/**
 * Screen 4: Statistik.
 * Funktionalität 4: Gesamtausgaben pro Monat, Aufteilung nach Kategorien,
 * Hochrechnung auf Jahr und 5 Jahre.
 * Funktionalität 5: Bewertung der Notwendigkeit jedes Abos (Skala 1-5)
 * basierend auf den "Genutzt"-Bestätigungen aus der Übersicht.
 */
public class StatistikActivity extends AppCompatActivity {

    private TextView textAusgaben, textKategorien, textHochrechnung, textBewertung;

    /**
     * Verknüpft das Layout, verdrahtet den Zurück-Button und stößt das Laden +
     * Berechnen der Statistikdaten an.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistik); 

        textAusgaben = findViewById(R.id.statAusgaben);
        textKategorien = findViewById(R.id.statKategorien);
        textHochrechnung = findViewById(R.id.statHochrechnung);
        textBewertung = findViewById(R.id.statBewertung);

        Button buttonZurueck = findViewById(R.id.buttonStatZurueck);
        buttonZurueck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        datenLadenUndBerechnen(); 
    }

    /**
     * Lädt einmalig alle Abos des eingeloggten Users aus Firebase und übergibt sie
     * anschließend an statistikBerechnen() zur eigentlichen Auswertung.
     */
    private void datenLadenUndBerechnen() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        String uid = user.getUid();
        DatabaseReference aboRef = FirebaseDatabase.getInstance(Config.DATABASE_URL)
                .getReference("abos").child(uid);

        aboRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Abo> abos = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Abo abo = child.getValue(Abo.class);
                    if (abo != null) abos.add(abo);
                }
                statistikBerechnen(abos); 
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StatistikActivity.this,
                        "Fehler beim Laden: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Berechnet aus der übergebenen Abo-Liste alle vier Statistik-Blöcke (Gesamtausgaben,
     * Kategorie-Aufteilung, Hochrechnung, Notwendigkeits-Bewertung) und schreibt sie in
     * die entsprechenden TextViews.
     */
    private void statistikBerechnen(List<Abo> abos) {
        if (abos.isEmpty()) {
            textAusgaben.setText("Noch keine Abos vorhanden.");
            textKategorien.setText("");
            textHochrechnung.setText("");
            textBewertung.setText("");
            return;
        }

        double summeMonat = 0;
        for (Abo abo : abos) {
            summeMonat += abo.getMonatsPreis(); 
        }
        textAusgaben.setText(String.format(Locale.GERMANY,
                "Gesamtausgaben pro Monat: %.2f €\nAnzahl aktiver Abos: %d",
                summeMonat, abos.size()));

        Map<String, Double> proKategorie = new HashMap<>(); 
        for (Abo abo : abos) {
            String kat = abo.getKategorie() == null ? "Sonstiges" : abo.getKategorie(); 
            double bisher = proKategorie.containsKey(kat) ? proKategorie.get(kat) : 0; 
            proKategorie.put(kat, bisher + abo.getMonatsPreis()); 
        }
        StringBuilder sbKat = new StringBuilder("Ausgaben nach Kategorie (pro Monat):\n");
        for (Map.Entry<String, Double> e : proKategorie.entrySet()) {
            double anteil = summeMonat > 0 ? e.getValue() / summeMonat * 100 : 0;
            sbKat.append(String.format(Locale.GERMANY,
                    "• %s: %.2f € (%.0f %%)\n", e.getKey(), e.getValue(), anteil));
        }
        textKategorien.setText(sbKat.toString().trim()); 

        textHochrechnung.setText(String.format(Locale.GERMANY,
                "Hochrechnung:\n• pro Quartal: %.2f €\n• pro Jahr: %.2f €\n• in 5 Jahren: %.2f €",
                summeMonat * 3, summeMonat * 12, summeMonat * 60));

        StringBuilder sbBew = new StringBuilder(
                "Notwendigkeits-Bewertung (1 = sehr inaktiv, 5 = sehr aktiv):\n");
        for (Abo abo : abos) {
            int bewertung = abo.getNutzungsBewertung(); 
            sbBew.append(String.format(Locale.GERMANY,
                    "• %s: %d/5 (%d Nutzungen in 30 Tagen)%s\n",
                    abo.getName(), bewertung, abo.getNutzungenLetzte30Tage(),
                    bewertung <= 2 ? " → Kündigung prüfen!" : "")); 
        }
        textBewertung.setText(sbBew.toString().trim());
    }
}
