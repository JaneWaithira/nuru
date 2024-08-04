package com.example.nuru;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Locale;

public class Profile extends AppCompatActivity {

    private ImageView profilePicture;
    private ImageView editIcon;
    private ImageView backArrow;
    private TextView userNameText;
    private TextView emailText;

    private RadioGroup languageRadioGroup;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private StorageReference storageReference;

    private ActivityResultLauncher<Intent> activityResultLauncher;

    private String currentLocale;
    private ProfileViewModel profileViewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Log.d("Profile", "Profile activity started.");

        profilePicture = findViewById(R.id.profile_picture);
        editIcon = findViewById(R.id.edit_icon);
        backArrow = findViewById(R.id.back_arrow);
        userNameText = findViewById(R.id.username);
        emailText = findViewById(R.id.email);
        AppCompatButton editInfoButton = findViewById(R.id.edit_info_btn);
        languageRadioGroup = findViewById(R.id.language_settings);
        TextView signOutView = findViewById(R.id.signout);
        TextView deleteView = findViewById(R.id.delete);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("profile pictures");

        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        currentLocale = prefs.getString("My_Lang", "en");
        setLocale(currentLocale);

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if(firebaseUser != null) {
            db.collection("users").document(firebaseUser.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        userNameText.setText(document.getString("username"));
                        emailText.setText(document.getString("email"));
                        String profilePicUrl = document.getString("ProfilePicUrl");
                        if (profilePicUrl != null) {
                            if (!isFinishing() && !isDestroyed()) {
                                Glide.with(Profile.this).load(profilePicUrl).into(profilePicture);
                            }

                        } else {
                            //set default picture
                            profilePicture.setImageResource(R.drawable.profile_pic);
                        }
                    } else {
                        Log.e(TAG, "Error getting documents: ", task.getException());
                        Toast.makeText(Profile.this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Failed to get profile data", e);
                }
            });
        }



        backArrow.setOnClickListener(v -> finish());

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result ->{
                        if (result.getResultCode() == RESULT_OK && result.getData() != null){
                            Uri imageUri = result.getData().getData();
                            uploadImageToFirebase(imageUri);
                        }

                });


        View.OnClickListener selectPhotoListener = v -> openImagePicker();
        profilePicture.setOnClickListener(selectPhotoListener);
        editIcon.setOnClickListener(selectPhotoListener);

        editInfoButton.setOnClickListener(v -> {
            EditProfile dialogFragment = new EditProfile();
            dialogFragment.show(getSupportFragmentManager(), "EditProfile");
        });

        //Language change Listener
        languageRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String newLocale = checkedId == R.id.language_1 ? "en" : "sw";
            if (!newLocale.equals(currentLocale)) {
                currentLocale = newLocale;
                setLocale(newLocale);
                recreate();
            }
        });

//        On click listeners for sign out and delete
        signOutView.setOnClickListener(v -> showSignOutDialog());
        deleteView.setOnClickListener(v -> showDeleteAccountDialog());


    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete you account? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> deleteAcount())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteAcount() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.delete().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            Toast.makeText(Profile.this, "Account deleted successsfully", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(Profile.this, RegisterActivity.class));
                            finish();
                        } else {
                            Toast.makeText(Profile.this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(Profile.this, "Failed to delete user data", Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Yes", (dialog, which) -> signOut())
                .setNegativeButton("No", null)
                .show();
    }

    private void signOut() {
        firebaseAuth.signOut();
        startActivity(new Intent(Profile.this, Login.class));
        finish();
    }


    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activityResultLauncher.launch(intent);
    }

    private void uploadImageToFirebase(Uri imageUri) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            StorageReference fileRef = storageReference.child(firebaseUser.getUid() + ".jpg");
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            db.collection("users").document(firebaseUser.getUid())
                                    .update("ProfilePicUrl", imageUrl)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(Profile.this, "Profile Picture updated", Toast.LENGTH_SHORT).show();
                                                Glide.with(Profile.this)
                                                        .load(imageUrl)
                                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                        .skipMemoryCache(true)
                                                        .into(profilePicture);
                                            } else {
                                                Log.e(TAG, "Failed to update profile picture", task.getException());
                                                Toast.makeText(Profile.this, "Failed to Update profile picture", Toast.LENGTH_SHORT).show();
                                             }
                                        }
                                    });
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload image to Firebase Storage", e);
                        Toast.makeText(Profile.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    });
        }
    }




    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
        editor.putString("My_Lang", lang);
        editor.apply();

    }



}