package com.example.nuru;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText musername;
    private TextInputEditText memail;
    private TextInputEditText mpassword;
    private Button registerbutton;
    private TextInputEditText mconfirmpassword;
    private TextView mgotologin;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;

    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        musername = findViewById(R.id.username);
        memail = findViewById(R.id.email);
        mpassword = findViewById(R.id.password);
        mconfirmpassword = findViewById(R.id.confirmpassword);
        registerbutton = findViewById(R.id.btn_register);
        mgotologin = findViewById(R.id.login_page);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        mgotologin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, Login.class));
            }
        });

        registerbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = musername.getText().toString().trim();
                String mail = memail.getText().toString().trim();
                String password = mpassword.getText().toString().trim();
                String confirmPassword = mconfirmpassword.getText().toString().trim();

                if (username.isEmpty() || mail.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                } else if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
                    Toast.makeText(RegisterActivity.this, "Invalid email address", Toast.LENGTH_SHORT).show();
                } else if (password.length() < 8) {
                    Toast.makeText(RegisterActivity.this, "Password should be at least 8 characters", Toast.LENGTH_SHORT).show();
                } else if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                } else {
                    firebaseAuth.createUserWithEmailAndPassword(mail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                                if (firebaseUser != null) {
                                    firebaseUser.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(RegisterActivity.this, "Registered successfully. Please check your email for verification", Toast.LENGTH_SHORT).show();
                                                saveUserToFirestore(firebaseUser, username, mail);
                                                firebaseAuth.signOut();
                                                finish();
                                                startActivity(new Intent(RegisterActivity.this, Login.class));
                                            } else {
                                                Log.e(TAG, "sendEmailVerification failed: " + task.getException());
                                                Toast.makeText(RegisterActivity.this, "Failed to send verification email", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                } else {
                                    Log.e(TAG, "FirebaseUser is null after successful registration.");
                                    Toast.makeText(RegisterActivity.this, "Registration Failed. Please try again", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                    Toast.makeText(RegisterActivity.this, "An account with this email already exists.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.e(TAG, "createUserWithEmailAndPassword failed: " + task.getException());
                                    Toast.makeText(RegisterActivity.this, "Registration Failed. Please try again", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    private void saveUserToFirestore(FirebaseUser user, String username, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", email);
        userData.put("role", "user");

        db.collection("users").document(user.getUid()).set(userData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "User data saved", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "saveUserToFirestore failed: " + task.getException());
                            Toast.makeText(RegisterActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
