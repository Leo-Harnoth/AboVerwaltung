package de.hwr.aboverwaltung;

import android.content.Intent;   
import android.os.Bundle;        
import android.view.View;        
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;     

import androidx.annotation.NonNull;              
import androidx.appcompat.app.AppCompatActivity; 

import com.google.android.gms.tasks.OnCompleteListener; 
import com.google.android.gms.tasks.Task;               
import com.google.firebase.auth.AuthResult;             
import com.google.firebase.auth.FirebaseAuth;           

/**
 * Loginscreen mit E-Mail und Passwort über Firebase Authentication.
 * Ein neuer User kann sich über den Registrieren-Button anlegen.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText editEmail;    
    private EditText editPasswort;
    private FirebaseAuth auth;     

    /**
     * Verknüpft das Login-Layout und verdrahtet die Login-/Registrieren-Buttons
     * mit den jeweiligen Methoden.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); 

        auth = FirebaseAuth.getInstance(); 

        editEmail = findViewById(R.id.editEmail);
        editPasswort = findViewById(R.id.editPasswort);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonRegistrieren = findViewById(R.id.buttonRegistrieren);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });

        buttonRegistrieren.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registrieren();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() != null) {
            weiterZurUebersicht();
        }
    }

    /**
     * Prüft, ob E-Mail nicht leer und Passwort mindestens 6 Zeichen lang ist.
     * Zeigt bei Verstoß eine Fehlermeldung direkt am jeweiligen Feld an.
     */
    private boolean eingabenGueltig(String email, String passwort) {
        if (email.isEmpty()) {
            editEmail.setError("Bitte E-Mail eingeben");
            return false;
        }
        if (passwort.length() < 6) {
            editPasswort.setError("Passwort muss mindestens 6 Zeichen haben");
            return false;
        }
        return true;
    }

    /**
     * Liest E-Mail/Passwort aus, validiert sie und meldet den User bei Firebase Auth an.
     * Bei Erfolg geht es weiter zur Übersicht, sonst wird ein Fehler-Toast angezeigt.
     */
    private void login() {
        String email = editEmail.getText().toString().trim();
        String passwort = editPasswort.getText().toString(); 
        if (!eingabenGueltig(email, passwort)) return; 

        auth.signInWithEmailAndPassword(email, passwort)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            weiterZurUebersicht();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Login fehlgeschlagen: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Liest E-Mail/Passwort aus, validiert sie und legt bei Firebase Auth einen neuen
     * User-Account an. Bei Erfolg geht es weiter zur Übersicht.
     */
    private void registrieren() {
        String email = editEmail.getText().toString().trim();
        String passwort = editPasswort.getText().toString();
        if (!eingabenGueltig(email, passwort)) return;

        auth.createUserWithEmailAndPassword(email, passwort)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this,
                                    "Registrierung erfolgreich", Toast.LENGTH_SHORT).show();
                            weiterZurUebersicht();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Registrierung fehlgeschlagen: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void weiterZurUebersicht() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); 
    }
}
