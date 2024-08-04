package com.example.nuru;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import java.util.Locale;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeFragment extends Fragment implements CalendarAdapter.OnItemListener, ResourceAdapter.OnResourceClickListener{

    private RecyclerView calendarRecyclerView;
    private CalendarAdapter calendarAdapter;

    private RecyclerView resourcesRecyclerView;
    private Set<String> loggedMoodDays = new HashSet<>();
    private ResourceAdapter resourceAdapter;
    private List<QueryDocumentSnapshot> resourceList;

    private TextView monthYearText;
    private ImageView previousWeek, nextWeek;

    private Calendar currentCalendar;
    private LocalDate date;
    
    private PieChart pieChart;

    private static final String TAG = "HomeFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        monthYearText = view.findViewById(R.id.monthYearTV);
        calendarRecyclerView = view.findViewById(R.id.calendarRecyclerView);
        previousWeek = view.findViewById(R.id.previousWeekTV);
        nextWeek = view.findViewById(R.id.nextWeekIV);

        currentCalendar = Calendar.getInstance();
        currentCalendar.setFirstDayOfWeek(Calendar.SUNDAY);
        updateMonthYearLabel(currentCalendar);

        calendarRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 7));
        calendarAdapter = new CalendarAdapter(generateCalendarDays(), this, loggedMoodDays);
        calendarRecyclerView.setAdapter(calendarAdapter);

        resourcesRecyclerView = view.findViewById(R.id.rv_resources);

        //Resource recycler view
        resourcesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        resourcesRecyclerView.addItemDecoration(new SpacingItemDecoration(16, 16));
        resourceList = new ArrayList<>();
        resourceAdapter = new ResourceAdapter(getContext(), resourceList, (ResourceAdapter.OnResourceClickListener) this);
        resourcesRecyclerView.setAdapter(resourceAdapter);
        
        pieChart = view.findViewById(R.id.moods_pie);

        loadResourcesFromFirestore();
        loadMonthlyMoodCounts();

        loadLoggedMoodDays(() -> {
            calendarAdapter = new CalendarAdapter(generateCalendarDays(), this, loggedMoodDays);
            calendarRecyclerView.setAdapter(calendarAdapter);
        });

        // Previous week arrow click
        previousWeek.setOnClickListener(v -> {
            currentCalendar.add(Calendar.WEEK_OF_YEAR, -1);
            updateMonthYearLabel(currentCalendar);
            calendarAdapter.updateDays(generateCalendarDays(), loggedMoodDays); // Missing semicolon here
        });

        // Next week arrow click
        nextWeek.setOnClickListener(v -> {
            currentCalendar.add(Calendar.WEEK_OF_YEAR, 1);
            updateMonthYearLabel(currentCalendar);
            calendarAdapter.updateDays(generateCalendarDays(), loggedMoodDays);
        });
        

        view.findViewById(R.id.log_mood_btn).setOnClickListener(v -> openMoodLoggingPage(-1));

        return view;
    }

    private void loadLoggedMoodDays(Runnable onLoadComplete) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(userId).collection("mood_logs")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (document.contains("date")) {
                                long timestamp = document.getLong("date");
                                Date date = new Date(timestamp);
                                DateFormat dateFormat = new SimpleDateFormat("dd", Locale.getDefault());
                                String formattedDate = dateFormat.format(date);
                                loggedMoodDays.add(formattedDate);
                            } else {
                                Log.w(TAG, "Document does not contain a date field");
                            }
                        }
                        onLoadComplete.run();
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }



    private void loadMonthlyMoodCounts() {
        List<Map<String, Integer>> dailyMoodCounts = fetchDailyMoodCounts();
        setUpHalfPieChart(dailyMoodCounts);
    }

    private List<Map<String, Integer>> fetchDailyMoodCounts() {
        List<Map<String, Integer>> dailyMoodCounts = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(userId).collection("mood_logs")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            List<String> moods = (List<String>) document.get("moods");
                            Map<String, Integer> dailyMoodCount = new HashMap<>();

                            for (String mood : moods) {
                                dailyMoodCount.put(mood, dailyMoodCount.getOrDefault(mood, 0) + 1);
                            }

                            dailyMoodCounts.add(dailyMoodCount);
                        }
                        Log.d(TAG, "Mood data loaded: " + dailyMoodCounts);
                        setUpHalfPieChart(dailyMoodCounts);
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
        return dailyMoodCounts;
    }


    private void setUpHalfPieChart(List<Map<String, Integer>> dailyMoodCounts) {
        Map<String, Integer> monthlyMoodCounts = new HashMap<>();
        String[] MOODS = {"Happy", "Sad", "Angry", "Anxious", "Calm", "Tired", "Scared", "Confused"};
        int[] MOOD_COLORS = {Color.YELLOW, Color.BLUE, Color.RED, Color.MAGENTA, Color.GREEN, Color.rgb(0,0,128), Color.rgb(0, 0, 0), Color.rgb(150, 75, 0)};

        for (Map<String, Integer> dailyMood : dailyMoodCounts) {
            for (String mood : MOODS) {
                int count = dailyMood.getOrDefault(mood, 0);
                monthlyMoodCounts.put(mood, monthlyMoodCounts.getOrDefault(mood, 0) + count);
            }
        }

        List<PieEntry> pieEntries = new ArrayList<>();
        for (String mood : MOODS) {
            int count = monthlyMoodCounts.getOrDefault(mood, 0);
            if (count > 0) {
                pieEntries.add(new PieEntry(count, mood));
            }
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, "");
        dataSet.setColors(MOOD_COLORS);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.parseColor("#CC352671"));

        pieChart.setData(data);
        pieChart.invalidate(); // Refresh the chart

        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(false);
        pieChart.setRotationAngle(180f); // Start from top
        pieChart.setMaxAngle(180f); // Half pie chart

        pieChart.setTransparentCircleRadius(58f);
        pieChart.setHoleRadius(58f);
        pieChart.setDrawEntryLabels(false);

        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setYOffset(10f);
        legend.setTextSize(12f);
        legend.setXEntrySpace(20f);
        legend.setYEntrySpace(10f);
        legend.setWordWrapEnabled(true);

        pieChart.animateY(1400, Easing.EaseInOutQuad); // Animation

    }

    private void showResourceDialog(QueryDocumentSnapshot resource) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_resource_details, null);

        TextView titleTextView = dialogView.findViewById(R.id.resource_title);
        TextView locationTextView = dialogView.findViewById(R.id.resource_location);
        TextView dateTextView = dialogView.findViewById(R.id.resource_date);
        TextView linkTextView = dialogView.findViewById(R.id.resource_link);
        ImageButton closeButton = dialogView.findViewById(R.id.close_btn);

        titleTextView.setText(resource.getString("title"));
        locationTextView.setText(resource.getString("location"));
        dateTextView.setText(resource.getString("date"));
        linkTextView.setText(resource.getString("link"));

        Log.d("ResourceDialog", "Dialog created with title: " + resource.getString("title"));
