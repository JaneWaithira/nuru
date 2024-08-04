package com.example.nuru;

import android.database.Cursor;
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
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AdminResourcesFragment extends Fragment {
    private TableLayout tableLayout;
    private FirebaseFirestore db;
    private Uri selectedImageUri;

    // Activity result launcher for image picker
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            result -> {
                if (result != null) {
                    selectedImageUri = result;
                    // Display the selected image's name or other info
                    String displayName = getDisplayName(selectedImageUri);
                    TextView selectedImageTextView = requireView().findViewById(R.id.text_selected_image);
                    if (selectedImageTextView != null) {
                        selectedImageTextView.setText(displayName);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_resources, container, false);

        tableLayout = view.findViewById(R.id.resources_table);
        db = FirebaseFirestore.getInstance();

        loadTableHeaders();

        Button addPostButton = view.findViewById(R.id.add_resource_btn);
        addPostButton.setOnClickListener(v -> showAddPostDialog());

        loadCommunityPosts();

        return view;
    }

    private void loadTableHeaders() {

    }

    private void showAddPostDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_resource, null);
        dialogBuilder.setView(dialogView);

        EditText titleEditText = dialogView.findViewById(R.id.edit_text_title);
        EditText locationEditText = dialogView.findViewById(R.id.edit_text_location);
        EditText dateEditText = dialogView.findViewById(R.id.edit_text_date);
        EditText linkEditText = dialogView.findViewById(R.id.edit_text_link);
        Button addImageButton = dialogView.findViewById(R.id.button_add_image);

        Button addButton = dialogView.findViewById(R.id.button_add);
        Button cancelButton = dialogView.findViewById(R.id.button_cancel);

        AlertDialog dialog = dialogBuilder.create();

        addImageButton.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
            Log.d("AdminResourcesFragment", "Image picker launched");
        });

        addButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String location = locationEditText.getText().toString().trim();
            String date = dateEditText.getText().toString().trim();
            String link = linkEditText.getText().toString().trim();

            if (title.isEmpty() || location.isEmpty() || selectedImageUri == null) {
                Toast.makeText(getContext(), "Please fill all fields and select an image", Toast.LENGTH_SHORT).show();
                Log.d("AdminResourcesFragment", "Add button clicked: Fields empty or image not selected");
            } else {
                // Image has been selected, upload it
                uploadImageToStorage(selectedImageUri, title, location, date, link, dialog);
                Log.d("AdminResourcesFragment", "Add button clicked: Image selected, uploading image");
            }
        });

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
            Log.d("AdminResourcesFragment", "Cancel button clicked: Dialog dismissed");
        });

        dialog.show();
        Log.d("AdminResourcesFragment", "Add post dialog shown");
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
    private void uploadImageToStorage(Uri imageUri, String title, String location, String date, String link, AlertDialog dialog) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("images/" + imageUri.getLastPathSegment());

        UploadTask uploadTask = storageRef.putFile(imageUri);
        Log.d("AdminResourcesFragment", "Starting upload of image");

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
                    addPostToFirestore(title, location, imageUrl, date, link);
                    dialog.dismiss();
                    Log.d("AdminResourcesFragment", "Image upload successful, adding post to Firestore");
                } else {
                    Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                    Log.d("AdminResourcesFragment", "Failed to get download URL after image upload");
                }
            } else {
                Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                Log.d("AdminResourcesFragment", "Image upload task failed");
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d("AdminResourcesFragment", "Failed to upload image: " + e.getMessage());
        });
    }

    private void addPostToFirestore(String title, String location, String imageUrl, String date, String link) {
        Map<String, Object> post = new HashMap<>();
        post.put("title", title);
        post.put("location", location);
        post.put("image_url", imageUrl);
        post.put("date", date);
        post.put("link", link);

        db.collection("resources")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Resource added successfully", Toast.LENGTH_SHORT).show();
                    loadCommunityPosts(); // Refresh the table after adding a new post
                    Log.d("AdminResourcesFragment", "Post added to Firestore successfully");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to add resource: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("AdminResourcesFragment", "Failed to add post to Firestore: " + e.getMessage());
                });
    }

    private void loadCommunityPosts() {
        db.collection("resources")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        tableLayout.removeViews(1, tableLayout.getChildCount() - 1);

                        // Reverse iteration through the query result
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            String postId = document.getId();
                            String title = document.getString("title");
                            String imageUrl = document.getString("image_url");
                            String location = document.getString("location");
                            String date = document.getString("date");
                            String link = document.getString("link");

                            Log.d("AdminResourcesFragment", "Post loaded from Firestore: " + postId);

                            TableRow row = new TableRow(getContext());
                            row.setLayoutParams(new TableLayout.LayoutParams(
                                    TableLayout.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT));

                            // Populate row with data
                            addTextViewToRow(row, title);
                            addTextViewToRow(row, imageUrl);
                            addTextViewToRow(row, location);
                            addTextViewToRow(row, date);
                            addTextViewToRow(row, link);
                            addActionButtonsToRow(row, postId, title, location, date, link, imageUrl);

                            tableLayout.addView(row);
                        }
                    } else {
                        Toast.makeText(getContext(), "Failed to fetch community posts.", Toast.LENGTH_SHORT).show();
                        Log.d("AdminResourcesFragment", "Failed to fetch community posts: " + task.getException().getMessage());
                    }
                });
    }

    private void addActionButtonsToRow(TableRow row, String postId, String title, String location, String date, String link, String imageUrl) {
        Button editButton = new Button(getContext());
        editButton.setText("Edit");
        editButton.setOnClickListener(v -> showEditPostDialog(postId, title, location, date, link, imageUrl));

        Button deleteButton = new Button(getContext());
        deleteButton.setText("Delete");
        deleteButton.setOnClickListener(v -> deletePost(postId));

        row.addView(editButton);
        row.addView(deleteButton);
    }

    private void showEditPostDialog(String postId, String title, String location, String date, String link, String imageUrl) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_resource, null);
        dialogBuilder.setView(dialogView);

        EditText titleEditText = dialogView.findViewById(R.id.edit_text_title);
        EditText locationEditText = dialogView.findViewById(R.id.edit_text_location);
        EditText dateEditText = dialogView.findViewById(R.id.edit_text_date);
        EditText linkEditText = dialogView.findViewById(R.id.edit_text_link);
        Button editImageButton = dialogView.findViewById(R.id.button_add_image);

        titleEditText.setText(title);
        locationEditText.setText(location);
        dateEditText.setText(date);
        linkEditText.setText(link);

        editImageButton.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
            Log.d("AdminResourcesFragment", "Edit image button clicked");
        });

        Button updateButton = dialogView.findViewById(R.id.button_update);
        Button cancelButton = dialogView.findViewById(R.id.button_cancel);

        AlertDialog dialog = dialogBuilder.create();

        updateButton.setOnClickListener(v -> {
            String newTitle = titleEditText.getText().toString().trim();
            String newLocation = locationEditText.getText().toString().trim();
            String newDate = dateEditText.getText().toString().trim();
            String newLink = linkEditText.getText().toString().trim();

            if (newTitle.isEmpty() || newLocation.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                Log.d("AdminResourcesFragment", "Update button clicked: Fields empty");
            } else {
                if (selectedImageUri != null) {
                    // Image has been selected, upload it and update with image URL
                    uploadImageToStorage(selectedImageUri, newTitle, newLocation, newDate, newLink, dialog);
                    Log.d("AdminResourcesFragment", "Update button clicked: Image selected, uploading image");
                } else {
                    // No new image selected, update without changing the image URL
                    updatePostInFirestore(postId, newTitle, newLocation, newDate, imageUrl, newLink);
                    dialog.dismiss();
                    Log.d("AdminResourcesFragment", "Update button clicked: No image selected, updating post without image");
                }
            }
        });

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
            Log.d("AdminResourcesFragment", "Cancel button clicked: Dialog dismissed");
        });

        dialog.show();
        Log.d("AdminResourcesFragment", "Edit post dialog shown");
    }

    private void updatePostInFirestore(String postId, String newTitle, String newLocation, String newDate, String newImageUrl, String newLink) {
        DocumentReference postRef = db.collection("resources").document(postId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", newTitle);
        updates.put("location", newLocation);
        updates.put("date", newDate);
        updates.put("link", newLink);
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

    private void deletePost(String postId) {
        DocumentReference postRef = db.collection("resources").document(postId);

        postRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Post deleted successfully", Toast.LENGTH_SHORT).show();
                    loadCommunityPosts(); // Refresh the table after deleting a post
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addTextViewToRow(TableRow row, String text) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(8, 8, 8, 8);
        row.addView(textView);
    }
}
