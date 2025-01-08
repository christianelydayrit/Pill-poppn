package com.example.pillpoppn;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class TimelineFragment extends Fragment {

    private ArrayList<String> timelineItems;
    private ArrayAdapter<String> adapter;

    private int defaultColor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_timeline, container, false);

        // Initialize timeline items and adapter
        timelineItems = new ArrayList<>();
        ListView listView = rootView.findViewById(R.id.timelineListView);

        defaultColor = ContextCompat.getColor(requireContext(), R.color.timelinepills); // Set your default color here

        adapter = new ArrayAdapter<String>(requireActivity(), android.R.layout.simple_list_item_1, timelineItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Get the current item from the ArrayAdapter
                View view = super.getView(position, convertView, parent);
                String item = getItem(position);

                // Apply default color
                if (item != null) {
                    view.setBackgroundColor(defaultColor);
                }
                return view;
            }
        };

        listView.setAdapter(adapter);

        // Load logs from Firebase
        loadLogsFromFirebase();

        return rootView;
    }

    private void loadLogsFromFirebase() {
        DatabaseReference logsRef = FirebaseDatabase.getInstance().getReference("Logs");

        logsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                timelineItems.clear(); // Clear the list to avoid duplicates

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String log = snapshot.getValue(String.class);

                    if (log != null) {
                        Log.d("TimelineFragment", "Log retrieved: " + log); // Debugging
                        timelineItems.add(log); // Add log to the list
                    }
                }

                adapter.notifyDataSetChanged(); // Notify adapter to update the ListView
                Log.d("TimelineFragment", "Timeline updated with " + timelineItems.size() + " logs.");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("TimelineFragment", "Error loading logs: " + databaseError.getMessage()); // Log errors
            }
        });
    }
}
