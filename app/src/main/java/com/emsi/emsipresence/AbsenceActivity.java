package com.emsi.emsipresence;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class AbsenceActivity extends AppCompatActivity {

    private Spinner spinnerCampus, spinnerFiliere, spinnerAnnee, spinnerGroupe, spinnerMatiere;
    private Button btnDate, btnSave;
    private LinearLayout containerEtudiants;
    private String selectedDate = "";
    private FirebaseFirestore db;
    private List<Etudiant> etudiants = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_absence);

        db = FirebaseFirestore.getInstance();
        initViews();
        setupSpinners();
        setupListeners();
    }

    private void initViews() {
        spinnerCampus = findViewById(R.id.spinner_campus);
        spinnerFiliere = findViewById(R.id.spinner_filiere);
        spinnerAnnee = findViewById(R.id.spinner_annee);
        spinnerGroupe = findViewById(R.id.spinner_groupe);
        spinnerMatiere = findViewById(R.id.spinner_matiere);
        btnDate = findViewById(R.id.btn_date);
        btnSave = findViewById(R.id.btn_save);
        containerEtudiants = findViewById(R.id.container_etudiants);
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
        List<String> filieres = Arrays.asList(
                "Sélectionner une filière",
                "Génie Informatique",
                "Génie Industriel",
                "Génie Civil",
                "Génie Électrique"
        );
        setupSpinner(spinnerFiliere, filieres);

        // Spinner Filière → Annee
        spinnerFiliere.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    List<String> annees = Arrays.asList(
                            "Sélectionner une année",
                            "1ère année", "2ème année", "3ème année", "4ème année", "5ème année"
                    );
                    setupSpinner(spinnerAnnee, annees);
                } else {
                    resetSpinner(spinnerAnnee);
                    resetSpinner(spinnerGroupe);
                    resetSpinner(spinnerMatiere);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Spinner Annee → Groupe
        spinnerAnnee.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    List<String> groupes = new ArrayList<>();
                    groupes.add("Sélectionner un groupe");
                    for (int i = 1; i <= 10; i++) groupes.add("Groupe " + i);
                    setupSpinner(spinnerGroupe, groupes);
                } else {
                    resetSpinner(spinnerGroupe);
                    resetSpinner(spinnerMatiere);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Spinner Groupe → Matières et étudiants
        spinnerGroupe.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    chargerMatieres();
                    chargerEtudiants();
                } else {
                    resetSpinner(spinnerMatiere);
                    containerEtudiants.removeAllViews();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void chargerMatieres() {
        String filiere = spinnerFiliere.getSelectedItem().toString();
        List<String> matieres = new ArrayList<>();
        matieres.add("Sélectionner une matière");

        // Liste statique des matières pour chaque filière
        switch (filiere) {
            case "Génie Informatique":
                matieres.addAll(Arrays.asList("Java", "C++", "Python", "Systèmes d'exploitation", "Bases de données"));
                break;
            case "Génie Industriel":
                matieres.addAll(Arrays.asList("Logistique", "Management industriel", "Qualité", "Production"));
                break;
            case "Génie Civil":
                matieres.addAll(Arrays.asList("Résistance des matériaux", "Béton armé", "Topographie", "Structures"));
                break;
            case "Génie Électrique":
                matieres.addAll(Arrays.asList("Électronique", "Machines électriques", "Automatisme", "Électrotechnique"));
                break;
            default:
                // Si aucune filière sélectionnée ou inconnue
                setupSpinner(spinnerMatiere, matieres);
                return;
        }

        // Ajouter les matières récupérées depuis Firestore (si disponibles)
        db.collection("matieres")
                .whereEqualTo("filiere", filiere)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String nom = doc.getString("nom");
                        if (nom != null && !matieres.contains(nom)) {
                            matieres.add(nom);
                        }
                    }
                    setupSpinner(spinnerMatiere, matieres);
                })
                .addOnFailureListener(e -> {
                    // En cas d’échec de Firestore, on affiche quand même les matières statiques
                    setupSpinner(spinnerMatiere, matieres);
                });
    }


    private void chargerEtudiants() {
        String campus = spinnerCampus.getSelectedItem().toString();
        String filiere = spinnerFiliere.getSelectedItem().toString();
        String annee = spinnerAnnee.getSelectedItem().toString();
        String groupe = spinnerGroupe.getSelectedItem().toString();

        db.collection("etudiants")
                .whereEqualTo("campus", campus)
                .whereEqualTo("filiere", filiere)
                .whereEqualTo("annee", annee)
                .whereEqualTo("groupe", groupe)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    etudiants.clear();
                    containerEtudiants.removeAllViews();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Etudiant etudiant = doc.toObject(Etudiant.class);
                        if (etudiant != null) {
                            etudiant.setId(doc.getId());
                            etudiants.add(etudiant);
                            ajouterEtudiantView(etudiant);
                        }
                    }
                });
    }

    private void ajouterEtudiantView(Etudiant etudiant) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_etudiant, containerEtudiants, false);
        TextView tvNom = view.findViewById(R.id.tv_nom);
        RadioButton rbPresent = view.findViewById(R.id.rb_present);
        RadioButton rbAbsent = view.findViewById(R.id.rb_absent);
        EditText etRemarque = view.findViewById(R.id.et_remarque);

        tvNom.setText(etudiant.getNom() + " " + etudiant.getPrenom());
        etudiant.setRbPresent(rbPresent);
        etudiant.setRbAbsent(rbAbsent);
        etudiant.setEtRemarque(etRemarque);

        containerEtudiants.addView(view);
    }

    private void setupListeners() {
        btnDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                btnDate.setText(selectedDate);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnSave.setOnClickListener(v -> {
            if (validerFormulaire()) {
                enregistrerAbsences();
            }
        });
    }

    private boolean validerFormulaire() {
        if (spinnerCampus.getSelectedItemPosition() == 0 ||
                spinnerFiliere.getSelectedItemPosition() == 0 ||
                spinnerAnnee.getSelectedItemPosition() == 0 ||
                spinnerGroupe.getSelectedItemPosition() == 0 ||
                spinnerMatiere.getSelectedItemPosition() == 0 ||
                selectedDate.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void enregistrerAbsences() {
        String campus = spinnerCampus.getSelectedItem().toString();
        String filiere = spinnerFiliere.getSelectedItem().toString();
        String annee = spinnerAnnee.getSelectedItem().toString();
        String groupe = spinnerGroupe.getSelectedItem().toString();
        String matiere = spinnerMatiere.getSelectedItem().toString();

        List<Map<String, Object>> absences = new ArrayList<>();

        for (Etudiant etudiant : etudiants) {
            Map<String, Object> absence = new HashMap<>();
            absence.put("etudiantId", etudiant.getId());
            absence.put("nom", etudiant.getNom());
            absence.put("prenom", etudiant.getPrenom());
            absence.put("present", etudiant.getRbPresent().isChecked());
            absence.put("remarque", etudiant.getEtRemarque().getText().toString());
            absence.put("date", selectedDate);
            absence.put("matiere", matiere);
            absence.put("campus", campus);
            absence.put("filiere", filiere);
            absence.put("annee", annee);
            absence.put("groupe", groupe);
            absences.add(absence);
        }

        db.collection("absences")
                .add(new HashMap<String, Object>() {{
                    put("date", selectedDate);
                    put("matiere", matiere);
                    put("campus", campus);
                    put("filiere", filiere);
                    put("annee", annee);
                    put("groupe", groupe);
                    put("absences", absences);
                }})
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Absences enregistrées avec succès!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSpinner(Spinner spinner, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void resetSpinner(Spinner spinner) {
        List<String> emptyList = Collections.singletonList("Sélectionner une option");
        setupSpinner(spinner, emptyList);
    }
}
