package com.jamierajewski.sensorcompanion;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.jamierajewski.sensorcompanion.R;

public class AddModeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mode);
    }

    public void submitMode(View view){

        // Fetch both fields
        EditText name_text = findViewById(R.id.modeNameTextbox);
        String name = name_text.getText().toString().trim();
        EditText formula_text = findViewById(R.id.formulaTextbox);
        String formula = formula_text.getText().toString().trim();

        Toast.makeText(this, name, Toast.LENGTH_SHORT).show();
        Toast.makeText(this, formula, Toast.LENGTH_SHORT).show();

        // Verify that both textboxes are not empty, otherwise cancel the submition
        if (name.matches("") || formula.matches("")){
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
        }

        else {
            // Now that we have the items, store them in the shared preferences file
            SharedPreferences.Editor editor = getSharedPreferences("modeFile", MODE_PRIVATE).edit();

            //// DEBUG ///// - THIS WILL DELETE ALL PREVIOUS RECORDS AND ONLY STORE THE NEWLY ADDED ONE
            editor.clear();

            editor.putString(name, formula);
            editor.apply();
            finish();
        }
    }
}
