package com.example.nuru;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.Set;

public class MoodAdapter extends RecyclerView.Adapter<MoodAdapter.MoodViewHolder> {
    private String[] moods;
    private int[] drawableIds;
    private Set<Integer> selectedPositions = new HashSet<>();

    public MoodAdapter(String[] moods, int[] drawableIds) {
        this.moods = moods;
        this.drawableIds = drawableIds;
    }

    @NonNull
    @Override
    public MoodAdapter.MoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_moods, parent, false);
        return new MoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoodViewHolder holder, int position) {
        holder.moodTextView.setText(moods[position]);
        holder.moodTextView.setCompoundDrawablesWithIntrinsicBounds(drawableIds[position], 0, 0, 0);

        if (selectedPositions.contains(position)) {
            holder.moodTextView.setBackgroundResource(R.drawable.moods_bg_selected);
        } else {
            holder.moodTextView.setBackgroundResource(R.drawable.mood_bg);
        }

        holder.itemView.setOnClickListener(v -> {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove(position);
            } else {
                selectedPositions.add(position);
            }
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return moods.length;
    }

    public class MoodViewHolder extends RecyclerView.ViewHolder {
        TextView moodTextView;

        public MoodViewHolder(@NonNull View itemView) {
            super(itemView);
            moodTextView = itemView.findViewById(R.id.moodTextView);
        }
    }

    public Set<Integer> getSelectedPositions() {
        return selectedPositions;
    }

    public Set<String> getSelectedMoods() {
        Set<String> selectedMoods = new HashSet<>();
        for (Integer position : selectedPositions) {
            selectedMoods.add(moods[position]);
        }
        return selectedMoods;
    }
}
