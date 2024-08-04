package com.example.nuru;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommunityPostsAdapter extends RecyclerView.Adapter<CommunityPostsAdapter.PostViewHolder> {
    private List<Map<String, Object>> posts;
    private Context context;

    public CommunityPostsAdapter(List<Map<String, Object>> posts, Context context) {
        this.posts = posts;
        this.context = context;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_community_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Map<String, Object> post = posts.get(position);

        String category = (String) post.get("category");
        String imageUrl = (String) post.get("image_url");
        String question = (String) post.get("question");
        String postId = (String) post.get("id");

        holder.postCategory.setText(category);
        holder.postQuestion.setText(question);
        if (context != null) {
            Glide.with(context).load(imageUrl).into(holder.postImage);
        }

        // Load comments for the specific post
        loadComments(postId, holder);

        // Open comments activity on button click
        holder.viewCommentsBtn.setOnClickListener(v -> openCommentActivity(postId));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    private void loadComments(String postId, PostViewHolder holder) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("community_posts")
                .document(postId)
                .collection("comments")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot commentsSnapshot = task.getResult();
                        holder.comments.clear();
                        int commentCount = 0;
                        for (QueryDocumentSnapshot commentDocument : commentsSnapshot) {
                            if (commentCount < 2) { // Limit to 2 comments
                                Map<String, Object> comment = commentDocument.getData();
                                holder.comments.add(comment);
                            }
                            commentCount++;
                        }
                        if (!holder.comments.isEmpty()) {
                            holder.rvComments.setVisibility(View.VISIBLE);
                            holder.viewCommentsBtn.setVisibility(View.VISIBLE);
                            holder.noCommentsText.setVisibility(View.GONE);
                        } else {
                            holder.rvComments.setVisibility(View.GONE);
                            holder.viewCommentsBtn.setVisibility(View.GONE);
                            holder.noCommentsText.setVisibility(View.VISIBLE);
                        }
                        holder.commentsAdapter.notifyDataSetChanged();
                    } else {
                        holder.rvComments.setVisibility(View.GONE);
                        holder.viewCommentsBtn.setVisibility(View.GONE);
                        holder.noCommentsText.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void openCommentActivity(String postId) {
        if (postId != null && !postId.isEmpty()) {
            Intent intent = new Intent(context, CommentActivity.class);
            intent.putExtra("postId", postId);
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Failed to open comments", Toast.LENGTH_SHORT).show();
        }
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView postCategory, postQuestion, noCommentsText;
        ImageView postImage;
        Button viewCommentsBtn;
        RecyclerView rvComments;
        List<Map<String, Object>> comments = new ArrayList<>();
        CommentsAdapter commentsAdapter;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            postCategory = itemView.findViewById(R.id.post_category);
            postQuestion = itemView.findViewById(R.id.post_question);
            postImage = itemView.findViewById(R.id.post_image);
            noCommentsText = itemView.findViewById(R.id.no_comments_text);
            viewCommentsBtn = itemView.findViewById(R.id.view_comments_btn);
            rvComments = itemView.findViewById(R.id.rv_comment);

            rvComments.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            commentsAdapter = new CommentsAdapter(comments, itemView.getContext(), position -> {
                // No action needed
            }, false);
            rvComments.setAdapter(commentsAdapter);
        }
    }
}
