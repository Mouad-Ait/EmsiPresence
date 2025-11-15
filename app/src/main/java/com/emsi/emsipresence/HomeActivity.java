package com.emsi.emsipresence;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private static final int GALLERY_REQUEST_CODE = 123;

    private ImageView profileImage;
    private TextView dashboardAdminName;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        profileImage = findViewById(R.id.profileImage);
        dashboardAdminName = findViewById(R.id.dashboard_adminName);

        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String email = currentUser.getEmail();
            String displayName = currentUser.getDisplayName();

            if (displayName != null && !displayName.isEmpty()) {
                dashboardAdminName.setText("Bienvenue, " + displayName);
            } else if (email != null && !email.isEmpty()) {
                dashboardAdminName.setText("Bienvenue, " + email.split("@")[0]);
            } else {
                dashboardAdminName.setText("Bienvenue !");
            }

            // Chargement de la photo de profil
            db.collection("Users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String photoUrl = documentSnapshot.getString("photoUrl");
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Picasso.get().load(photoUrl).into(profileImage);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erreur Firestore: ", e);
                        Toast.makeText(this, "Erreur de chargement du profil", Toast.LENGTH_SHORT).show();
                    });
        } else {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        }

        // ✅ Nouvelle méthode native : cliquer sur l’image pour ouvrir la galerie
        profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, GALLERY_REQUEST_CODE);
        });

        setupCardClickListeners();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                uploadProfileImage(selectedImageUri);
            }
        }
    }

    private void uploadProfileImage(Uri imageUri) {
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        StorageReference ref = storage.getReference().child("profile_images/" + userId + ".jpg");

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();

                    // Mise à jour dans Firestore
                    db.collection("Users").document(userId)
                            .update("photoUrl", imageUrl)
                            .addOnSuccessListener(aVoid -> {
                                Picasso.get().load(imageUrl).into(profileImage);
                                Toast.makeText(this, "Photo mise à jour !", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Erreur Firestore update: ", e);
                                Toast.makeText(this, "Échec mise à jour Firestore", Toast.LENGTH_SHORT).show();
                            });
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur Upload: ", e);
                    Toast.makeText(this, "Échec du téléchargement", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupCardClickListeners() {
        if (findViewById(R.id.documents) != null) {
            findViewById(R.id.documents).setOnClickListener(v ->
                    startActivity(new Intent(this, DocumentActivity.class)));
        }

        if (findViewById(R.id.absence) != null) {
            findViewById(R.id.absence).setOnClickListener(v ->
                    startActivity(new Intent(this, AbsenceActivity.class)));
        }

        if (findViewById(R.id.rattrapage) != null) {
            findViewById(R.id.rattrapage).setOnClickListener(v ->
                    startActivity(new Intent(this, RattrapageActivity.class)));
        }
        if (findViewById(R.id.maps) != null) {
            findViewById(R.id.maps).setOnClickListener(v ->
                    startActivity(new Intent(this, LocalisationActivity.class)));

            if (findViewById(R.id.AI) != null) {
                findViewById(R.id.AI).setOnClickListener(v ->
                        startActivity(new Intent(this, AssistantActivity.class)));
            }

            if (findViewById(R.id.planning) != null) {
                findViewById(R.id.planning).setOnClickListener(v ->
                        startActivity(new Intent(this, EmploiDuTempsActivity.class)));
            }
        }
    }
}
