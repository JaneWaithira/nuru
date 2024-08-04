package com.example.nuru;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Map;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    public interface OnCommentDeleteListener {
        void onCommentDelete(int position);
    }

    private List<Map<String, Object>> comments;
    private Context context;
    private OnCommentDeleteListener deleteListener;

    private boolean showDeleteIcon;
    public CommentsAdapter(List<Map<String, Object>> comments, Context context, OnCommentDeleteListener deleteListener, boolean showDeleteIcon) {
        this.comments = comments;
        this.context = context;
        this.deleteListener = deleteListener;
        this.showDeleteIcon = showDeleteIcon;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Map<String, Object> comment = comments.get(position);

        String commentText = (String) comment.get("comment");
        String profilePictureUrl = (String) comment.get("profile_picture_url");
        String userId = (String) comment.get("user_id");

        Log.d("CommentsAdapter", "Binding comment at position " + position);
        Log.d("CommentsAdapter", "Comment text: " + commentText);
        Log.d("CommentsAdapter", "Profile picture URL: " + profilePictureUrl);

        holder.commentTextView.setText(commentText);
        Glide.with(context).load(profilePictureUrl).into(holder.profileImageView);

        if (showDeleteIcon) {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            if (currentUserId.equals(userId)) {
                holder.deleteComment.setVisibility(View.VISIBLE);
                holder.deleteComment.setOnClickListener(v -> {
                    deleteListener.onCommentDelete(position);
                });
            } else {
                holder.deleteComment.setVisibility(View.GONE);
            }
        } else {
            holder.deleteComment.setVisibility(View.GONE);
        }


    }

    @Override
    public int getItemCount() {
        int size = comments.size();
        Log.d("CommentsAdapter", "Total comments: " + size);
        return size;
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        public TextView commentTextView;
        public ImageView profileImageView;
        public ImageView deleteComment;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            commentTextView = itemView.findViewById(R.id.comment_content);
            profileImageView = itemView.findViewById(R.id.comment_user_id);
            deleteComment = itemView.findViewById(R.id.delete_comment);
            Log.d("CommentViewHolder", "CommentViewHolder created");
            if (commentTextView == null) {
                Log.e("CommentViewHolder", "commentTextView is null");
            } else {
                Log.d("CommentViewHolder", "commentTextView found");
            }
            if (profileImageView == null) {
                Log.e("CommentViewHolder", "profileImageView is null");
            } else {
                Log.d("CommentViewHolder", "profileImageView found");
            }
        }
    }
}
