package com.example.nuru;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class MoodsFragment extends Fragment {
    private static final String TAG = "MoodsFragment";
    private RecyclerView moodRecyclerView;
    private ExpandableListView sensoryListView;
    private ExpandableListView copingListView;
    private CustomExpandableListAdapter sensoryListAdapter;
    private CustomExpandableListAdapter copingListAdapter;
    private List<String> sensoryListGroup;
    private List<String> copingListGroup;
    private HashMap<String, List<String>> sensoryListChild;
    private HashMap<String, List<String>> copingListChild;
    private MoodAdapter moodAdapter;
    private FirebaseFirestore db;

    private Calendar selectedDate;

    private Slider emotionIntensitySlider;
    private Slider copingEffectivenessSlider;
    private Button saveBtn;
    private EditText personalNotes;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: started");
        View view = inflater.inflate(R.layout.fragment_moods, container, false);

        db = FirebaseFirestore.getInstance();

        // Initialize mood data
        String[] moods = {"Happy", "Sad", "Angry", "Calm", "Anxious", "Tired", "Scared", "Confused"};
        int[] drawableIds = {R.drawable.happy_mood, R.drawable.sad_mood, R.drawable.angry_mood,
                R.drawable.calm, R.drawable.anxious_mood, R.drawable.tired,
                R.drawable.scared_mood, R.drawable.confused_mood};

        moodRecyclerView = view.findViewById(R.id.moodRecyclerView);
        moodAdapter = new MoodAdapter(moods, drawableIds);
        moodRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        moodRecyclerView.setAdapter(moodAdapter);
        Log.d(TAG, "onCreateView: moodRecyclerView setup completed");

//        Expandable list variables
        sensoryListView = view.findViewById(R.id.sensory_experience_list);
        copingListView = view.findViewById(R.id.coping_mechanism_list);

        sensoryListData();
        copingMechanismData();

        Bundle args = getArguments();
        if (args != null && args.containsKey("selected_date")) {
            long selectedDateMillis = args.getLong("selected_date");
            selectedDate = Calendar.getInstance();
            selectedDate.setTimeInMillis(selectedDateMillis);
        } else {
            selectedDate = Calendar.getInstance();
        }

        sensoryListAdapter = new CustomExpandableListAdapter(getActivity(), sensoryListGroup, sensoryListChild);
        copingListAdapter = new CustomExpandableListAdapter(getActivity(), copingListGroup, copingListChild);
        sensoryListView.setAdapter(sensoryListAdapter);
        copingListView.setAdapter(copingListAdapter);
        Log.d(TAG, "onCreateView: Expandable lists setup completed");

        sensoryListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            sensoryListAdapter.setSelected(groupPosition, childPosition, !sensoryListAdapter.isSelected(groupPosition, childPosition));
            return true;
        });

        copingListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            copingListAdapter.setSelected(groupPosition, childPosition, !copingListAdapter.isSelected(groupPosition, childPosition));
            return true;
        });

        emotionIntensitySlider = view.findViewById(R.id.emotionIntensitySlider);
        copingEffectivenessSlider = view.findViewById(R.id.copingEffectivenessSlider);
        saveBtn = view.findViewById(R.id.save_button);
        personalNotes = view.findViewById(R.id.personal_notes);
        saveBtn.setOnClickListener(v -> saveData());



        return view;
    }


    private void saveData() {
        String notes = personalNotes.getText().toString();
        List<String> selectedMoods = new ArrayList<>(moodAdapter.getSelectedMoods());
        List<String> selectedSensoryExperiences = getSelectedItems(sensoryListAdapter, sensoryListGroup, sensoryListChild);
        List<String> selectedCopingMechanisms = getSelectedItems(copingListAdapter, copingListGroup, copingListChild);
        int emotionIntensity = (int) emotionIntensitySlider.getValue();
        int copingEffectiveness = (int) copingEffectivenessSlider.getValue();

        // Log selected items for debugging
        Log.d(TAG, "Selected Moods: " + selectedMoods.toString());
        Log.d(TAG, "Selected Sensory Experiences: " + selectedSensoryExperiences.toString());
        Log.d(TAG, "Selected Coping Mechanisms: " + selectedCopingMechanisms.toString());

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();


        // Create a data object
        Bundle args = getArguments();
        long selectedDateMillis = System.currentTimeMillis(); // Default to current time if not specified
        if (args != null && args.containsKey("selected_date")) {
            selectedDateMillis = args.getLong("selected_date");
        }

        HashMap<String, Object> data = new HashMap<>();
        data.put("moods", selectedMoods);
        data.put("sensory_experiences", selectedSensoryExperiences);
        data.put("coping_mechanisms", selectedCopingMechanisms);
        data.put("emotion_intensity", emotionIntensity);
        data.put("coping_effectiveness", copingEffectiveness);
        data.put("notes", notes);
        data.put("date", selectedDateMillis); // Use current time as date

        // Save data to Firestore
        db.collection("users")
                .document(userId)
                .collection("mood_logs")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    Toast.makeText(getContext(),"Mood log saved succesfully", Toast.LENGTH_SHORT).show();
                    refreshFragment();

                })
                .addOnFailureListener(e -> Log.w(TAG, "Error adding document", e));

    }

    private void refreshFragment() {
        Fragment fragment = getParentFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            getParentFragmentManager().beginTransaction()
                    .detach(fragment)
                    .attach(fragment)
                    .commit();
        }
    }

    private List<String> getSelectedItems(CustomExpandableListAdapter adapter, List<String> groupList, HashMap<String, List<String>> childList) {
        List<String> selectedItems = new ArrayList<>();
        for (int i = 0; i < groupList.size(); i++) {
            for (int j = 0; j < childList.get(groupList.get(i)).size(); j++) {
                if (adapter.isSelected(i, j)) {
                    selectedItems.add(childList.get(groupList.get(i)).get(j));
                    Log.d(TAG, "Selected Child: " + childList.get(groupList.get(i)).get(j));
                }
            }
        }
        return selectedItems;
    }

    private void copingMechanismData() {
        Log.d(TAG, "copingMechanismData: started");

        copingListGroup = new ArrayList<>();
        copingListChild = new HashMap<>();

        // Adding groups
        copingListGroup.add("Sensory Regulation");
        copingListGroup.add("Environmental Modifications");
        copingListGroup.add("Physical Activities");

        // Adding child items
        List<String> sensoryRegulation = new ArrayList<>();
        sensoryRegulation.add("Using noise-cancelling headphones");
        sensoryRegulation.add("Wearing sunglasses");
        sensoryRegulation.add("Using fidget toys");
        sensoryRegulation.add("Weighted blankets or clothing");
        sensoryRegulation.add("Chewing gum or using chewy jewelry");
        sensoryRegulation.add("Deep pressure activities (e.g., squeezes, bear hugs)");

        List<String> environmentalModifications = new ArrayList<>();
        environmentalModifications.add("Finding a quiet space");
        environmentalModifications.add("Dimming lights");
        environmentalModifications.add("Organizing surroundings");
        environmentalModifications.add("Using visual schedules or timers");

        List<String> physicalActivities = new ArrayList<>();
        physicalActivities.add("Taking a walk");
        physicalActivities.add("Doing yoga or stretching");
        physicalActivities.add("Engaging in physical exercise");
        physicalActivities.add("Deep breathing exercises");
        physicalActivities.add("Progressive muscle relaxation");

        // Adding child data to HashMap
        copingListChild.put(copingListGroup.get(0), sensoryRegulation); // Sensory Regulation
        copingListChild.put(copingListGroup.get(1), environmentalModifications); // Environmental Modifications
        copingListChild.put(copingListGroup.get(2), physicalActivities); // Physical Activities
        Log.d(TAG, "copingMechanismData: completed");
    }

    private void sensoryListData() {
        Log.d(TAG, "sensoryListData: started");
        sensoryListGroup = new ArrayList<>();
        sensoryListChild = new HashMap<>();

        // Adding groups
        sensoryListGroup.add("Auditory (Sound)");
        sensoryListGroup.add("Visual");
        sensoryListGroup.add("Tactile (Touch)");

        // Adding child items
        List<String> auditory = new ArrayList<>();
        auditory.add("Loud noises");
        auditory.add("Sudden sounds");
        auditory.add("Repetitive sounds");
        auditory.add("Background noise");
        auditory.add("Specific sounds (e.g., sirens, music, voices)");

        List<String> visual = new ArrayList<>();
        visual.add("Bright lights");
        visual.add("Flashing lights");
        visual.add("Busy visual environments");
        visual.add("Specific colors or patterns");
        visual.add("Moving objects");

        List<String> tactile = new ArrayList<>();
        tactile.add("Certain textures (rough, smooth, etc.)");
        tactile.add("Light touch");
        tactile.add("Pressure");
        tactile.add("Temperature (hot or cold)");
        tactile.add("Clothing tags or seams");

        // Adding child data to HashMap
        sensoryListChild.put(sensoryListGroup.get(0), auditory); // Auditory
        sensoryListChild.put(sensoryListGroup.get(1), visual); // Visual
        sensoryListChild.put(sensoryListGroup.get(2), tactile); // Tactile (Touch)
        Log.d(TAG, "sensoryListData: completed");
    }


}