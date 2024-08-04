package com.example.nuru;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminHomeActivity extends AppCompatActivity {

    private ImageView profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        profileImage = findViewById(R.id.profile_image);

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Log.d("HomeActivity", "Settings icon clicked. Starting Profile activity.");
                    Intent intent = new Intent(AdminHomeActivity.this, Profile.class);
                    startActivity(intent);

                } catch (Exception e) {
                    Log.e("HomeActivity", "Failed to open profile", e);
                    Toast.makeText(AdminHomeActivity.this, "Failed to open profile", Toast.LENGTH_SHORT).show();
                }

            }
        });


        BottomNavigationView bottomNav = findViewById(R.id.adminbottomnavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_resources) {
                loadFragment(new AdminResourcesFragment());
                return true;
            } else if (itemId == R.id.nav_community) {
                loadFragment(new AdminCommunityFragment());
                return true;
            }
            return false;
        });
        if (savedInstanceState == null) {
            loadFragment(new AdminResourcesFragment());
        }
    }


    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

}