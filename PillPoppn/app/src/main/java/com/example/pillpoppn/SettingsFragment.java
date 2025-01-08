package com.example.pillpoppn;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.text.TextUtils;
import android.widget.LinearLayout;

public class SettingsFragment extends Fragment {

    private List<List<Calendar>> alarmTimesList = new ArrayList<>();
    private List<ArrayAdapter<String>> alarmAdapterList = new ArrayList<>();
    private EditText pill1Header, pill2Header, pill3Header, pill4Header;
    private Button btnSetAlarm1, btnSetAlarm2, btnSetAlarm3, btnSetAlarm4;
    private Button btnEditHeaders;

    // Firebase references
    private DatabaseReference databaseReference;
    private DatabaseReference[] alarmRefs = new DatabaseReference[4];
    private DatabaseReference headerReference;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize Firebase references
        databaseReference = FirebaseDatabase.getInstance().getReference("alarms");
        headerReference = FirebaseDatabase.getInstance().getReference("headers");

        // Initialize Firebase references for each pill container
        alarmRefs[0] = databaseReference.child("pill1");
        alarmRefs[1] = databaseReference.child("pill2");
        alarmRefs[2] = databaseReference.child("pill3");
        alarmRefs[3] = databaseReference.child("pill4");

        // Find EditTexts
        pill1Header = view.findViewById(R.id.pill1Header);
        pill2Header = view.findViewById(R.id.pill2Header);
        pill3Header = view.findViewById(R.id.pill3Header);
        pill4Header = view.findViewById(R.id.pill4Header);

        // Find Buttons
        btnSetAlarm1 = view.findViewById(R.id.btnSetAlarm1);
        btnSetAlarm2 = view.findViewById(R.id.btnSetAlarm2);
        btnSetAlarm3 = view.findViewById(R.id.btnSetAlarm3);
        btnSetAlarm4 = view.findViewById(R.id.btnSetAlarm4);
        btnEditHeaders = view.findViewById(R.id.btnEditNames);

        // Load alarms and headers from Firebase
        loadAlarmsFromFirebase();
        loadHeadersFromFirebase();