//        Make the link clickable
        linkTextView.setOnClickListener(v -> {
            String url = resource.getString("link");
            Log.d("ResourceDialog", "Link TextView clicked. URL retrieved: " + url);
            if (url != null && !url.isEmpty()) {
                showNavigationAlert(url);
            } else {
                Log.d("ResourceDialog", "Invalid URL, displaying toast.");
                Toast.makeText(getContext(), "Invalid link.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showNavigationAlert(String url) {
        Log.d("ResourceDialog", "Showing navigation alert for URL: " + url);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Navigate to External Page")
        .setMessage("You are about to open a link in an external browser. Do you want to continue?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Log.d("ResourceDialog", "User chose to continue to URL.");
                    openUrlWithChooser(url);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    Log.d("ResourceDialog", "User canceled navigation to URL.");
                    dialog.dismiss();
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void openUrlWithChooser(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        Log.d("ResourceDialog", "Intent created with URL: " + url);

        Intent chooser = Intent.createChooser(intent, "Open with");
        if (chooser.resolveActivity(requireActivity().getPackageManager()) != null) {
            Log.d("ResourceDialog", "Activity found to handle intent.");
            startActivity(chooser);
        } else {
            Log.d("ResourceDialog", "No activity found to handle intent.");
            Toast.makeText(getContext(), "No application available to handle request. Please install a web browser.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadResourcesFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("resources")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        resourceList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            resourceList.add(document);
                        }
                        resourceAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Resources loaded successfully. Count: " + resourceList.size());
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }

    private List<String> generateCalendarDays() {
        List<String> days = new ArrayList<>();
        Calendar calendar = (Calendar) currentCalendar.clone();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        SimpleDateFormat sdf = new SimpleDateFormat("d");
        for (int i = 0; i < 7; i++) {
            days.add(sdf.format(calendar.getTime()));
            Log.d("CalendarDays", "Generated day: " + sdf.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    private void updateMonthYearLabel(Calendar calendar) {
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        String monthYear = new DateFormatSymbols().getMonths()[month] + " " + year;
        monthYearText.setText(monthYear);
    }

    @Override
    public void onItemClick(String day) {
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH);
        int dayOfMonth = Integer.parseInt(day);

        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(year, month, dayOfMonth);

        openMoodLoggingPage(selectedDate.getTimeInMillis());
        Log.d(TAG, "Calendar day clicked: " + day);
    }

    public void openMoodLoggingPage(long selectedDateMillis) {
        MoodsFragment moodsFragment = new MoodsFragment();
        Bundle args = new Bundle();
        args.putLong("selected_day",selectedDateMillis);
        moodsFragment.setArguments(args);

        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, moodsFragment);
        transaction.addToBackStack(null);
        try {
            transaction.commit();
            Log.d(TAG, "FragmentTransaction committed successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error committing FragmentTransaction: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @Override
    public void onResourceClick(QueryDocumentSnapshot resource) {
        showResourceDialog(resource);
    }
}
