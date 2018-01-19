package com.jamierajewski.sensorcompanion;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class CalibrationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
    }

    public void newConnection(View view){
        Intent intent = new Intent(this, ConnectionActivity.class);
        startActivity(intent);
    }
}
