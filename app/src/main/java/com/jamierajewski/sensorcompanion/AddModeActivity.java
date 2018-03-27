package com.jamierajewski.sensorcompanion;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class AddModeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mode);
    }

    // ***BEFORE ALLOWING USER TO SUBMIT, ATTEMPT TO PARSE IT AND TEST IT TO ENSURE IT IS VALID***
    public void submitMode(View view){

        SharedPreferences prefs = getSharedPreferences("modeFile", MODE_PRIVATE);

        // Fetch both fields
        EditText name_text = findViewById(R.id.editNameTextbox);
        String name = name_text.getText().toString().trim();
        if (prefs.contains(name)){
            Toast.makeText(this, "That name already exists, please choose another", Toast.LENGTH_LONG).show();
        }

        else {
            EditText formula_text = findViewById(R.id.editFormulaTextbox);
            String formula = formula_text.getText().toString().trim();

            // Verify that both textboxes are not empty, otherwise cancel the submission
            if (name.matches("") || formula.matches("")){
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            }

            else {
                // Now that we have the items, store them in the shared preferences file
                SharedPreferences.Editor editor = getSharedPreferences("modeFile", MODE_PRIVATE).edit();

                editor.putString(name, formula);
                editor.apply();
                finish();
            }
        }
    }
}
