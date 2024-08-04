package com.example.nuru;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Login extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long TIME_FRAME = 5 * 60 * 1000; // 5 minutes in milliseconds

    private TextInputEditText mloginemail;
    private TextInputEditText mloginpassword;
    private Button mloginbutton;
    private TextView mforgotpassword;
    private TextView mregisterpage;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.login);

        mloginemail = findViewById(R.id.email);
        mloginpassword = findViewById(R.id.password);
        mloginbutton = findViewById(R.id.btn_login);
        mforgotpassword = findViewById(R.id.password_reset);
        mregisterpage = findViewById(R.id.register_page);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        mregisterpage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Register page clicked");
                startActivity(new Intent(Login.this, RegisterActivity.class));
            }
        });

        mforgotpassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Forgot password clicked");
                startActivity(new Intent(Login.this, ForgotPassword.class));
            }
        });

        mloginbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Login button clicked");
                String mail = Objects.requireNonNull(mloginemail.getText()).toString().trim();
                String password = Objects.requireNonNull(mloginpassword.getText()).toString().trim();

                if (mail.isEmpty() || password.isEmpty()) {
                    Log.d(TAG, "Fields are empty");
                    Toast.makeText(Login.this, "All fields are required", Toast.LENGTH_SHORT).show();
                } else if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
                    Log.d(TAG, "Invalid email address: " + mail);
                    Toast.makeText(Login.this, "Invalid email address", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "Email and password validation passed");
                    checkRateLimit(mail, new RateLimitCallback() {
                        @Override
                        public void onComplete(boolean allowed) {
                            if (allowed) {
                                Log.d(TAG, "Login attempt allowed");
                                firebaseAuth.signInWithEmailAndPassword(mail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "Firebase authentication successful");
                                            resetFailedAttempts(mail);
                                            checkMailVerification();
                                        } else {
                                            Log.d(TAG, "Firebase authentication failed: " + task.getException().getMessage());
                                            incrementFailedAttempts(mail);
                                            Toast.makeText(Login.this, "Invalid credentials, try again", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            } else {
                                Log.d(TAG, "Login attempt not allowed");
                                Toast.makeText(Login.this, "Too many failed attempts. Please try again later.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }

    private void checkRateLimit(String email, final RateLimitCallback callback) {
        db.collection("loginAttempts").document(email).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Long lastAttemptTime = document.getLong("lastAttemptTime");
                        Long failedAttempts = document.getLong("failedAttempts");

                        if (lastAttemptTime != null && failedAttempts != null) {
                            long currentTime = System.currentTimeMillis();

                            if (currentTime - lastAttemptTime < TIME_FRAME && failedAttempts >= MAX_FAILED_ATTEMPTS) {
                                callback.onComplete(false);
                            } else {
                                callback.onComplete(true);
                            }
                        } else {
                            callback.onComplete(true);
                        }
                    } else {
                        callback.onComplete(true);
                    }
                } else {
                    callback.onComplete(true); // Allow login if check fails
                }
            }
        });
    }

    private void incrementFailedAttempts(String email) {
        db.collection("loginAttempts").document(email).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    long currentTime = System.currentTimeMillis();
                    Map<String, Object> data = new HashMap<>();
                    data.put("lastAttemptTime", currentTime);

                    if (document.exists()) {
                        Long failedAttempts = document.getLong("failedAttempts");
                        if (failedAttempts != null) {
                            data.put("failedAttempts", failedAttempts + 1);
                        } else {
                            data.put("failedAttempts", 1);
                        }
                    } else {
                        data.put("failedAttempts", 1);
                    }

                    db.collection("loginAttempts").document(email).set(data, SetOptions.merge());
                }
            }
        });
    }

    private void resetFailedAttempts(String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("failedAttempts", 0);
        db.collection("loginAttempts").document(email).set(data, SetOptions.merge());
    }

    private void checkMailVerification() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null && firebaseUser.isEmailVerified()) {
            Log.d(TAG, "Email verified");
            Toast.makeText(getApplicationContext(), "Successfully logged in", Toast.LENGTH_SHORT).show();
            checkUserRole(firebaseUser);
        } else {
            Log.d(TAG, "Email not verified or user is null");
            Toast.makeText(getApplicationContext(), "Kindly verify Email to continue", Toast.LENGTH_SHORT).show();
            firebaseAuth.signOut();
        }
    }

    private void navigateToHome() {
        Log.d(TAG, "Navigating to HomeActivity");
        finish();
        startActivity(new Intent(Login.this, HomeActivity.class));
    }

    private void checkUserRole(FirebaseUser user) {
        Log.d(TAG, "Checking user role for UID: " + user.getUid());

        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String role = document.getString("role");
                                if ("admin".equals(role)) {
                                    navigateToAdminHome();
                                } else {
                                    navigateToHome();
                                }
                            } else {
                                navigateToHome();
                            }
                        } else {
                            Toast.makeText(Login.this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
                            navigateToHome();
                        }
                    }
                });
    }

    private void navigateToAdminHome() {
        finish();
        startActivity(new Intent(Login.this, AdminHomeActivity.class));
    }

    // Custom callback interface
    private interface RateLimitCallback {
        void onComplete(boolean allowed);
    }
}
