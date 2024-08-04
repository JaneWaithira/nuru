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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class ResourceAdapter extends RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder> {
    private Context context;
    private List<QueryDocumentSnapshot> resourceList;

    private OnResourceClickListener listener;

    public ResourceAdapter(Context context, List<QueryDocumentSnapshot> resourceList,OnResourceClickListener listener ) {
        this.context = context;
        this.resourceList = resourceList;
        this.listener = listener;
    }



    @NonNull
    @Override
    public ResourceAdapter.ResourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.rv_resources_item, parent, false);
        return new ResourceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResourceViewHolder holder, int position) {
        QueryDocumentSnapshot document  = resourceList.get(position);

        String title = document.getString("title");
        String imageUrl = document.getString("image_url");


        holder.resourceTitle.setText(title);
        Log.d("ResourceAdapter", "Image URL: " + imageUrl);
        Glide.with(context).load(imageUrl).into(holder.resourceImage);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onResourceClick(document);
            }
        });

    }

    @Override
    public int getItemCount() {
        return resourceList.size();
    }

    public static class ResourceViewHolder extends RecyclerView.ViewHolder {
        ImageView resourceImage;
        TextView resourceTitle;

        public ResourceViewHolder(@NonNull View itemView) {
            super(itemView);
            resourceImage = itemView.findViewById(R.id.resource_image);
            resourceTitle = itemView.findViewById(R.id.resource_title);
        }


    }
    public interface OnResourceClickListener {
        void onResourceClick(QueryDocumentSnapshot resource);
    }
}