        // Initialize adapters and set them to their respective list views
        for (int i = 1; i <= 4; i++) {
            ListView listView = findListViewById(view, i);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.list_item_alarm);
            listView.setAdapter(adapter);
            alarmAdapterList.add(adapter);
            alarmTimesList.add(new ArrayList<>());
            setLongClickListener(listView);
            setClickListener(i);
        }

        // Set up the edit button click listener
        btnEditHeaders.setOnClickListener(v -> toggleEditMode());

        return view;
    }

    private void loadHeadersFromFirebase() {
        headerReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    pill1Header.setText(snapshot.child("pill1").getValue(String.class));
                    pill2Header.setText(snapshot.child("pill2").getValue(String.class));
                    pill3Header.setText(snapshot.child("pill3").getValue(String.class));
                    pill4Header.setText(snapshot.child("pill4").getValue(String.class));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load headers: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveHeadersToFirebase() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("pill1", pill1Header.getText().toString());
        headers.put("pill2", pill2Header.getText().toString());
        headers.put("pill3", pill3Header.getText().toString());
        headers.put("pill4", pill4Header.getText().toString());

        headerReference.updateChildren(headers)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), "Headers updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to update headers", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void toggleEditMode() {
        boolean isEditable = !pill1Header.isFocusable(); // Determine if we are in edit mode or not

        // Toggle the edit mode
        setEditMode(isEditable);

        if (isEditable) {
            btnEditHeaders.setText("Save Headers");
        } else {
            // Save changes to Firebase
            saveHeadersToFirebase();
            btnEditHeaders.setText("Edit Headers");
        }
    }

    private void setEditMode(boolean isEditable) {
        pill1Header.setFocusable(isEditable);
        pill1Header.setFocusableInTouchMode(isEditable);
        pill1Header.setClickable(isEditable);
        pill1Header.setEnabled(isEditable);

        pill2Header.setFocusable(isEditable);
        pill2Header.setFocusableInTouchMode(isEditable);
        pill2Header.setClickable(isEditable);
        pill2Header.setEnabled(isEditable);

        pill3Header.setFocusable(isEditable);
        pill3Header.setFocusableInTouchMode(isEditable);
        pill3Header.setClickable(isEditable);
        pill3Header.setEnabled(isEditable);

        pill4Header.setFocusable(isEditable);
        pill4Header.setFocusableInTouchMode(isEditable);
        pill4Header.setClickable(isEditable);
        pill4Header.setEnabled(isEditable);
    }

    private void loadAlarmsFromFirebase() {
        for (int i = 0; i < 4; i++) {
            int finalI = i;
            alarmRefs[i].addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        alarmAdapterList.get(finalI).clear();
                        alarmTimesList.get(finalI).clear();
                        for (DataSnapshot alarmSnapshot : snapshot.getChildren()) {
                            String alarmTime = alarmSnapshot.getValue(String.class);
                            if (alarmTime != null) {
                                alarmAdapterList.get(finalI).add(alarmTime);
                            }
                        }
                        alarmAdapterList.get(finalI).notifyDataSetChanged(); // Notify adapter of data change
                    } else {
                        Toast.makeText(requireContext(), "No alarms found for pill" + (finalI + 1), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(requireContext(), "Failed to load alarms: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private ListView findListViewById(View view, int buttonNumber) {
        switch (buttonNumber) {
            case 1:
                return view.findViewById(R.id.listViewAlarms1);
            case 2:
                return view.findViewById(R.id.listViewAlarms2);
            case 3:
                return view.findViewById(R.id.listViewAlarms3);
            case 4:
                return view.findViewById(R.id.listViewAlarms4);
            default:
                return null;
        }
    }

    private void setLongClickListener(ListView listView) {
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            int buttonNumber = getButtonNumber(listView);

            if (buttonNumber != -1 && position >= 0 && position < alarmAdapterList.get(buttonNumber - 1).getCount()) {
                cancelAlarm(buttonNumber, position);
                removeAlarm(buttonNumber, position);
                alarmAdapterList.get(buttonNumber - 1).notifyDataSetChanged();
            } else {
                Toast.makeText(requireContext(), "Unable to delete the alarm", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    private void setClickListener(final int buttonNumber) {
        Button button = findButtonById(buttonNumber);
        if (button != null) {
            button.setOnClickListener(v -> openTimePickerDialog(buttonNumber));
        }
    }

    private Button findButtonById(int buttonNumber) {
        switch (buttonNumber) {
            case 1:
                return btnSetAlarm1;
            case 2:
                return btnSetAlarm2;
            case 3:
                return btnSetAlarm3;
            case 4:
                return btnSetAlarm4;
            default:
                return null;
        }
    }

    private void openTimePickerDialog(final int buttonNumber) {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Alarm Time")
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build();

        timePicker.show(requireActivity().getSupportFragmentManager(), "timePicker");
        timePicker.addOnPositiveButtonClickListener(view -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();
            String time = getTimeString(hour, minute);
            setAlarm(buttonNumber, hour, minute);
            addAlarmToList(buttonNumber, time);
            saveAlarmToFirebase(buttonNumber, time);
            Toast.makeText(requireContext(), "Alarm Set for " + time, Toast.LENGTH_SHORT).show();
        });
    }

    private String getTimeString(int hour, int minute) {
        if (hour > 12) {
            return String.format("%02d", (hour - 12)) + ":" + String.format("%02d", minute) + " PM";
        } else {
            return String.format("%02d", hour) + ":" + String.format("%02d", minute) + " AM";
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void setAlarm(int buttonNumber, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), AlarmReceiver.class);

        intent.putExtra("pillNumber", buttonNumber);
        intent.putExtra("alarmNumber", alarmTimesList.get(buttonNumber - 1).size() + 1);

        int requestCode = (buttonNumber * 1000) + alarmTimesList.get(buttonNumber - 1).size() + 1;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmTimesList.get(buttonNumber - 1).add(calendar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void cancelAlarm(int buttonNumber, int position) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), AlarmReceiver.class);

        List<Calendar> alarmTimes = alarmTimesList.get(buttonNumber - 1);

        if (position >= 0 && position < alarmTimes.size()) {
            Calendar alarmTime = alarmTimes.get(position);
            int requestCode = (buttonNumber * 1000) + position + 1;
            PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE);

            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();

            alarmTimes.remove(position);
        }
    }

    private void addAlarmToList(int buttonNumber, String time) {
        alarmAdapterList.get(buttonNumber - 1).add(time);
    }

    private void saveAlarmToFirebase(int buttonNumber, String time) {
        int index = alarmAdapterList.get(buttonNumber - 1).getCount() - 1;
        alarmRefs[buttonNumber - 1].push().setValue(time).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(requireContext(), "Alarm saved to Firebase", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to save alarm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeAlarm(int buttonNumber, int position) {
        DatabaseReference alarmRef = alarmRefs[buttonNumber - 1];

        alarmRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<DataSnapshot> alarms = new ArrayList<>();
                    for (DataSnapshot alarmSnapshot : snapshot.getChildren()) {
                        alarms.add(alarmSnapshot);
                    }

                    if (position < alarms.size()) {
                        DataSnapshot alarmToRemove = alarms.get(position);
                        String keyToRemove = alarmToRemove.getKey();

                        alarmRef.child(keyToRemove).removeValue((error, ref) -> {
                            if (error == null) {
                                // Ensure local list is consistent with Firebase
                                ArrayAdapter<String> adapter = alarmAdapterList.get(buttonNumber - 1);
                                if (position >= 0 && position < adapter.getCount()) {
                                    String alarmToRemoveLocal = adapter.getItem(position);
                                    if (alarmToRemoveLocal != null) {
                                        adapter.remove(alarmToRemoveLocal);
                                        adapter.notifyDataSetChanged();
                                        Toast.makeText(requireContext(), "Alarm removed", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(requireContext(), "Alarm not found locally", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } else {
                                Toast.makeText(requireContext(), "Failed to remove alarm from Firebase: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    Toast.makeText(requireContext(), "No alarms found for this pill", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to read alarms from Firebase: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getButtonNumber(ListView listView) {
        int listViewId = listView.getId();
        if (listViewId == R.id.listViewAlarms1) {
            return 1;
        } else if (listViewId == R.id.listViewAlarms2) {
            return 2;
        } else if (listViewId == R.id.listViewAlarms3) {
            return 3;
        } else if (listViewId == R.id.listViewAlarms4) {
            return 4;
        } else {
            return -1; // Invalid ID
        }
    }
}
