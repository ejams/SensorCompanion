package com.jamierajewski.sensorcompanion;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

public class CalibrationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
        populateSpinner();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Now that we have returned to the mode selection, refresh the spinner
        populateSpinner();
    }

    public void populateSpinner(){
        // Create and/or open file containing different modes
        SharedPreferences prefs = getSharedPreferences("modeFile", MODE_PRIVATE);
        // Check if voltage exists; if not, add it in as a default
        if (!prefs.contains("Voltage")){
            SharedPreferences.Editor editor = getSharedPreferences("modeFile", MODE_PRIVATE).edit();
            editor.putString("Voltage", "y=x");
            editor.apply();
        }
        // Get all entries so that we can pull just the keys out to populate the spinner
        ArrayList<String> tempList = new ArrayList<>();
        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()){
            tempList.add(entry.getKey());
        }

        // Once all keys are pulled, convert the temporary array list to an array to be used
        // with the spinner
        String[] arraySpinner = new String[tempList.size()];
        arraySpinner = tempList.toArray(arraySpinner);

        Spinner spinner = findViewById(R.id.modeSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, arraySpinner);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        // Set the default value to position 0
        /// POSSIBLE CRASH IF THERE ARE NO ELEMENTS IN THE SPINNER? ///
        spinner.setSelection(0,true);
    }

    public void connect(View view){
        /// WHEN MOVING ON WITH A VALID SELECTION, PASS THE FORMULA ///
        Intent intent = new Intent(this, DeviceList.class);
        startActivity(intent);
    }

    public void addMode(View view) {
        Intent intent = new Intent(this, AddModeActivity.class);
        startActivity(intent);
    }

    public void editMode(View view) {
        // Get the currently selected item and pass it with the intent
        Spinner spinner = findViewById(R.id.modeSpinner);
        // Since Voltage cannot be removed, it'll always be selected in the case of no selection
        String mode = spinner.getSelectedItem().toString();

        if (mode.matches("Voltage")){
            Toast.makeText(this, "Cannot edit default Voltage", Toast.LENGTH_LONG).show();
        }

        else {
            Intent intent = new Intent(this, EditMode.class);
            intent.putExtra("item", mode);
            startActivity(intent);
        }
    }
}
