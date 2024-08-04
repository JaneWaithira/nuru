package com.example.nuru;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private OnboardingAdapter onboardingAdapter;
    private LinearLayout layoutOnboardingIndicators;
    private Button buttonOnboardingAction;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MainActivity", "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "Layout set");


        layoutOnboardingIndicators = findViewById(R.id.onboaridngIndicators);
        buttonOnboardingAction = findViewById(R.id.buttonOnboarding);

        if (buttonOnboardingAction == null) {
            Log.e("MainActivity", "buttonOnboardingAction is null");
        } else {
            Log.d("MainActivity", "buttonOnboardingAction initialized");
        }

        setupOnboardingItems();
        ViewPager2 onboardingViewPager = findViewById(R.id.onboardingViewPager);
        onboardingViewPager.setAdapter(onboardingAdapter);

        setupOnboardingIndicators();
        setCurrentOnboardingIndicator(0);
        onboardingViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentOnboardingIndicator(position);
            }
        });

        buttonOnboardingAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Go to sign up
                Log.d("MainActivity", "Get Started Button clicked");
                Toast.makeText(MainActivity.this, "Button Clicked", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);

            }
        });
        Log.d("MainActivity", "onCreate finished");
    }

    //Onboarding pages
    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();

        //Page 1
        OnboardingItem itemEmpowering = new OnboardingItem();
        itemEmpowering.setTitle("Empowering Neurodiverse Kenyans.");
        itemEmpowering.setDescription("Track your moods, manage triggers, and connect with a supportive community. Let’s make life easier together!");
        itemEmpowering.setImage(R.drawable.page1);

        //Page 2
        OnboardingItem itemFeatures = new OnboardingItem();
        itemFeatures.setTitle("Log your moods, behaviours, and triggers.");
        itemFeatures.setDescription("Nuru’s smart algorithms predict potential triggers, helping you stay prepared and informed.");
        itemFeatures.setImage(R.drawable.page2);

        //Page 3
        OnboardingItem itemResources = new OnboardingItem();
        itemResources.setTitle("Find culturally relevant information and resources.");
        itemResources.setDescription("Join community discussions to share experiences and support each other on this journey.");
        itemResources.setImage(R.drawable.page3);

        onboardingItems.add(itemEmpowering);
        onboardingItems.add(itemFeatures);
        onboardingItems.add(itemResources);

        onboardingAdapter = new OnboardingAdapter(onboardingItems);

    }

    private void setupOnboardingIndicators(){
        ImageView[] indicators = new ImageView[onboardingAdapter.getItemCount()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(8, 0, 8, 0);
        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(),
                    R.drawable.onboarding_indicator_inactive
            ));
            indicators[i].setLayoutParams(layoutParams);
            layoutOnboardingIndicators.addView(indicators[i]);

        }
    }

    private void setCurrentOnboardingIndicator(int index) {
        int childCount = layoutOnboardingIndicators.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutOnboardingIndicators.getChildAt(i);
            if (i == index) {
                imageView.setImageDrawable(
                        ContextCompat.getDrawable(getApplicationContext(), R.drawable.onboarding_indicator_active)
                );
            } else {
                imageView.setImageDrawable(
                        ContextCompat.getDrawable(getApplicationContext(), R.drawable.onboarding_indicator_inactive)
                );

            }
        }
    }

}