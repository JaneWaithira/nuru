package com.example.nuru;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminCommunityFragment extends Fragment {
    private TableLayout tableLayout;
    private FirebaseFirestore db;
    private Uri selectedImageUri;

    private static final int PICK_IMAGE_REQUEST = 1;

    // Activity result launcher for image picker
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            result -> {
                if (result != null) {
                    selectedImageUri = result;
                    // Display the selected image's name or other info
                    String displayName = getDisplayName(selectedImageUri);
                    // Assuming you have a selectedImageTextView in your layout
                    TextView selectedImageTextView = requireView().findViewById(R.id.text_selected_image);
                    if (selectedImageTextView != null) {
                        selectedImageTextView.setText(displayName);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_community, container, false);

        tableLayout = view.findViewById(R.id.community_posts_table);
        db = FirebaseFirestore.getInstance();

        loadTableHeaders();

        Button addPostButton = view.findViewById(R.id.add_post_btn);
        addPostButton.setOnClickListener(v -> showAddPostDialog());

        loadCommunityPosts();

        return view;
    }

    private void loadTableHeaders() {
    }

    private void showAddPostDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_post, null);
        dialogBuilder.setView(dialogView);

        EditText questionEditText = dialogView.findViewById(R.id.edit_text_question);
        EditText categoryEditText = dialogView.findViewById(R.id.edit_text_category);
        Button addImageButton = dialogView.findViewById(R.id.button_add_image);

        Button addButton = dialogView.findViewById(R.id.button_add);
        Button cancelButton = dialogView.findViewById(R.id.button_cancel);

        AlertDialog dialog = dialogBuilder.create();

        addImageButton.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
            Log.d("AdminCommunityFragment", "Image picker launched");
        });

        addButton.setOnClickListener(v -> {
            String question = questionEditText.getText().toString().trim();
            String category = categoryEditText.getText().toString().trim();

            if (question.isEmpty() || category.isEmpty() || selectedImageUri == null) {
                Toast.makeText(getContext(), "Please fill all fields and select an image", Toast.LENGTH_SHORT).show();
                Log.d("AdminCommunityFragment", "Add button clicked: Fields empty or image not selected");
            } else {
                // Image has been selected, upload it
                uploadImageToStorage(selectedImageUri, question, category, dialog);
                Log.d("AdminCommunityFragment", "Add button clicked: Image selected, uploading image");
            }
        });

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
            Log.d("AdminCommunityFragment", "Cancel button clicked: Dialog dismissed");
        });

        dialog.show();
        Log.d("AdminCommunityFragment", "Add post dialog shown");
    }

    // Helper method to get display name of selected image
    private String getDisplayName(Uri uri) {
        Cursor cursor = null;
        try {
            String[] projection = {MediaStore.Images.Media.DISPLAY_NAME};
            cursor = requireContext().getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    // Method to upload image to Firebase Storage
    private void uploadImageToStorage(Uri imageUri, String question, String category, AlertDialog dialog) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("images/" + imageUri.getLastPathSegment());

        UploadTask uploadTask = storageRef.putFile(imageUri);
        Log.d("AdminCommunityFragment", "Starting upload of image");

        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                if (downloadUri != null) {
                    String imageUrl = downloadUri.toString();
                    addPostToFirestore(question, category, imageUrl);
                    dialog.dismiss();
                    Log.d("AdminCommunityFragment", "Image upload successful, adding post to Firestore");
                } else {
                    Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                    Log.d("AdminCommunityFragment", "Failed to get download URL after image upload");
                }
            } else {
                Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                Log.d("AdminCommunityFragment", "Image upload task failed");
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("AdminCommunityFragment", "Failed to upload image: " + e.getMessage());
        });
    }

    private void addPostToFirestore(String question, String category, String imageUrl) {

        List<Map<String, Object>> comments = new ArrayList<>();

        Map<String, Object> post = new HashMap<>();
        post.put("question", question);
        post.put("category", category);
        post.put("image_url", imageUrl);
        post.put("comments", comments);
        post.put("creation_date", String.valueOf(System.currentTimeMillis()));

        db.collection("community_posts")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Post added successfully", Toast.LENGTH_SHORT).show();
                    loadCommunityPosts(); // Refresh the table after adding a new post
                    Log.d("AdminCommunityFragment", "Post added to Firestore successfully");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to add post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("AdminCommunityFragment", "Failed to add post to Firestore: " + e.getMessage());
                });
    }

    private void loadCommunityPosts() {
        db.collection("community_posts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        tableLayout.removeViews(1, tableLayout.getChildCount() - 1);

                        // Reverse iteration through the query result
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            String postId = document.getId();
                            String question = document.getString("question");
                            String imageUrl = document.getString("image_url");
                            String category = document.getString("category");
                            String creationDate = document.getString("creation_date");

                            Log.d("AdminCommunityFragment", "Post loaded from Firestore: " + postId);

                            TableRow row = new TableRow(getContext());
                            row.setLayoutParams(new TableLayout.LayoutParams(
                                    TableLayout.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT));

//                            Populate row with data
                            addTextViewToRow(row, postId);
                            addTextViewToRow(row, question);
                            addTextViewToRow(row, creationDate);
                            addTextViewToRow(row, category);
                            addActionButtonsToRow(row, postId, question, category, imageUrl);

                            tableLayout.addView(row);

                        }
                    } else {
                        Toast.makeText(getContext(), "Failed to fetch community posts.", Toast.LENGTH_SHORT).show();
                        Log.d("AdminCommunityFragment", "Failed to fetch community posts: " + task.getException().getMessage());
                    }
                });
    }



    private void addActionButtonsToRow(TableRow row, String postId, String question, String category, String imageUrl) {
        LinearLayout buttonLayout = new LinearLayout(getContext());
        buttonLayout.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button editButton = new Button(getContext());
        editButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        editButton.setText("Edit");
        editButton.setOnClickListener(v -> showEditPostDialog(postId, question, category, imageUrl));
        buttonLayout.addView(editButton);

        Button deleteButton = new Button(getContext());
        deleteButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        deleteButton.setText("Delete");
        deleteButton.setOnClickListener(v -> deletePost(postId));
        buttonLayout.addView(deleteButton);

        row.addView(buttonLayout);
    }

    private void addTextViewToRow(TableRow row, String text) {
        TextView textView = new TextView(getContext());
        textView.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textView.setText(text);
        textView.setPadding(8, 8, 8, 8);
        textView.setBackgroundResource(android.R.color.white);
        textView.setMaxLines(1);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(textView);

    }

    private void deletePost(String postId) {
        db.collection("community_posts").document(postId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Post deleted successfully", Toast.LENGTH_SHORT).show();
                    loadCommunityPosts(); // Refresh the table after deleting a post
                    Log.d("AdminCommunityFragment", "Post deleted from Firestore: " + postId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("AdminCommunityFragment", "Failed to delete post from Firestore: " + e.getMessage());
                });
    }

    private void showEditPostDialog(String postId, String question, String category, String imageUrl) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_post, null);
        dialogBuilder.setView(dialogView);

        EditText questionEditText = dialogView.findViewById(R.id.edit_text_question);
        EditText categoryEditText = dialogView.findViewById(R.id.edit_text_category);
        Button editImageButton = dialogView.findViewById(R.id.edit_image);

        questionEditText.setText(question);
        categoryEditText.setText(category);

        editImageButton.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
            Log.d("AdminCommunityFragment", "Edit image button clicked");
        });

        Button updateButton = dialogView.findViewById(R.id.button_update);
        Button cancelButton = dialogView.findViewById(R.id.button_cancel);

        AlertDialog dialog = dialogBuilder.create();

        updateButton.setOnClickListener(v -> {
            String newQuestion = questionEditText.getText().toString().trim();
            String newCategory = categoryEditText.getText().toString().trim();

            if (newQuestion.isEmpty() || newCategory.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                Log.d("AdminCommunityFragment", "Update button clicked: Fields empty");
            } else {
                if (selectedImageUri != null) {
                    // Image has been selected, upload it and update with image URL
                    uploadImageAndUpdatePost(selectedImageUri, postId, newQuestion, newCategory, dialog);
                    Log.d("AdminCommunityFragment", "Update button clicked: Image selected, uploading image");
                } else {
                    // No new image selected, update without changing the image URL
                    updatePostInFirestore(postId, newQuestion, newCategory, imageUrl);
                    dialog.dismiss();
                    Log.d("AdminCommunityFragment", "Update button clicked: No image selected, updating post without image");
                }
            }
        });

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
            Log.d("AdminCommunityFragment", "Cancel button clicked: Dialog dismissed");
        });

        dialog.show();
        Log.d("AdminCommunityFragment", "Edit post dialog shown");
    }

    private void uploadImageAndUpdatePost(Uri imageUri, String postId, String newQuestion, String newCategory, AlertDialog dialog) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("images/" + imageUri.getLastPathSegment());

        UploadTask uploadTask = storageRef.putFile(imageUri);
        Log.d("AdminCommunityFragment", "Starting upload of image");

        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                if (downloadUri != null) {
                    String imageUrl = downloadUri.toString();
                    updatePostInFirestore(postId, newQuestion, newCategory, imageUrl);
                    dialog.dismiss();
                    Log.d("AdminCommunityFragment", "Image upload successful, updating post in Firestore");
                } else {
                    Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                    Log.d("AdminCommunityFragment", "Failed to get download URL after image upload");
                }
            } else {
                Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                Log.d("AdminCommunityFragment", "Image upload task failed");
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("AdminCommunityFragment", "Failed to upload image: " + e.getMessage());
        });
    }

    private void updatePostInFirestore(String postId, String newQuestion, String newCategory, String newImageUrl) {
        DocumentReference postRef = db.collection("community_posts").document(postId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("question", newQuestion);
        updates.put("category", newCategory);
        updates.put("image_url", newImageUrl);

        postRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Post updated successfully", Toast.LENGTH_SHORT).show();
                    loadCommunityPosts(); // Refresh the table after updating a post
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
