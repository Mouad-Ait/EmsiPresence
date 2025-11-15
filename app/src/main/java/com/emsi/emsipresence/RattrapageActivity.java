package com.emsi.emsipresence;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RattrapageActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView rattrapageTextView;
    private EditText editTextDate, editTextSujet;
    private Button buttonAjouter, buttonFiltrerDate;

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rattrapage);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        rattrapageTextView = findViewById(R.id.rattrapageTextView);
        editTextDate = findViewById(R.id.editTextDate);
        editTextSujet = findViewById(R.id.editTextSujet);
        buttonAjouter = findViewById(R.id.buttonAjouter);
        buttonFiltrerDate = findViewById(R.id.buttonFiltrerDate);

        afficherRattrapages(); // Affichage des séances au lancement

        // DatePicker pour ajout
        editTextDate.setOnClickListener(v -> showDatePicker(editTextDate));

        // Ajout de séance
        buttonAjouter.setOnClickListener(v -> ajouterRattrapage());

        // Filtrer par date
        buttonFiltrerDate.setOnClickListener(v -> showDatePicker(null));
    }

    private void showDatePicker(EditText targetEditText) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    if (targetEditText != null) {
                        targetEditText.setText(selectedDate);
                    } else {
                        filtrerParDate(selectedDate);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void ajouterRattrapage() {
        String date = editTextDate.getText().toString().trim();
        String sujet = editTextSujet.getText().toString().trim();

        if (date.isEmpty() || sujet.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("dateSeance", date);
        data.put("sujet", sujet);
        data.put("userId", userId);

        db.collection("rattrapages").add(data)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Séance ajoutée avec succès", Toast.LENGTH_SHORT).show();
                    editTextDate.setText("");
                    editTextSujet.setText("");
                    afficherRattrapages(); // Rafraîchir la liste après ajout
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void afficherRattrapages() {
        db.collection("rattrapages")
                .whereEqualTo("userId", userId)
                .orderBy("dateSeance", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    StringBuilder details = new StringBuilder();
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        details.append("📅 ").append(doc.getString("dateSeance"))
                                .append("\n📝 ").append(doc.getString("sujet")).append("\n\n");
                    }
                    rattrapageTextView.setText(details.length() > 0 ? details.toString() : "Aucune séance disponible.");
                })
                .addOnFailureListener(e -> rattrapageTextView.setText("Erreur de chargement."));
    }

    private void filtrerParDate(String selectedDate) {
        db.collection("rattrapages")
                .whereEqualTo("userId", userId)
                .whereEqualTo("dateSeance", selectedDate)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    StringBuilder details = new StringBuilder();
                    for (QueryDocumentSnapshot doc : querySnapshots) {
                        details.append("📅 ").append(doc.getString("dateSeance"))
                                .append("\n📝 ").append(doc.getString("sujet")).append("\n\n");
                    }
                    rattrapageTextView.setText(details.length() > 0 ? details.toString() : "Aucune séance à cette date.");
                })
                .addOnFailureListener(e -> rattrapageTextView.setText("Erreur de filtrage."));
    }
}
