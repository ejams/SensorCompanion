package com.jamierajewski.sensorcompanion;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.udojava.evalex.Expression;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CalibrationActivity extends AppCompatActivity {

    public static String MODE = "formula";
    public static String FUNCTION = "function";
    public static String MIN = "min";
    public static String MAX = "max";

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
            editor.putFloat("Voltage_", 0.0f);
            editor.putFloat("Voltage__", 5.0f);
            editor.apply();
        }
        // Get all entries so that we can pull just the keys out to populate the spinner
        ArrayList<String> tempList = new ArrayList<>();
        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()){
            // Filter out the entries that are for min/max
            if (!entry.getKey().endsWith("_")){
                tempList.add(entry.getKey());
            }
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
        spinner.setSelection(0,true);
    }

    public void connect(View view){
        Spinner spinner = findViewById(R.id.modeSpinner);
        String mode = spinner.getSelectedItem().toString();

        // Get the associated formula with the selection
        SharedPreferences prefs = getSharedPreferences("modeFile", MODE_PRIVATE);
        String formula_text = prefs.getString(mode, null);
        float min = prefs.getFloat(mode+"_", 0.0f);
        float max = prefs.getFloat(mode+"__", 0.0f);

        // Only take the expression after the '=' to evaluate
        formula_text = (formula_text.split("="))[1];

        Intent intent = new Intent(this, DeviceListActivity.class);
        intent.putExtra(MODE, formula_text);
        intent.putExtra(FUNCTION, mode);
        intent.putExtra(MIN, min);
        intent.putExtra(MAX, max);
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
            Intent intent = new Intent(this, EditModeActivity.class);
            intent.putExtra("item", mode);
            startActivity(intent);
        }
    }
}
