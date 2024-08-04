package com.example.nuru;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {
    private List<String> days;
    private final OnItemListener onItemListener;
    private int currentDayPosition = -1;

    private Set<String> loggedMoodDays = new HashSet<>();

    private static final String TAG = "CalendarAdapter";

    CalendarAdapter(List<String> days, OnItemListener onItemListener, Set<String> loggedMoodDays) {
        this.days = days;
        this.onItemListener = onItemListener;
        this.loggedMoodDays = loggedMoodDays;
        this.currentDayPosition = getCurrentDayPosition();
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_cell, parent, false);
        return new CalendarViewHolder(view, onItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        String day = days.get(position);
        holder.dayNumberTextView.setText(day);
        Log.d(TAG, "Binding day: " + day);

        if (position == currentDayPosition) {
            holder.itemView.setBackgroundResource(R.drawable.selected_day_bg);
        } else {
            holder.itemView.setBackgroundResource(0);
        }

        // Show indicator if moods logged for the day
        if (loggedMoodDays.contains(day)) {
            holder.indicatorDot.setVisibility(View.VISIBLE);
        } else {
            holder.indicatorDot.setVisibility(View.GONE);
        }

        // Set click listener on itemView
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Calendar cell clicked: " + day);
            // Notify the listener about the item click
            onItemListener.onItemClick(day);
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public void updateDays(List<String> updatedDays, Set<String> updatedLoggedMoodDays) {
        this.days = updatedDays;
        this.loggedMoodDays = updatedLoggedMoodDays;
        currentDayPosition = getCurrentDayPosition();
        notifyDataSetChanged();
    }

    private int getCurrentDayPosition() {
        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        for (int i = 0; i < days.size(); i++) {
            if (days.get(i).equals(String.valueOf(currentDay))) {
                return i;
            }
        }
        return -1;
    }

    // ViewHolder class
    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView dayNumberTextView;
        View indicatorDot;

        CalendarViewHolder(View itemView, OnItemListener onItemListener) {
            super(itemView);
            dayNumberTextView = itemView.findViewById(R.id.cellDayText);
            indicatorDot = itemView.findViewById(R.id.indicator_dot);
        }
    }

    // Interface for item click handling
    public interface OnItemListener {
        void onItemClick(String day);
    }
}
