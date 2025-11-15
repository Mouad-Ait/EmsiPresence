package com.emsi.emsipresence;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;

public class AssistantActivity extends AppCompatActivity {

    private static final int MAX_MESSAGES = 100; // Limit message history
    private static final String GEMINI_MODEL = "gemini-pro";

    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ProgressBar progressBar;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messages = new ArrayList<>();
    private GenerativeModelFutures model;
    private boolean isModelInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant);

        initializeUI();
        initializeGemini();
        displayWelcomeMessage();
    }

    private void initializeUI() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        progressBar = findViewById(R.id.progressBar);

        // Configure RecyclerView
        chatAdapter = new ChatAdapter(messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        // Set up send button
        findViewById(R.id.sendButton).setOnClickListener(v -> handleSendMessage());

        // Set up keyboard enter key listener
        messageEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {
                handleSendMessage();
                return true;
            }
            return false;
        });
    }

    private void initializeGemini() {
        try {
            // Get API key from BuildConfig or resources - NEVER hardcode it
            String apiKey = getApiKey();

            if (apiKey == null || apiKey.isEmpty()) {
                showToast("Configuration manquante pour l'assistant IA");
                finish();
                return;
            }

            GenerativeModel gm = new GenerativeModel(GEMINI_MODEL, apiKey);
            model = GenerativeModelFutures.from(gm);
            isModelInitialized = true;

        } catch (Exception e) {
            showToast("Erreur d'initialisation de l'assistant IA");
            e.printStackTrace();
            finish();
        }
    }

    private String getApiKey() {
        try {
            return BuildConfig.GEMINI_API_KEY;
        } catch (Exception e) {
            try {
                return getString(R.string.gemini_api_key);
            } catch (Exception ex) {
                return null;
            }
        }
    }
    private void displayWelcomeMessage() {
        String welcomeMsg = "Bonjour ! Je suis l'assistant EMSI pour la gestion des présences.\n\n" +
                "Je peux vous aider avec :\n" +
                "• Suivi des présences et absences\n" +
                "• Questions sur les cours\n" +
                "• Gestion administrative\n\n" +
                "Comment puis-je vous aider aujourd'hui ?";

        addMessageToChat(welcomeMsg, false);
    }

    private void handleSendMessage() {
        String userMessage = messageEditText.getText().toString().trim();
        if (userMessage.isEmpty()) {
            return;
        }

        if (!isModelInitialized) {
            showToast("Assistant non disponible. Veuillez redémarrer l'application.");
            return;
        }

        addMessageToChat(userMessage, true);
        messageEditText.setText("");
        processUserMessage(userMessage);
    }

    private void processUserMessage(String message) {
        showLoading(true);

        String systemPrompt = buildSystemPrompt();
        Content content = new Content.Builder()
                .addText(systemPrompt + "\n\nQuestion de l'utilisateur: " + message)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    try {
                        String responseText = result.getText();
                        if (responseText != null && !responseText.trim().isEmpty()) {
                            addMessageToChat(responseText.trim(), false);
                        } else {
                            addMessageToChat("Je n'ai pas pu générer de réponse. Veuillez reformuler votre question.", false);
                        }
                    } catch (Exception e) {
                        addMessageToChat("Erreur lors du traitement de la réponse.", false);
                        e.printStackTrace();
                    } finally {
                        showLoading(false);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    String errorMsg = "Désolé, je rencontre des difficultés techniques. ";
                    if (t.getMessage() != null && t.getMessage().contains("network")) {
                        errorMsg += "Vérifiez votre connexion internet.";
                    } else {
                        errorMsg += "Veuillez réessayer dans quelques instants.";
                    }
                    addMessageToChat(errorMsg, false);
                    showLoading(false);
                });
                t.printStackTrace();
            }
        }, getMainExecutor());
    }

    private String buildSystemPrompt() {
        return "Tu es l'assistant officiel de l'EMSI (École Marocaine des Sciences de l'Ingénieur). " +
                "Tu es spécialisé dans l'aide à la gestion des présences et au suivi académique.\n\n" +
                "Tes domaines d'expertise :\n" +
                "- Gestion des présences et absences des étudiants\n" +
                "- Suivi et reporting académique\n" +
                "- Procédures administratives liées aux cours\n" +
                "- Réglementations de l'école concernant l'assiduité\n" +
                "- Outils et systèmes de gestion des présences\n\n" +
                "Instructions :\n" +
                "- Réponds UNIQUEMENT aux questions liées à tes domaines d'expertise\n" +
                "- Sois concis, précis et professionnel\n" +
                "- Utilise un français formel mais accessible\n" +
                "- Si la question sort de ton domaine, redirige poliment vers tes spécialités\n" +
                "- Propose des solutions pratiques et actionables";
    }

    private void addMessageToChat(String message, boolean isUser) {
        // Manage memory by limiting message history
        if (messages.size() >= MAX_MESSAGES) {
            messages.remove(0);
            chatAdapter.notifyItemRemoved(0);
        }

        messages.add(new ChatMessage(message, isUser));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        scrollChatToBottom();
    }

    private void scrollChatToBottom() {
        if (messages.size() > 0) {
            chatRecyclerView.postDelayed(() -> {
                chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
            }, 100);
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        findViewById(R.id.sendButton).setEnabled(!isLoading);
        messageEditText.setEnabled(!isLoading);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (messages != null) {
            messages.clear();
        }
    }
}