package com.example.nuru;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ImageView backArrow;
    private ImageView postImage;
    private TextView postQuestion;
    private ImageView commentUserImage;

    private EditText commentEditText;
    private Button addCommentButton;
    private RecyclerView rvComments;
    private List<Map<String, Object>> comments = new ArrayList<>();
    private CommentsAdapter commentsAdapter;
    private String postId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comment_item);

        // Fetch postId from intent extras
        postId = getIntent().getStringExtra("postId");
        if (postId == null) {
            Log.e("CommentActivity", "postId is null, unable to proceed");
            finish(); // Finish the activity if postId is null
            return;
        }

        db = FirebaseFirestore.getInstance();

        backArrow = findViewById(R.id.back_arrow);

        postImage = findViewById(R.id.post_image);
        postQuestion = findViewById(R.id.post_question);
        commentUserImage = findViewById(R.id.comment_user_id);
        commentEditText = findViewById(R.id.post_detail_comment);
        addCommentButton = findViewById(R.id.post_detail_add_comment_btn);
        rvComments = findViewById(R.id.rv_comment);

        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentsAdapter = new CommentsAdapter(comments, getBaseContext(), position -> deleteComment(position), true);
        rvComments.setAdapter(commentsAdapter);

        loadPostDetails(postId);

        addCommentButton.setOnClickListener(v -> addComment());
        Log.d("CommentActivity", "onCreate executed with postId: " + postId);

        backArrow.setOnClickListener(v -> finish());
    }

    private void loadPostDetails(String postId) {
        db.collection("community_posts").document(postId).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                Log.d("CommentActivity", "Post details loaded successfully");
                String imageUrl = document.getString("image_url");
                String question = document.getString("question");

                if (!isFinishing() && !isDestroyed()) {
                    Glide.with(this)
                            .load(imageUrl)
                            .into(postImage);
                }

                postQuestion.setText(question);
                loadComments(postId);
            } else {
                Log.e("CommentActivity", "Post details not found");
            }
        }).addOnFailureListener(e -> {
            Log.e("CommentActivity", "Error loading post details", e);
        });
    }

    private void loadComments(String postId) {
        Log.d("CommentActivity", "Loading comments for postId: " + postId);
        db.collection("community_posts").document(postId).collection("comments").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("CommentActivity", "Comments loaded successfully");
                comments.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Map<String, Object> commentData = document.getData();
                    String userId = (String) commentData.get("user_id");
                    db.collection("users").document(userId).get().addOnSuccessListener(userDocument -> {
                        if (userDocument.exists()) {
                            String profilePictureUrl = userDocument.getString("ProfilePicUrl");
                            commentData.put("profile_picture_url", profilePictureUrl);
                        }
                        comments.add(commentData);
                        commentsAdapter.notifyDataSetChanged();
                    }).addOnFailureListener(e -> {
                        Log.e("CommentActivity", "Error fetching user profile", e);
                    });
                }
            } else {
                Log.e("CommentActivity", "Failed to load comments", task.getException());
            }
        });
    }


    private void addComment() {
        Log.d("CommentActivity", "Add comment button clicked");
        String commentText = commentEditText.getText().toString().trim();
        if (!commentText.isEmpty()) {
            String postId = getIntent().getStringExtra("postId");

            // Fetch user profile information from Firestore
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String profilePictureUrl = documentSnapshot.getString("ProfilePicUrl");

                            // Create comment object
                            Map<String, Object> comment = new HashMap<>();
                            comment.put("comment", commentText);
                            comment.put("user_id", userId);
                            comment.put("profile_picture_url", profilePictureUrl);

                            // Add comment to Firestore
                            db.collection("community_posts").document(postId).collection("comments").add(comment)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(this, "Comment added", Toast.LENGTH_SHORT).show();
                                        loadComments(postId);
                                        commentEditText.setText("");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("CommentActivity", "Error adding comment", e);
                                        Toast.makeText(this, "Failed to add comment", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Log.e("CommentActivity", "User profile not found in Firestore");
                            Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CommentActivity", "Error fetching user profile from Firestore", e);
                        Toast.makeText(this, "Failed to fetch user profile", Toast.LENGTH_SHORT).show();
                    });

        } else {
            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteComment(int position) {
        Map<String, Object> comment = comments.get(position);
        String commentId = (String) comment.get("comment_id");

        db.collection("community_posts").document(postId).collection("comments").document(commentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Comment deleted", Toast.LENGTH_SHORT).show();
                    loadComments(postId);
                })
                .addOnFailureListener(e -> {
                    Log.e("CommentActivity", "Error deleting comment", e);
                    Toast.makeText(this, "Failed to delete comment", Toast.LENGTH_SHORT).show();
                });
    }

}
