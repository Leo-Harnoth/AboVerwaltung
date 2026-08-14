package de.hwr.aboverwaltung;

import android.content.Intent;              
import android.os.Bundle;                    
import android.text.Editable;               
import android.text.TextWatcher;             
import android.view.View;                   
import android.widget.AdapterView;           
import android.widget.ArrayAdapter;         
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;                 

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity; 
import androidx.recyclerview.widget.LinearLayoutManager; 
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;     
import com.google.firebase.auth.FirebaseUser;     
import com.google.firebase.database.DataSnapshot; 
import com.google.firebase.database.DatabaseError; 
import com.google.firebase.database.DatabaseReference; 
import com.google.firebase.database.FirebaseDatabase;   
import com.google.firebase.database.MutableData;  
import com.google.firebase.database.Transaction;  
import com.google.firebase.database.ValueEventListener; 

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 1: Übersicht.
 * Zeigt alle aktiven Abos des eingeloggten Users als Liste.
 * Suchfunktion (Textsuche) + Filter (Kategorie).
 * Pro Abo gibt es einen "Genutzt"-Button für die Nutzungsstatistik.
 */


public class MainActivity extends AppCompatActivity implements AboAdapter.AboListener {

    private DatabaseReference aboRef; 
    private AboAdapter adapter;       



    private final List<Abo> alleAbos = new ArrayList<>();      
    private final List<Abo> gefilterteAbos = new ArrayList<>(); 

    private EditText editSuche;      
    private Spinner spinnerFilter;   
    private TextView textLeer;       

    /**
     * Wird einmalig beim Erzeugen des Screens aufgerufen. Prüft den Login-Status,
     * verknüpft das Layout, initialisiert RecyclerView/Spinner/Suchfeld samt ihren
     * Listenern und stößt am Ende das erste Laden der Abo-Daten an.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.activity_main); 

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish(); 
            return;   
        }
        String uid = user.getUid(); 
        aboRef = FirebaseDatabase.getInstance(Config.DATABASE_URL).getReference("abos").child(uid);

        editSuche = findViewById(R.id.editSuche);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        textLeer = findViewById(R.id.textLeer);

        RecyclerView recycler = findViewById(R.id.recyclerAbos);
        recycler.setLayoutManager(new LinearLayoutManager(this)); 
        adapter = new AboAdapter(gefilterteAbos, this); 
        recycler.setAdapter(adapter);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this, R.array.kategorien_filter, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
        spinnerFilter.setAdapter(spinnerAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                filternUndAnzeigen(); 
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { } 
        });

        
        editSuche.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { } 
            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                filternUndAnzeigen(); 
            }
            @Override
            public void afterTextChanged(Editable s) { } 
        });

        Button buttonHinzufuegen = findViewById(R.id.buttonHinzufuegen);
        buttonHinzufuegen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddAboActivity.class));
            }
        });

        Button buttonStatistik = findViewById(R.id.buttonStatistik);
        buttonStatistik.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, StatistikActivity.class));
            }
        });

        Button buttonLogout = findViewById(R.id.buttonLogout);
        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();               
                startActivity(new Intent(MainActivity.this, LoginActivity.class)); 
                finish(); 
            }
        });

        datenLaden(); 
    }

    /**
     * Lauscht dauerhaft auf Änderungen in Firebase (Realtime).
     */
    private void datenLaden() {
        aboRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                alleAbos.clear(); 
                for (DataSnapshot child : snapshot.getChildren()) {
                    Abo abo = child.getValue(Abo.class); 
                    if (abo != null) {
                        abo.setId(child.getKey()); 
                        alleAbos.add(abo);
                    }
                }
                filternUndAnzeigen(); 
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this,
                        "Fehler beim Laden: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Wendet Suche + Kategoriefilter auf die Liste an.
     */
    private void filternUndAnzeigen() {
        String suche = editSuche.getText().toString().toLowerCase().trim(); 
        String kategorie = spinnerFilter.getSelectedItem() != null
                ? spinnerFilter.getSelectedItem().toString() : "Alle"; 
        gefilterteAbos.clear(); 
        for (Abo abo : alleAbos) {
            boolean passtSuche = suche.isEmpty()
                    || abo.getName().toLowerCase().contains(suche);
            boolean passtKategorie = kategorie.equals("Alle")
                    || kategorie.equals(abo.getKategorie());
            if (passtSuche && passtKategorie) {
                gefilterteAbos.add(abo); 
            }
        }
        adapter.notifyDataSetChanged(); 
        textLeer.setVisibility(gefilterteAbos.isEmpty() ? View.VISIBLE : View.GONE); 
    }

    // ---------- Callbacks aus dem Adapter ----------

    /**
     * Klick auf ein Abo: Details-Screen öffnen.
     */
    @Override
    public void onAboClick(Abo abo) {
        Intent intent = new Intent(this, AboDetailActivity.class);
        intent.putExtra("abo", abo); 
        startActivity(intent);
    }

    /**
     * Klick auf "Genutzt": aktuellen Timestamp zur Nutzungsliste hinzufügen
     * und in Firebase speichern.
     */
    @Override
    public void onGenutztClick(Abo abo) {
        aboRef.child(abo.getId()).child("nutzungen")
                .runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                List<Long> nutzungen = new ArrayList<>();
                for (MutableData child : currentData.getChildren()) {
                    Long t = child.getValue(Long.class);
                    if (t != null) nutzungen.add(t);
                }
                nutzungen.add(System.currentTimeMillis()); 
                currentData.setValue(nutzungen); 
                return Transaction.success(currentData); 
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) { }
        });
        Toast.makeText(this, abo.getName() + ": Nutzung gespeichert",
                Toast.LENGTH_SHORT).show();
    }
}
