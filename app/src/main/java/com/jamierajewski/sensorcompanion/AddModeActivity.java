package com.jamierajewski.sensorcompanion;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.udojava.evalex.Expression;

import java.util.HashSet;
import java.util.Set;

public class AddModeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mode);
    }

    // Run a simple test on the user-added formula to ensure its validity
    private boolean validFormula(String formula){
        try{
            formula = (formula.split("="))[1];
            Expression expression = new Expression(formula).with("x", "2.41");
            expression.eval();
            return true;

        } catch (Exception ex){
            return false;
        }
    }

    public void helpMenu(View view){
        Intent i = new Intent(AddModeActivity.this, HelpActivity.class);
        startActivity(i);
    }

    // ***BEFORE ALLOWING USER TO SUBMIT, ATTEMPT TO PARSE IT AND TEST IT TO ENSURE IT IS VALID***
    public void submitMode(View view){

        SharedPreferences prefs = getSharedPreferences("modeFile", MODE_PRIVATE);

        // Fetch both fields
        EditText name_text = findViewById(R.id.addNameTextbox);
        String name = name_text.getText().toString().trim();

        if (prefs.contains(name)){
            Toast.makeText(this, "That name already exists, please choose another", Toast.LENGTH_LONG).show();
        }

        else {
            EditText formula_text = findViewById(R.id.addFormulaTextbox);
            String formula = formula_text.getText().toString().trim();

            // Verify that both textboxes are not empty, otherwise cancel the submission
            if (name.matches("") || formula.matches("")){
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            }

            else if (!validFormula(formula)){
                Toast.makeText(this, "Formula invalid, please validate your expression", Toast.LENGTH_LONG).show();
            }

            // If the formula passes the validation test, continue
            else{
                // Now that we have the items, store them in the shared preferences file
                SharedPreferences.Editor editor = getSharedPreferences("modeFile", MODE_PRIVATE).edit();

                EditText min = findViewById(R.id.addMinTextbox);
                String minText = min.getText().toString();
                EditText max = findViewById(R.id.addMaxTextbox);
                String maxText = max.getText().toString();

                editor.putString(name, formula);
                // Absolutely filthy and probably not the best way to do it (could've stored as one string separated by
                // delimiter and then split/cast later?) but itll probably work
                editor.putFloat(name+"_", Float.parseFloat(minText));
                editor.putFloat(name+"__", Float.parseFloat(maxText));
                editor.apply();
                finish();
            }
        }
    }
}
