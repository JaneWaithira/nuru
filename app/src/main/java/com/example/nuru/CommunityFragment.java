package com.example.nuru;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CommunityFragment extends Fragment {
    private FirebaseFirestore db;
    private RecyclerView rvCommunityPosts;
    private List<Map<String, Object>> communityPosts = new ArrayList<>();

    private CommunityPostsAdapter communityPostsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);

        db = FirebaseFirestore.getInstance();

        rvCommunityPosts = view.findViewById(R.id.rv_community_posts);
        rvCommunityPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCommunityPosts.addItemDecoration(new SpacingItemDecoration(10, 10));
        communityPostsAdapter = new CommunityPostsAdapter(communityPosts, getContext());
        rvCommunityPosts.setAdapter(communityPostsAdapter);


        loadCommunityPosts();

        Log.d("CommunityFragment", "onCreateView executed");

        return view;

    }


    private void loadCommunityPosts() {
        db.collection("community_posts")
                .orderBy("creation_date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> post = document.getData();
                            post.put("id", document.getId());
                            communityPosts.add(post);
                        }
                        communityPostsAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Failed to load posts", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}