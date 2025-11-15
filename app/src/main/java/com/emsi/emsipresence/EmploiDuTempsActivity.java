package com.emsi.emsipresence;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EmploiDuTempsActivity extends AppCompatActivity {

    private Spinner spinnerCampus, spinnerFiliere, spinnerAnnee, spinnerGroupe;
    private TableLayout tableSchedule;
    private Button btnAfficher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emploi_du_temps);

        // Initialisation des vues
        initViews();

        // Configuration des Spinners
        setupSpinners();

        // Configuration du bouton
        btnAfficher.setOnClickListener(v -> afficherEmploiDuTemps());
    }

    private void initViews() {
        spinnerCampus = findViewById(R.id.spinner_campus);
        spinnerFiliere = findViewById(R.id.spinner_filiere);
        spinnerAnnee = findViewById(R.id.spinner_annee);
        spinnerGroupe = findViewById(R.id.spinner_groupe);
        btnAfficher = findViewById(R.id.btn_afficher);
        tableSchedule = findViewById(R.id.table_schedule);
    }

    private void setupSpinners() {
        // Campus
        List<String> campuses = Arrays.asList(
                "Sélectionner un campus",
                "EMSI Centre (Casablanca)",
                "EMSI Roudani (Casablanca)",
                "EMSI Maarif (Casablanca)"
        );
        setupSpinner(spinnerCampus, campuses);

        // Filières
        spinnerCampus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    List<String> filieres = Arrays.asList(
                            "Sélectionner une filière",
                            "Génie Informatique",
                            "Génie Civil",
                            "Génie Financier",
                            "Génie Industriel"
                    );
                    setupSpinner(spinnerFiliere, filieres);
                } else {
                    resetSpinner(spinnerFiliere);
                    resetSpinner(spinnerAnnee);
                    resetSpinner(spinnerGroupe);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Années
        spinnerFiliere.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    List<String> annees = Arrays.asList(
                            "Sélectionner une année",
                            "1ère année",
                            "2ème année",
                            "3ème année",
                            "4ème année",
                            "5ème année"
                    );
                    setupSpinner(spinnerAnnee, annees);
                } else {
                    resetSpinner(spinnerAnnee);
                    resetSpinner(spinnerGroupe);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Groupes - CORRECTION ICI
        spinnerAnnee.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    // Créer une ArrayList modifiable au lieu d'Arrays.asList()
                    List<String> groupes = new ArrayList<>();
                    groupes.add("Sélectionner un groupe");

                    // Ajouter les groupes (jusqu'à 10)
                    for (int i = 1; i <= 10; i++) {
                        groupes.add("Groupe " + i);
                    }
                    setupSpinner(spinnerGroupe, groupes);
                } else {
                    resetSpinner(spinnerGroupe);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    private void afficherEmploiDepuisFirestore(DocumentSnapshot doc) {
        tableSchedule.removeAllViews();

        // Créer l'en-tête du tableau
        TableRow headerRow = new TableRow(this);
        addHeaderCell(headerRow, "Horaire");
        String[] jours = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"};
        for (String jour : jours) {
            addHeaderCell(headerRow, jour);
        }
        tableSchedule.addView(headerRow);

        String[] creneaux = {"8:30 - 10:00", "10:15 - 11:45", "14:30 - 16:00", "16:15 - 17:45"};

        for (String creneau : creneaux) {
            TableRow row = new TableRow(this);
            addCell(row, creneau);

            for (String jour : jours) {
                String cours = "";
                if (doc.contains(jour)) {
                    Object horaires = doc.get(jour);
                    if (horaires instanceof Map) {
                        Object matiere = ((Map) horaires).get(creneau);
                        cours = matiere != null ? matiere.toString() : "Libre";
                    }
                }
                addCell(row, cours);
            }

            tableSchedule.addView(row);
        }
    }

    private void setupSpinner(Spinner spinner, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                items
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void resetSpinner(Spinner spinner) {
        spinner.setAdapter(null);
    }

    private void afficherEmploiDuTemps() {
        if (spinnerCampus.getSelectedItemPosition() == 0 ||
                spinnerFiliere.getSelectedItemPosition() == 0 ||
                spinnerAnnee.getSelectedItemPosition() == 0 ||
                spinnerGroupe.getSelectedItemPosition() == 0) {

            Toast.makeText(this, "Veuillez sélectionner tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        String campus = spinnerCampus.getSelectedItem().toString();
        String filiere = spinnerFiliere.getSelectedItem().toString();
        String annee = spinnerAnnee.getSelectedItem().toString();
        String groupe = spinnerGroupe.getSelectedItem().toString();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("emploi_du_temps")
                .document(campus)
                .collection(filiere)
                .document(annee)
                .collection(groupe)
                .document("planning")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> planning = documentSnapshot.getData();
                        afficherTableauDynamique(planning);
                    } else {
                        Toast.makeText(this, "Aucune donnée trouvée.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur de récupération des données", Toast.LENGTH_SHORT).show();
                });
    }

    private void afficherTableauDynamique(Map<String, Object> planning) {
        tableSchedule.removeAllViews();

        TableRow headerRow = new TableRow(this);
        addHeaderCell(headerRow, "Horaire");
        String[] jours = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi"};
        for (String jour : jours) {
            addHeaderCell(headerRow, capitalize(jour));
        }
        tableSchedule.addView(headerRow);

        String[] creneaux = {"08:30 - 10:00", "10:15 - 11:45", "12:00 - 14:30", "14:30 - 16:00", "16:15 - 17:45"};

        for (String creneau : creneaux) {
            TableRow row = new TableRow(this);
            addCell(row, creneau);
            for (String jour : jours) {
                if (planning.containsKey(jour)) {
                    Map<String, Object> coursJour = (Map<String, Object>) planning.get(jour);
                    String cours = coursJour.containsKey(creneau) ? coursJour.get(creneau).toString() : "Libre";
                    addCell(row, cours);
                } else {
                    addCell(row, "Libre");
                }
            }
            tableSchedule.addView(row);
        }
    }

    private String capitalize(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
    private void addHeaderCell(TableRow row, String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(16, 12, 16, 12);
        textView.setBackgroundResource(R.drawable.table_header_border);
        textView.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        textView.setTextColor(getResources().getColor(android.R.color.white));
        textView.setGravity(android.view.Gravity.CENTER);

        // Définir une largeur minimum pour les cellules
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        );
        params.weight = 1;
        textView.setLayoutParams(params);

        row.addView(textView);
    }

    private void addCell(TableRow row, String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(16, 12, 16, 12);
        textView.setBackgroundResource(R.drawable.table_cell_border);
        textView.setGravity(android.view.Gravity.CENTER);
        textView.setMinHeight(80); // Hauteur minimum pour les cellules

        // Définir une largeur minimum pour les cellules
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        );
        params.weight = 1;
        textView.setLayoutParams(params);

        row.addView(textView);
    }
}