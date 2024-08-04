package com.example.nuru;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.nuru.databinding.ActivityHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    ActivityHomeBinding binding;

    private ImageView profileImage;
    private TextView dynamicText;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private ActivityResultLauncher<Intent> profileActivityLauncher;

    private ListenerRegistration userSnapshotListener;

    @Override
    protected void onResume() {
        super.onResume();
        loadLocale();
        updateUI();
        loadProfileImage();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        profileImage = findViewById(R.id.profile_image);
        dynamicText = binding.dynamicText;
        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadLocale();


        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Log.d("HomeActivity", "Settings icon clicked. Starting Profile activity.");
                    Intent intent = new Intent(HomeActivity.this, Profile.class);
                    startActivity(intent);

                } catch (Exception e) {
                    Log.e("HomeActivity", "Failed to open profile", e);
                    Toast.makeText(HomeActivity.this, "Failed to open profile", Toast.LENGTH_SHORT).show();
                }

            }
        });



        //        Load Initial fragment
        String defaultTitle = getCurrentDateTitle();
        replaceFragment(new HomeFragment(), defaultTitle);

//        Load profile image
        loadProfileImage();

        // Setup Firestore listener
        setupProfilePicListener();

        binding.bottomnavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            String title= "";
            if (itemId == R.id.homepage) {
                replaceFragment(new HomeFragment(), getCurrentDateTitle());
            } else if (itemId == R.id.calendar) {
                replaceFragment(new MoodsFragment(), "Mood" );
            } else if (itemId == R.id.graphs) {
                replaceFragment(new InsightsFragment(),"Insights" );
            } else if (itemId == R.id.resources) {
                replaceFragment(new ResourcesFragment(),"Chat" );
            } else if (itemId == R.id.community) {
                replaceFragment(new CommunityFragment(), "Community Posts");
            }

            return true;
        });

        binding.dynamicText.setText(defaultTitle);

    }

    private String getCurrentDateTitle() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }



    private void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String language = prefs.getString("My_Lang", "en");
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    private void updateUI() {
    }

    private void setupProfilePicListener() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            userSnapshotListener = db.collection("users")
                    .document(firebaseUser.getUid())
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null) {
                            Log.e("HomeActivity", "Error listening for profile pic update", error);
                            return;
                        }
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String profilePicUrl = documentSnapshot.getString("ProfilePicUrl");
                            if (profilePicUrl != null) {
                                Glide.with(HomeActivity.this)
                                        .load(profilePicUrl)
                                        .placeholder(R.drawable.profile_pic)
                                        .into(profileImage);
                            } else {
                                profileImage.setImageResource(R.drawable.profile_pic);
                            }
                        }
                    });
        }
    }

    private void replaceFragment(Fragment fragment, String title){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();

        binding.dynamicText.setText(title);
    }

    private void loadProfileImage() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            db.collection("users").document(firebaseUser.getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    String profilePicUrl = document.getString("ProfilePicUrl");
                    if (profilePicUrl != null) {
                        Glide.with(HomeActivity.this)
                                .load(profilePicUrl)
                                .placeholder(R.drawable.profile_pic)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.profile_pic);
                    }

                } else {
                    Toast.makeText(HomeActivity.this, "Failed to load profile pic", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        // Remove Firestore listener
        if (userSnapshotListener != null) {
            userSnapshotListener.remove();
        }
    }


}
