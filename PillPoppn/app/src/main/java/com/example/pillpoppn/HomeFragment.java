package com.example.pillpoppn;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment {

    private TextView quantityTextView1, quantityTextView2, quantityTextView3, quantityTextView4;
    private TextView pill1TextView, pill2TextView, pill3TextView, pill4TextView; // Updated UI elements
    private Button buttonPlus1, buttonPlus2, buttonPlus3, buttonPlus4;
    private Button buttonMinus1, buttonMinus2, buttonMinus3, buttonMinus4;

    private int quantity1 = 0, quantity2 = 0, quantity3 = 0, quantity4 = 0;
    private static final String CHANNEL_ID = "alarm_channel";
    private static final int NOTIFICATION_ID = 123;

    private DatabaseReference databasePills;
    private DatabaseReference headerReference; // New reference for names

    public HomeFragment() {
        // Required empty public constructor
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase Database references
        databasePills = FirebaseDatabase.getInstance().getReference("pills");
        headerReference = FirebaseDatabase.getInstance().getReference("headers"); // New reference for pill container names

        // Initialize views for pill quantities
        quantityTextView1 = rootView.findViewById(R.id.quantityTextView1);
        pill1TextView = rootView.findViewById(R.id.nameTextView1); // Updated view for pill container name
        buttonPlus1 = rootView.findViewById(R.id.buttonPlus1);
        buttonMinus1 = rootView.findViewById(R.id.buttonMinus1);

        quantityTextView2 = rootView.findViewById(R.id.quantityTextView2);
        pill2TextView = rootView.findViewById(R.id.nameTextView2); // Updated view for pill container name
        buttonPlus2 = rootView.findViewById(R.id.buttonPlus2);
        buttonMinus2 = rootView.findViewById(R.id.buttonMinus2);

        quantityTextView3 = rootView.findViewById(R.id.quantityTextView3);
        pill3TextView = rootView.findViewById(R.id.nameTextView3); // Updated view for pill container name
        buttonPlus3 = rootView.findViewById(R.id.buttonPlus3);
        buttonMinus3 = rootView.findViewById(R.id.buttonMinus3);

        quantityTextView4 = rootView.findViewById(R.id.quantityTextView4);
        pill4TextView = rootView.findViewById(R.id.nameTextView4); // Updated view for pill container name
        buttonPlus4 = rootView.findViewById(R.id.buttonPlus4);
        buttonMinus4 = rootView.findViewById(R.id.buttonMinus4);

        // Set onClickListeners for buttons
        buttonPlus1.setOnClickListener(v -> increaseQuantity(1));
        buttonMinus1.setOnClickListener(v -> decreaseQuantity(1));
        buttonPlus2.setOnClickListener(v -> increaseQuantity(2));
        buttonMinus2.setOnClickListener(v -> decreaseQuantity(2));
        buttonPlus3.setOnClickListener(v -> increaseQuantity(3));
        buttonMinus3.setOnClickListener(v -> decreaseQuantity(3));
        buttonPlus4.setOnClickListener(v -> increaseQuantity(4));
        buttonMinus4.setOnClickListener(v -> decreaseQuantity(4));

        // Create notification channel
        createNotificationChannel(requireContext());

        // Set up Firebase listeners for quantities and names
        setupFirebaseListeners();
        loadHeadersFromFirebase(); // Load pill container names from Firebase

        return rootView;
    }

    private void loadHeadersFromFirebase() {
        headerReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (getContext() == null) return; // Ensure the fragment is attached

                if (snapshot.exists()) {
                    pill1TextView.setText(snapshot.child("pill1").getValue(String.class));
                    pill2TextView.setText(snapshot.child("pill2").getValue(String.class));
                    pill3TextView.setText(snapshot.child("pill3").getValue(String.class));
                    pill4TextView.setText(snapshot.child("pill4").getValue(String.class));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load headers: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void increaseQuantity(int counter) {
        switch (counter) {
            case 1:
                if (quantity1 < 10) {
                    quantity1++;
                    quantityTextView1.setText(String.valueOf(quantity1));
                    if (quantity1 == 10) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Pill Container 1 FULL", Toast.LENGTH_SHORT).show();
                        }
                        showNotification("Pill Container 1 FULL", "Your Pill Container 1 is now full.");
                    }
                    databasePills.child("pill1").setValue(quantity1);
                }
                break;
            case 2:
                if (quantity2 < 10) {
                    quantity2++;
                    quantityTextView2.setText(String.valueOf(quantity2));
                    if (quantity2 == 10) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Pill Container 2 FULL", Toast.LENGTH_SHORT).show();
                        }
                        showNotification("Pill Container 2 FULL", "Your Pill Container 2 is now full.");
                    }
                    databasePills.child("pill2").setValue(quantity2);
                }
                break;
            case 3:
                if (quantity3 < 10) {
                    quantity3++;
                    quantityTextView3.setText(String.valueOf(quantity3));
                    if (quantity3 == 10) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Pill Container 3 FULL", Toast.LENGTH_SHORT).show();
                        }
                        showNotification("Pill Container 3 FULL", "Your Pill Container 3 is now full.");
                    }
                    databasePills.child("pill3").setValue(quantity3);
                }
                break;
            case 4:
                if (quantity4 < 10) {
                    quantity4++;
                    quantityTextView4.setText(String.valueOf(quantity4));
                    if (quantity4 == 10) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Pill Container 4 FULL", Toast.LENGTH_SHORT).show();
                        }
                        showNotification("Pill Container 4 FULL", "Your Pill Container 4 is now full.");
                    }
                    databasePills.child("pill4").setValue(quantity4);
                }
                break;
        }
    }

    private void decreaseQuantity(int counter) {
        switch (counter) {
            case 1:
                if (quantity1 > 0) {
                    quantity1--;
                    quantityTextView1.setText(String.valueOf(quantity1));
                    if (quantity1 == 3) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Pill Container 1 LOW", Toast.LENGTH_SHORT).show();
                        }
                        showNotification("Pill Container 1 LOW", "Your Pill Container 1 is running low.");
                    }
                    databasePills.child("pill1").setValue(quantity1);
                }
                break;
            case 2:
                if (quantity2 > 0) {
                    quantity2--;
                    quantityTextView2.setText(String.valueOf(quantity2));
                    if (quantity2 == 3) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Pill Container 2 LOW", Toast.LENGTH_SHORT).show();
                        }
                        showNotification("Pill Container 2 LOW", "Your Pill Container 2 is running low.");
                    }
                    databasePills.child("pill2").setValue(quantity2);
                }
                break;
            case 3:
                if (quantity3 > 0) {
                    quantity3--;
                    quantityTextView3.setText(String.valueOf(quantity3));
                    if (quantity3 == 3) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Pill Container 3 LOW", Toast.LENGTH_SHORT).show();
                        }
                        showNotification("Pill Container 3 LOW", "Your Pill Container 3 is running low.");
                    }
                    databasePills.child("pill3").setValue(quantity3);
                }
                break;
            case 4:
                if (quantity4 > 0) {
                    quantity4--;
                    quantityTextView4.setText(String.valueOf(quantity4));
                    if (quantity4 == 3) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Pill Container 4 LOW", Toast.LENGTH_SHORT).show();
                        }
                        showNotification("Pill Container 4 LOW", "Your Pill Container 4 is running low.");
                    }
                    databasePills.child("pill4").setValue(quantity4);
                }
                break;
        }
    }

    private void showNotification(String title, String message) {
        // Check if the fragment is attached to a context
        if (getContext() == null) {
            return; // Don't proceed if the fragment is not attached to a context
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (getContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                return; // Don't show notification if permission is not granted
            }
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Alarm Channel";
            String description = "Channel for alarm notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void setupFirebaseListeners() {
        databasePills.child("pill1").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (getContext() == null) return; // Ensure the fragment is attached

                quantity1 = dataSnapshot.getValue(Integer.class);
                quantityTextView1.setText(String.valueOf(quantity1));

                if (quantity1 == 10) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Pill Container 1 FULL", Toast.LENGTH_SHORT).show();
                    }
                    showNotification("Pill Container 1 FULL", "Your Pill Container 1 is now full.");
                } else if (quantity1 == 3) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Pill Container 1 LOW", Toast.LENGTH_SHORT).show();
                    }
                    showNotification("Pill Container 1 LOW", "Your Pill Container 1 is running low.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });

        databasePills.child("pill2").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (getContext() == null) return; // Ensure the fragment is attached

                quantity2 = dataSnapshot.getValue(Integer.class);
                quantityTextView2.setText(String.valueOf(quantity2));

                if (quantity2 == 10) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Pill Container 2 FULL", Toast.LENGTH_SHORT).show();
                    }
                    showNotification("Pill Container 2 FULL", "Your Pill Container 2 is now full.");
                } else if (quantity2 == 3) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Pill Container 2 LOW", Toast.LENGTH_SHORT).show();
                    }
                    showNotification("Pill Container 2 LOW", "Your Pill Container 2 is running low.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });

        databasePills.child("pill3").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (getContext() == null) return; // Ensure the fragment is attached

                quantity3 = dataSnapshot.getValue(Integer.class);
                quantityTextView3.setText(String.valueOf(quantity3));

                if (quantity3 == 10) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Pill Container 3 FULL", Toast.LENGTH_SHORT).show();
                    }
                    showNotification("Pill Container 3 FULL", "Your Pill Container 3 is now full.");
                } else if (quantity3 == 3) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Pill Container 3 LOW", Toast.LENGTH_SHORT).show();
                    }
                    showNotification("Pill Container 3 LOW", "Your Pill Container 3 is running low.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });

        databasePills.child("pill4").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (getContext() == null) return; // Ensure the fragment is attached

                quantity4 = dataSnapshot.getValue(Integer.class);
                quantityTextView4.setText(String.valueOf(quantity4));

                if (quantity4 == 10) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Pill Container 4 FULL", Toast.LENGTH_SHORT).show();
                    }
                    showNotification("Pill Container 4 FULL", "Your Pill Container 4 is now full.");
                } else if (quantity4 == 3) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Pill Container 4 LOW", Toast.LENGTH_SHORT).show();
                    }
                    showNotification("Pill Container 4 LOW", "Your Pill Container 4 is running low.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });
    }
}
