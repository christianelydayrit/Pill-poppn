package com.example.pillpoppn;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.Manifest;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "alarm_channel";
    private static final int NOTIFICATION_ID = 123;

    @Override
    public void onReceive(Context context, Intent intent) {
        int pillNumber = intent.getIntExtra("pillNumber", -1);
        int alarmNumber = intent.getIntExtra("alarmNumber", -1);

        if (pillNumber != -1) {
            fetchPillNamesAndNotify(context, pillNumber);
            updatePillCount(context, pillNumber); // New method to update pill count
        }

        logAlarmEvent(pillNumber, alarmNumber);
    }

    private void fetchPillNamesAndNotify(Context context, int pillNumber) {
        DatabaseReference pillNamesRef = FirebaseDatabase.getInstance().getReference("headers"); // Assuming "headers" is where pill names are stored
        pillNamesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String pillName = "Pill " + pillNumber; // Default name
                if (snapshot.exists()) {
                    switch (pillNumber) {
                        case 1:
                            pillName = snapshot.child("pill1").getValue(String.class);
                            break;
                        case 2:
                            pillName = snapshot.child("pill2").getValue(String.class);
                            break;
                        case 3:
                            pillName = snapshot.child("pill3").getValue(String.class);
                            break;
                        case 4:
                            pillName = snapshot.child("pill4").getValue(String.class);
                            break;
                    }
                }
                showNotification(context, pillName); // Show notification with the retrieved name
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error
            }
        });
    }

    private void showNotification(Context context, String pillName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Pop IT")
                .setContentText("Time to take your " + pillName + "!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannel(context);
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Intent permissionIntent = new Intent("com.example.alarmandnotif.REQUEST_NOTIFICATION_PERMISSION");
            context.sendBroadcast(permissionIntent);
        } else {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void logAlarmEvent(int pillNumber, int alarmNumber) {
        DatabaseReference pillNamesRef = FirebaseDatabase.getInstance().getReference("headers");

        pillNamesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String pillName = "Pill " + pillNumber;
                if (snapshot.exists()) {
                    switch (pillNumber) {
                        case 1:
                            pillName = snapshot.child("pill1").getValue(String.class);
                            break;
                        case 2:
                            pillName = snapshot.child("pill2").getValue(String.class);
                            break;
                        case 3:
                            pillName = snapshot.child("pill3").getValue(String.class);
                            break;
                        case 4:
                            pillName = snapshot.child("pill4").getValue(String.class);
                            break;
                    }
                }

                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm MM/dd/yyyy", Locale.getDefault());
                String timeStamp = dateFormat.format(calendar.getTime());

                String logMessage = pillName + " dispensed at " + timeStamp;

                DatabaseReference logRef = FirebaseDatabase.getInstance().getReference("Logs");

                logRef.push().setValue(logMessage).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("AlarmReceiver", "Log successfully written to Firebase");
                    } else {
                        Log.e("AlarmReceiver", "Failed to write log to Firebase");
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("AlarmReceiver", "Failed to read pill names from Firebase", error.toException());
            }
        });
    }

    // New method to decrement the pill count when the alarm goes off
    private void updatePillCount(Context context, int pillNumber) {
        DatabaseReference pillRef = FirebaseDatabase.getInstance().getReference("pills");

        pillRef.child("pill" + pillNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int currentQuantity = snapshot.getValue(Integer.class);
                    if (currentQuantity > 0) {
                        pillRef.child("pill" + pillNumber).setValue(currentQuantity - 1)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(context, "Pill " + pillNumber + " count updated.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Log.e("AlarmReceiver", "Failed to update pill count.");
                                    }
                                });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("AlarmReceiver", "Failed to read pill count from Firebase", error.toException());
            }
        });
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Alarm Notification";
            String description = "Channel for alarm notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
