package com.example.nuru;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import java.util.HashMap;
import java.util.List;

public class CustomExpandableListAdapter extends BaseExpandableListAdapter {
    private static final String TAG = "CustomExpandableAdapter";

    private Context context;
    private List<String> listGroup;
    private HashMap<String, List<String>> listChild;
    private SparseBooleanArray selectedItems;

    public CustomExpandableListAdapter(Context context, List<String> listGroup, HashMap<String, List<String>> listChild) {
        this.context = context;
        this.listGroup = listGroup;
        this.listChild = listChild;
        this.selectedItems = new SparseBooleanArray();
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return listChild.get(listGroup.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final String childText = (String) getChild(groupPosition, childPosition);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item, null);
            Log.d(TAG, "getChildView: Creating new view for child");
        }
        TextView textViewChild = convertView.findViewById(R.id.textViewChild);
        textViewChild.setText(childText);

        if (isSelected(groupPosition, childPosition)) {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.yellow));
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }
        return convertView;
    }


    @Override
    public int getChildrenCount(int groupPosition) {
        return listChild.get(listGroup.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return listGroup.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return listGroup.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String groupTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_group, null);
            Log.d(TAG, "getGroupView: Creating new view for group");
        }
        TextView textViewGroup = convertView.findViewById(R.id.textViewGroup);
        textViewGroup.setText(groupTitle);

        if (isSelected(groupPosition, -1)) {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.yellow));
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public boolean isSelected(int groupPosition, int childPosition) {
        int key = childPosition >= 0 ? groupPosition *1000 + childPosition : groupPosition;
        return selectedItems.get(key);
    }

    public void setSelected(int groupPosition, int childPosition, boolean isSelected) {
        int key = childPosition >= 0 ? groupPosition * 1000 + childPosition : groupPosition; // Adjusted logic for selected items
        selectedItems.put(key, isSelected);
        notifyDataSetChanged();
    }

}
