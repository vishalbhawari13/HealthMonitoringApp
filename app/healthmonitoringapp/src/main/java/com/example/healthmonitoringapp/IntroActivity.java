package com.example.healthmonitoringapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class IntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        // Initialize UI components
        TextView welcomeText = findViewById(R.id.welcomeText);
        Button startButton = findViewById(R.id.startButton);

        // Set welcome message
        welcomeText.setText("Welcome to PulseGuard");

        // Button click listener to navigate to MainActivity
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(IntroActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close the intro activity
        });
    }
}
