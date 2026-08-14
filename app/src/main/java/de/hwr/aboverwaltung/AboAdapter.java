package de.hwr.aboverwaltung;

import android.view.LayoutInflater;   
import android.view.View;             
import android.view.ViewGroup;        
import android.widget.Button;       
import android.widget.TextView;    
import androidx.annotation.NonNull;             
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;    
import java.util.Locale;  

/**
 * Adapter für die Abo-Liste in der Übersicht.
 * Zeigt pro Abo die wichtigsten Details und den "Genutzt"-Button.
 */

public class AboAdapter extends RecyclerView.Adapter<AboAdapter.AboViewHolder> {

    /**
     * Interface, damit die Activity auf Klicks reagieren kann
     * (Observer-Pattern, wie beim OnClickListener im Tutorial).
     */
    
    public interface AboListener {
        void onAboClick(Abo abo);      
        void onGenutztClick(Abo abo);  
    }

    
    private final List<Abo> abos;        
    private final AboListener listener;  

    public AboAdapter(List<Abo> abos, AboListener listener) {
        this.abos = abos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AboViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_abo, parent, false); 
        return new AboViewHolder(view); 
    }

    @Override
    public void onBindViewHolder(@NonNull AboViewHolder holder, int position) {
        final Abo abo = abos.get(position); 

        holder.textName.setText(abo.getName());             
        holder.textKategorie.setText(abo.getKategorie());   
        holder.textPreis.setText(String.format(Locale.GERMANY,
                "%.2f € / %s", abo.getPreis(), abo.getIntervall()));
        
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onAboClick(abo); 
            }
        });

        holder.buttonGenutzt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onGenutztClick(abo); 
            }
        });
    }

    @Override
    public int getItemCount() {
        return abos.size();
    }

    static class AboViewHolder extends RecyclerView.ViewHolder {
        TextView textName;        
        TextView textKategorie;   
        TextView textPreis;       
        Button buttonGenutzt;     

        AboViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.itemTextName);
            textKategorie = itemView.findViewById(R.id.itemTextKategorie);
            textPreis = itemView.findViewById(R.id.itemTextPreis);
            buttonGenutzt = itemView.findViewById(R.id.itemButtonGenutzt);
        }
    }
}
