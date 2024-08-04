package com.example.nuru;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.nuru.R;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class InsightsFragment extends Fragment {
    private FirebaseFirestore db;
    private LineChart lineChart;
    private PieChart pieChart;
    private BarChart barChart;

    private static final String TAG = "InsightsFragment";
    private static final String[] MOODS = {"Happy", "Sad", "Angry", "Calm", "Anxious", "Tired", "Scared", "Confused"};
    private static final int[] MOOD_COLORS = {Color.YELLOW, Color.RED, Color.BLACK, Color.GREEN, Color.BLUE, Color.GRAY, Color.MAGENTA, Color.CYAN};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_insights, container, false);

        lineChart = view.findViewById(R.id.lineChart);
        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);

        db = FirebaseFirestore.getInstance();
        loadInsightsData();

        return view;
    }

    private void loadInsightsData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users")
                .document(userId)
                .collection("mood_logs")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            List<Map<String, Integer>> dailyMoodCounts = new ArrayList<>();
                            Map<String, Integer> overallMoodCounts = new HashMap<>();
                            Map<String, Integer> copingMechanismCount = new HashMap<>();
                            Map<String, Map<String, Integer>> triggerCopingMap = new HashMap<>();

                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            Date earliestDate = null;

                            for (QueryDocumentSnapshot document : querySnapshot) {
                                List<String> moods = (List<String>) document.get("moods");
                                Map<String, Integer> dailyMoods = new HashMap<>();
                                List<String> copingMechanisms = (List<String>) document.get("coping_mechanisms");
                                List<String> sensoryExperiences = (List<String>) document.get("sensory_experiences");

                                Object dateObj = document.get("date");
                                String dateStr = null;
                                if (dateObj instanceof Long) {
                                    dateStr = dateFormat.format(new Date((Long) dateObj));
                                } else if (dateObj instanceof Timestamp) {
                                    dateStr = dateFormat.format(((Timestamp) dateObj).toDate());
                                }

                                if (dateStr != null) {
                                    try {
                                        Date date = dateFormat.parse(dateStr);
                                        if (earliestDate == null || (date != null && date.before(earliestDate))) {
                                            earliestDate = date;
                                        }
                                    } catch (ParseException e) {
                                        Log.e(TAG, "Error parsing date", e);
                                    }
                                } else {
                                    Log.e(TAG, "Date string is null for document: " + document.getId());
                                }

                                if (moods != null) {
                                    for (String mood : moods) {
                                        dailyMoods.put(mood, dailyMoods.getOrDefault(mood, 0) + 1);
                                        overallMoodCounts.put(mood, overallMoodCounts.getOrDefault(mood, 0) + 1);
                                    }
                                }

                                if (copingMechanisms != null) {
                                    for (String copingMechanism : copingMechanisms) {
                                        copingMechanismCount.put(copingMechanism, copingMechanismCount.getOrDefault(copingMechanism, 0) + 1);
                                    }
                                }

                                if (sensoryExperiences != null) {
                                    for (String trigger : sensoryExperiences) {
                                        Map<String, Integer> copingMap = triggerCopingMap.getOrDefault(trigger, new HashMap<>());
                                        for (String copingMechanism : copingMechanisms) {
                                            copingMap.put(copingMechanism, copingMap.getOrDefault(copingMechanism, 0) + 1);
                                        }
                                        triggerCopingMap.put(trigger, copingMap);
                                    }
                                }
                                dailyMoodCounts.add(dailyMoods);
                            }

                            long startDate = 0;
                            if (earliestDate != null) {
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(earliestDate);
                                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                                startDate = calendar.getTimeInMillis();
                            }

                            setupLineChart(dailyMoodCounts, startDate);
                            setupPieChartForCopingMechanisms(copingMechanismCount);
                            setupHorizontalBarChart(triggerCopingMap);
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }

    private void setupLineChart(List<Map<String, Integer>> dailyMoodCounts, long startDate) {
        List<ILineDataSet> dataSets = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startDate);

        while (dailyMoodCounts.size() < 7) {
            dailyMoodCounts.add(new HashMap<>());
        }

        for (int i = 0; i < MOODS.length; i++) {
            String mood = MOODS[i];
            List<Entry> moodEntries = new ArrayList<>();
            for (int j = 0; j < 7; j++) {
                calendar.setTimeInMillis(startDate + j * 24 * 60 * 60 * 1000); // Add a day
                Map<String, Integer> dailyMoods = dailyMoodCounts.get(j);
                float moodValue = dailyMoods.getOrDefault(mood, 0);
                moodEntries.add(new Entry(j, moodValue));
            }
            LineDataSet lineDataSet = new LineDataSet(moodEntries, mood);
            lineDataSet.setColor(MOOD_COLORS[i]);
            lineDataSet.setCircleColor(MOOD_COLORS[i]);
            lineDataSet.setLineWidth(2f);
            lineDataSet.setCircleRadius(3f);
            lineDataSet.setDrawValues(false);
            lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smoother lines
            lineDataSet.setDrawHighlightIndicators(true); // Highlight line
            dataSets.add(lineDataSet);
        }

        LineData lineData = new LineData(dataSets);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7); // Ensure this is always 7
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                calendar.setTimeInMillis(startDate + (int) value * 24 * 60 * 60 * 1000);
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE");
                return dateFormat.format(calendar.getTime());
            }
        });

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setGranularity(1f);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        lineChart.getDescription().setEnabled(false);
        Legend legend = lineChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);

        lineChart.setExtraOffsets(5, 10, 5, 20); // Add some offset to prevent clipping of labels
        lineChart.setDragEnabled(true); // Enable dragging
        lineChart.setScaleEnabled(true); // Enable scaling
        lineChart.animateX(1500, Easing.EaseInOutQuart); // Animation
        lineChart.invalidate();
    }

    private void setupPieChartForCopingMechanisms(Map<String, Integer> copingMechanismCount) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : copingMechanismCount.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Coping Mechanisms");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);

        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(false);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);


        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setXEntrySpace(7f);
        legend.setYEntrySpace(5f);
        legend.setYOffset(10f);

        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setExtraOffsets(5, 10, 5, 20); // offset to prevent clipping of labels
        pieChart.setDragDecelerationFrictionCoef(0.95f); // Adjust friction for smooth scrolling
        pieChart.animateY(1500, Easing.EaseInOutQuad); // Animation
        pieChart.invalidate();
    }

    private void setupHorizontalBarChart(Map<String, Map<String, Integer>> triggerCopingMap) {
        List<String> triggers = new ArrayList<>(triggerCopingMap.keySet());
        Set<String> uniqueCopingMechanisms = new HashSet<>();
        for (Map<String, Integer> copingMap : triggerCopingMap.values()) {
            uniqueCopingMechanisms.addAll(copingMap.keySet());
        }
        List<String> copingMechanisms = new ArrayList<>(uniqueCopingMechanisms);

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < triggers.size(); i++) {
            String trigger = triggers.get(i);
            Map<String, Integer> copingMap = triggerCopingMap.get(trigger);
            float[] values = new float[copingMechanisms.size()];
            for (int j = 0; j < copingMechanisms.size(); j++) {
                values[j] = copingMap.getOrDefault(copingMechanisms.get(j), 0);
            }
            entries.add(new BarEntry(i, values));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Trigger-Coping Mechanism Relationships");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value > 0 ? String.valueOf((int) value) : "";
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.75f);

        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(triggers));
        xAxis.setLabelRotationAngle(0);
        xAxis.setDrawLabels(true);
        xAxis.setYOffset(10f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setGranularity(1f);
        leftAxis.setAxisMinimum(0f);

        barChart.getAxisRight().setEnabled(false);

        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setForm(Legend.LegendForm.SQUARE);
        legend.setFormSize(9f);
        legend.setTextSize(11f);
        legend.setXEntrySpace(4f);
        legend.setYOffset(5f);

        legend.setCustom(
                IntStream.range(0, copingMechanisms.size())
                        .mapToObj(i -> new LegendEntry(
                                copingMechanisms.get(i),
                                Legend.LegendForm.SQUARE,
                                8f,
                                2f,
                                null,
                                ColorTemplate.MATERIAL_COLORS[i % ColorTemplate.MATERIAL_COLORS.length]
                        ))
                        .collect(Collectors.toList())
        );

        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(true);
        barChart.setExtraOffsets(10, 10, 10, 30);
        barChart.setDragEnabled(true); // Enable dragging
        barChart.setScaleEnabled(true); // Enable scaling
        barChart.animateY(1500, Easing.EaseInOutQuad);
        barChart.invalidate();
    }

}
