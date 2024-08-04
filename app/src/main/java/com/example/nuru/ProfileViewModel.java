package com.example.nuru;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;


public class ProfileViewModel extends ViewModel {
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private MutableLiveData<User> userLiveData;
    private ListenerRegistration listenerRegistration;

    public ProfileViewModel() {
        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userLiveData = new MutableLiveData<>();
        fetchUserData();
    }

    public LiveData<User> getUser() {
        return userLiveData;
    }

    private void fetchUserData() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            db.collection("users").document(firebaseUser.getUid()).addSnapshotListener((documentSnapshot, error) -> {
                if (error != null) {
                    // Handle error
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        userLiveData.setValue(user);
                    }
                }
            });
        }
    }

    protected void onCleared() {
        super.onCleared();
        // Remove Firestore listener when ViewModel is cleared
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    public void updateProfilePictureUrl(String url, Profile context) {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            db.collection("users").document(firebaseUser.getUid()).update("ProfilePicUrl", url)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(context, "Profile Picture updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e(TAG, "Failed to update profile picture", task.getException());
                                Toast.makeText(context, "Failed to Update profile picture", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Failed to update profile picture", e);
                        }
                    });
        }
    }




}
