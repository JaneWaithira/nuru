package com.example.nuru;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class ForgotPassword extends AppCompatActivity {
    private EditText mforgotpassword;
    private Button mforgotrecover;
    private TextView mgotologin;

    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mforgotpassword = findViewById(R.id.recoverpass);
        mforgotrecover = findViewById(R.id.recover_button);
        mgotologin = findViewById(R.id.tologin_page);

        firebaseAuth = FirebaseAuth.getInstance();

        mgotologin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ForgotPassword.this, Login.class);
                startActivity(intent);
            }
        });

        mforgotrecover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mail = mforgotpassword.getText().toString().trim();

                if (mail.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Kindly enter email address", Toast.LENGTH_SHORT).show();
                } else if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
                    Toast.makeText(getApplicationContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                } else {
                    // Send password recovery link to registered email
                    sendPasswordResetEmail(mail);
                }
            }
        });
    }

    private void sendPasswordResetEmail(String email) {
        firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "Recovery email sent to registered address", Toast.LENGTH_SHORT).show();
                    finish();
                    startActivity(new Intent(ForgotPassword.this, Login.class));
                } else {
                    String errorMessage = Objects.requireNonNull(task.getException()).getMessage();
                    if (errorMessage != null && errorMessage.contains("no user record")) {
                        Toast.makeText(getApplicationContext(), "No account with this email address", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Failed to send recovery email. Please try again later.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
