package com.example.nuru;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfile extends DialogFragment {
    private EditText editUsername;
    private EditText editEmail;
    private Button saveButton, cancelButton;


    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_edit_profile, container, false);

        editUsername = view.findViewById(R.id.edit_username);
        editEmail = view.findViewById(R.id.edit_email);
        saveButton = view.findViewById(R.id.save_button);
        cancelButton = view.findViewById(R.id.cancel_button);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // preload user info
        preloadUserInfo();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newUsername = editUsername.getText().toString().trim();
                String newEmail = editEmail.getText().toString().trim();
                updateUserInfo(newUsername, newEmail);
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return view;
    }

    private void preloadUserInfo() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            db.collection("users").document(firebaseUser.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        String currentUsername = document.getString("username");
                        String currentEmail = document.getString("email");

                        if (!TextUtils.isEmpty(currentUsername)) {
                            editUsername.setText(currentUsername);
                        }
                        if (!TextUtils.isEmpty(currentEmail)) {
                            editEmail.setText(currentEmail);
                        }
                    }
                }
            });
        }
    }

    private void updateUserInfo(String newUsername, String newEmail) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            Map<String, Object> updates = new HashMap<>();

            if (!TextUtils.isEmpty(newUsername)) {
                updates.put("username", newUsername);
            }

            if (!TextUtils.isEmpty(newEmail)) {
                updates.put("email", newEmail);
            }
            if (!updates.isEmpty()) {
                db.collection("users").document(firebaseUser.getUid())
                        .update(updates)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getActivity(), "user info updated successfully", Toast.LENGTH_SHORT).show();
                                    dismiss();
                                } else {
                                    Toast.makeText(getActivity(), "Failed to update user info", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } else {
                Toast.makeText(getActivity(), "No changes to update", Toast.LENGTH_SHORT).show();
            }

        }

    }
}