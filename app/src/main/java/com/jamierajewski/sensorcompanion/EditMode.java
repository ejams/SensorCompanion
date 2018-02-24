package com.jamierajewski.sensorcompanion;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class EditMode extends AppCompatActivity {

    String mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_mode);

        // Retrieve the passed in key from the calibration screen
        Bundle bundle = getIntent().getExtras();
        mode = bundle.getString("item");

        // Set the textbox for name to the passed in name
        EditText name = findViewById(R.id.editNameTextbox);
        name.setText(mode);

        // To get the associated formula, we have to pull it from sharedpreferences
        EditText formula = findViewById(R.id.editFormulaTextbox);
        SharedPreferences prefs = getSharedPreferences("modeFile", MODE_PRIVATE);
        String formula_text = prefs.getString(mode, null);
        formula.setText(formula_text);
    }

    public void submitEntry(View view){

        // Delete the old entry
        SharedPreferences.Editor editor = getSharedPreferences("modeFile", MODE_PRIVATE).edit();
        SharedPreferences prefs = getSharedPreferences("modeFile", MODE_PRIVATE);

        // Pull the new info
        EditText name = findViewById(R.id.editNameTextbox);
        String name_text = name.getText().toString();

        // Check if the name exists AND that the name being replaced is not that one
        if (prefs.contains(name_text) && !name_text.matches(mode)){
            Toast.makeText(this, "That name already exists, please choose another", Toast.LENGTH_LONG).show();
        }

        else{
            editor.remove(mode);
            EditText formula = findViewById(R.id.editFormulaTextbox);
            String formula_text = formula.getText().toString();

            // Insert into file and apply
            editor.putString(name_text, formula_text);

            editor.apply();

            finish();
        }
    }

    public void deleteEntry(View view){

        // Create an alert dialog to warn the user about deletion
        AlertDialog alertDialog = new AlertDialog.Builder(EditMode.this).create();
        alertDialog.setTitle("WARNING");
        alertDialog.setMessage("Are you sure you want to delete this entry?");
        alertDialog.setButton(alertDialog.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // DELETE THE ENTRY
                        SharedPreferences.Editor editor = getSharedPreferences("modeFile", MODE_PRIVATE).edit();
                        if (mode.matches("Voltage")){
                            Toast.makeText(EditMode.this, "Cannot remove Voltage default", Toast.LENGTH_SHORT).show();
                        }

                        else {
                            editor.remove(mode);
                            editor.apply();
                            finish();
                        }
                    }
                });
        alertDialog.setButton(alertDialog.BUTTON_NEGATIVE, "No",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        alertDialog.dismiss();
                    }
                });
        alertDialog.show();
    }
}
