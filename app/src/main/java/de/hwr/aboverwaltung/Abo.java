package de.hwr.aboverwaltung;

import java.io.Serializable;   
import java.text.SimpleDateFormat; 
import java.util.ArrayList;
import java.util.Calendar;     
import java.util.Date;
import java.util.List;
import java.util.Locale;       
import com.google.firebase.database.Exclude; 

/**
 * Datenmodell für ein Abonnement.
 * Wird 1:1 in der Firebase Realtime Database gespeichert,
 * daher: leerer Konstruktor + Getter/Setter (Firebase-Konvention).
 */

public class Abo implements Serializable {

    private String id;              
    private String name;           
    private String kategorie;       
    private double preis;           
    private String intervall;      
    private String startdatum;      
    private int laufzeitMonate;     
    private int kuendigungsfristTage; 
    private String bedingungen;     
    private List<Long> nutzungen;   

    
    public Abo() {
       
    }

   
    public Abo(String name, String kategorie, double preis, String intervall,
               String startdatum, int laufzeitMonate, int kuendigungsfristTage,
               String bedingungen) {
        this.name = name;
        this.kategorie = kategorie;
        this.preis = preis;
        this.intervall = intervall;
        this.startdatum = startdatum;
        this.laufzeitMonate = laufzeitMonate;
        this.kuendigungsfristTage = kuendigungsfristTage;
        this.bedingungen = bedingungen;
        this.nutzungen = new ArrayList<>();
    }

    // ---------- Berechnungen ----------

    /**
     * Rechnet den Preis auf einen Monat um.
     */
    
   @Exclude
    public double getMonatsPreis() {
        if ("jährlich".equals(intervall)) {
            return preis / 12.0;
        }
        return preis;
    }

    /**
     * Berechnet das nächste Laufzeitende ausgehend vom Startdatum.
     * Das Abo verlängert sich immer um laufzeitMonate.
     */
    @Exclude
    public Date getNaechstesLaufzeitende() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
            sdf.setLenient(false); 
            Date start = sdf.parse(startdatum); 
            Calendar cal = Calendar.getInstance();
            cal.setTime(start); 

            Calendar heute = Calendar.getInstance(); 

            
            int monate = Math.max(laufzeitMonate, 1);

            
            cal.add(Calendar.MONTH, monate); 
            while (cal.before(heute)) {
                cal.add(Calendar.MONTH, monate); 
            }
            return cal.getTime();
        } catch (Exception e) {
           
            return null;
        }
    }

    /**
     * Kündigungsfrist = nächstes Laufzeitende minus Frist in Tagen.
     * Gibt das Datum zurück, bis zu dem gekündigt werden muss.
     */
    @Exclude
    public Date getKuendigungsdatum() {
        Date ende = getNaechstesLaufzeitende();
        if (ende == null) return null; 
        Calendar cal = Calendar.getInstance();
        cal.setTime(ende);
        cal.add(Calendar.DAY_OF_YEAR, -kuendigungsfristTage); 
        return cal.getTime();
    }

    /**
     * Anzahl der Nutzungs-Bestätigungen in den letzten 30 Tagen.
     */
   
    @Exclude
    public int getNutzungenLetzte30Tage() {
        if (nutzungen == null) return 0; 
        long grenze = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000; 
        int count = 0;
        for (Long t : nutzungen) {
            if (t != null && t >= grenze) count++; 
        }
        return count;
    }

    /**
     * Bewertung der Notwendigkeit auf einer Skala von 1 bis 5,
     * basierend auf der Nutzungshäufigkeit der letzten 30 Tage.
     * 1 = sehr inaktiv, 5 = sehr aktiv.
     */
    @Exclude
    public int getNutzungsBewertung() {
        int n = getNutzungenLetzte30Tage();
        if (n == 0) return 1;
        if (n <= 2) return 2;
        if (n <= 5) return 3;
        if (n <= 10) return 4;
        return 5;
    }

    
    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getKategorie() { return kategorie; }
    public void setKategorie(String kategorie) { this.kategorie = kategorie; }

    public double getPreis() { return preis; }
    public void setPreis(double preis) { this.preis = preis; }

    public String getIntervall() { return intervall; }
    public void setIntervall(String intervall) { this.intervall = intervall; }

    public String getStartdatum() { return startdatum; }
    public void setStartdatum(String startdatum) { this.startdatum = startdatum; }

    public int getLaufzeitMonate() { return laufzeitMonate; }
    public void setLaufzeitMonate(int laufzeitMonate) { this.laufzeitMonate = laufzeitMonate; }

    public int getKuendigungsfristTage() { return kuendigungsfristTage; }
    public void setKuendigungsfristTage(int kuendigungsfristTage) { this.kuendigungsfristTage = kuendigungsfristTage; }

    public String getBedingungen() { return bedingungen; }
    public void setBedingungen(String bedingungen) { this.bedingungen = bedingungen; }

  
    public List<Long> getNutzungen() {
        if (nutzungen == null) nutzungen = new ArrayList<>();
        return nutzungen;
    }
    public void setNutzungen(List<Long> nutzungen) { this.nutzungen = nutzungen; }
}
