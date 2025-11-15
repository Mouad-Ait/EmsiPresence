package com.emsi.emsipresence;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class DocumentActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 1001;
    private static final int STORAGE_PERMISSION_CODE = 1002;
    private static final int MANAGE_STORAGE_REQUEST_CODE = 1003;
    private Uri selectedFileUri;
    private String selectedFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document);

        Button btnSelectFile = findViewById(R.id.btn_select_file);
        Button btnUpload = findViewById(R.id.btn_upload);
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        TextView tvStatus = findViewById(R.id.tv_status);

        btnSelectFile.setOnClickListener(v -> checkAndRequestPermissions());

        btnUpload.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                uploadFile(selectedFileUri);
            } else {
                Toast.makeText(this, "Aucun fichier sélectionné", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndRequestPermissions() {
        openFilePicker();
    }

    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showManageStorageDialog();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showPermissionRationaleDialog();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            }
        }
    }
    private void openFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, getMimeType(file.getPath()));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Aucune application pour ouvrir ce fichier", Toast.LENGTH_SHORT).show();
        }
    }
    private String getMimeType(String path) {
        String extension = path.substring(path.lastIndexOf(".") + 1);
        switch (extension.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default:
                return "*/*";
        }
    }
    private void showManageStorageDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Accès au stockage nécessaire")
                .setMessage("Cette application a besoin d'un accès complet pour gérer vos fichiers")
                .setPositiveButton("Paramètres", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
                    } catch (Exception e) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission nécessaire")
                .setMessage("Cette application a besoin d'accéder à votre stockage pour lire les fichiers")
                .setPositiveButton("OK", (dialog, which) ->
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                STORAGE_PERMISSION_CODE))
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {
                Toast.makeText(this,
                        "Permission refusée - Impossible d'accéder aux fichiers",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MANAGE_STORAGE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    Environment.isExternalStorageManager()) {
                openFilePicker();
            }
        } else if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            selectedFileName = getFileName(selectedFileUri);

            TextView tvSelectedFile = findViewById(R.id.tv_selected_file);
            tvSelectedFile.setText("Fichier sélectionné : " + selectedFileName);

            Button btnUpload = findViewById(R.id.btn_upload);
            btnUpload.setEnabled(true);
        }
    }
    private void debugPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean hasManageStorage = Environment.isExternalStorageManager();
            Log.d("PERM_DEBUG", "MANAGE_EXTERNAL_STORAGE granted: " + hasManageStorage);
        } else {
            int readStorage = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            Log.d("PERM_DEBUG", "READ_EXTERNAL_STORAGE granted: " +
                    (readStorage == PackageManager.PERMISSION_GRANTED));
        }
    }
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
    private Uri getMyFilesUri() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        } else {
            return Uri.fromFile(Environment.getExternalStorageDirectory());
        }
    }
    private void openFilePicker() {
        // Créer un intent pour ouvrir les documents
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Tous les types de fichiers
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Essayer d'ouvrir directement le stockage interne
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                    Uri.parse(Environment.getExternalStorageDirectory().getPath()));
        }

        // Lancer l'activité
        try {
            startActivityForResult(intent, PICK_FILE_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Aucune application pour gérer les fichiers", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadFile(Uri fileUri) {
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        TextView tvStatus = findViewById(R.id.tv_status);
        TextView tvProgressPercent = findViewById(R.id.tv_progress_percent);
        Button btnUpload = findViewById(R.id.btn_upload);
        LinearLayout uploadStatusLayout = findViewById(R.id.upload_status_layout);

        btnUpload.setEnabled(false);
        uploadStatusLayout.setVisibility(View.VISIBLE);
        tvStatus.setText("Préparation de l'import...");
        progressBar.setProgress(0);
        tvProgressPercent.setText("0%");

        new Thread(() -> {
            try {
                ContentResolver contentResolver = getContentResolver();

                try (InputStream inputStream = contentResolver.openInputStream(fileUri)) {
                    if (inputStream == null) {
                        throw new Exception("Impossible d'ouvrir le fichier");
                    }

                    File downloadsDir = new File(getExternalFilesDir(null), "EMSI_Documents");
                    if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                        throw new Exception("Impossible de créer le dossier de destination");
                    }

                    File outputFile = new File(downloadsDir, selectedFileName);
                    long fileSize = contentResolver.openFileDescriptor(fileUri, "r").getStatSize();

                    try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        long totalBytes = 0;

                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                            totalBytes += length;

                            final int progress = (int) ((totalBytes * 100) / fileSize);
                            runOnUiThread(() -> {
                                progressBar.setProgress(progress);
                                tvProgressPercent.setText(progress + "%");
                                tvStatus.setText("Importation... " + progress + "%");
                            });
                        }
                    }

                    runOnUiThread(() -> {
                        progressBar.setProgress(100);
                        tvStatus.setText("Importation réussie !");
                        Toast.makeText(this,
                                "Fichier sauvegardé dans : " + downloadsDir.getAbsolutePath(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                Log.e("DocumentActivity", "Erreur d'import", e);
                runOnUiThread(() -> {
                    tvStatus.setText("Échec de l'importation");
                    Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } finally {
                runOnUiThread(() -> btnUpload.setEnabled(true));
            }
        }).start();
    }
}