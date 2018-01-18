package com.jamierajewski.sensorcompanion;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    //public static final String EXTRA_MESSAGE = "com.jamierajewski.sensorcompanion.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void newMeasurement(View view){
        Intent intent = new Intent(this, CalibrationActivity.class);
        startActivity(intent);
    }
}
