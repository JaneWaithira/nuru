package com.example.nuru;

import android.annotation.SuppressLint;

import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ResourcesFragment extends Fragment {

    private EditText userPrompt;
    private Button sendPrompt;
    private LinearLayout chatHistory;
    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;

    private String userName;
    private List<String> conversationHistory;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_resources, container, false);

        userPrompt = view.findViewById(R.id.user_query);
        sendPrompt = view.findViewById(R.id.send_query);
        chatHistory = view.findViewById(R.id.chat_container);
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        conversationHistory = new ArrayList<>();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userDoc = firestore.collection("users").document(userId);

            userDoc.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        userName = document.getString("username");
                        if (userName != null && !userName.trim().isEmpty()) {
                            addWelcomeMessage();
                        }
                        loadConversationHistory();
                    } else {
                        Log.d("Firestore", "No such document");
                    }
                } else {
                    Log.d("Firestore", "get failed with ", task.getException());
                }
            });
        }

        sendPrompt.setOnClickListener(v -> {
            String prompt = userPrompt.getText().toString();
            if (!prompt.trim().isEmpty()) {
                conversationHistory.add("You: " + prompt);
                String fullPrompt = String.join("\n", conversationHistory);

                GenerativeModel gm = new GenerativeModel("gemini-1.5-flash","AIzaSyCR2s2T-Ao-UMAxtza4TlLFPPm6nt30iCs" );
                GenerativeModelFutures model = GenerativeModelFutures.from(gm);
                Content content = new Content.Builder()
                        .addText(fullPrompt)
                        .build();

                ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
                Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        String resultText = result.getText();
                        getActivity().runOnUiThread(() -> {
                            addMessageToChatHistory(prompt, resultText);
                            userPrompt.setText("");
                            storeConversation(prompt, resultText);

                            conversationHistory.add("Nuru: " + resultText);
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        t.printStackTrace();
                        getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }, Executors.newSingleThreadExecutor());
            }
        });

        return view;
    }

    private void addWelcomeMessage() {
        if (userName != null && !userName.trim().isEmpty()) {
            Log.d("ResourcesFragment", "Adding welcome message for user: " + userName);
            TextView welcomeMessageView = new TextView(getActivity());
            welcomeMessageView.setText("Welcome, " + userName + ". How can I help you today?");
            welcomeMessageView.setBackgroundResource(R.drawable.bot_response_bg);
            welcomeMessageView.setTextColor(Color.BLACK);
            LinearLayout.LayoutParams welcomeLayoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            welcomeLayoutParams.setMargins(0, 16, 0, 16);
            welcomeMessageView.setLayoutParams(welcomeLayoutParams);
            chatHistory.addView(welcomeMessageView);

            ScrollView scrollView = getView().findViewById(R.id.chat_scroll_view);
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        } else {
            Log.d("ResourcesFragment", "userName is null or empty");
        }
    }

    private void addMessageToChatHistory(String userMessage, String botResponse) {
        TextView userMessageView = new TextView(getActivity());
        userMessageView.setText("You: " + userMessage);
        userMessageView.setBackgroundResource(R.drawable.user_prompt_bg);
        userMessageView.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams userLayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        userLayoutParams.setMargins(0, 16, 0, 16);
        userMessageView.setLayoutParams(userLayoutParams);
        chatHistory.addView(userMessageView);

        TextView botResponseView = new TextView(getActivity());
        botResponseView.setText("Nuru: " + botResponse);
        botResponseView.setBackgroundResource(R.drawable.bot_response_bg);
        botResponseView.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams botLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        botLayoutParams.setMargins(0, 16, 0, 16);
        botResponseView.setLayoutParams(botLayoutParams);
        chatHistory.addView(botResponseView);

        ScrollView scrollView = getView().findViewById(R.id.chat_scroll_view);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void storeConversation(String userMessage, String botResponse) {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userDoc = firestore.collection("users").document(userId);
            Map<String, Object> conversation = new HashMap<>();
            conversation.put("userMessage", userMessage);
            conversation.put("botResponse", botResponse);
            conversation.put("timestamp", FieldValue.serverTimestamp());
            userDoc.collection("conversations").add(conversation)
                    .addOnSuccessListener(documentReference -> Log.d("Firestore", "Conversation added successfully"))
                    .addOnFailureListener(e -> Log.e("Firestore", "Error adding conversation", e));
        }
    }

    private void loadConversationHistory() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            firestore.collection("users").document(userId).collection("conversations")
                    .orderBy("timestamp", Query.Direction.ASCENDING) // Ensure correct order
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("ResourcesFragment", "Loading conversation history");
                            for (DocumentSnapshot document : task.getResult()) {
                                String userMessage = document.getString("userMessage");
                                String botResponse = document.getString("botResponse");
                                conversationHistory.add("You: " + userMessage);
                                conversationHistory.add("Nuru: " + botResponse);
                                addMessageToChatHistory(userMessage, botResponse);
                            }
                        } else {
                            Log.e("Firestore", "Error getting conversations", task.getException());
                        }
                    });
        } else {
            Log.d("ResourcesFragment", "currentUser is null");
        }
    }
}
