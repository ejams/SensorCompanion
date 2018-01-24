package com.jamierajewski.sensorcompanion;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ConnectionActivity extends AppCompatActivity {

    // ***FOR WRITING CONTINUOUS DATA, OPEN FILE IN THE onCreate AND CLOSE IT WHEN
    // THE MEASUREMENT IS STOPPED***

    private String FILENAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        // Create a unique filename by getting a timestamp in simple format down to
        // the second
        FILENAME = new SimpleDateFormat("yyyyMMddHHmmss'.csv'").format(new Date());
    }

    // Gets the data from the textbox when the button is clicked, and
    // writes it to the file in CSV format
    public void writeDataCSV (View view){
        try {
            // Create directory if it doesn't exist
            File file = new File(getExternalFilesDir(null),"Measurements");
            if(!file.exists()){
                Toast.makeText(this, "Creating dir", Toast.LENGTH_SHORT).show();
                file.mkdir();
            }

            //
            File data = new File(file, FILENAME);
            CSVWriter writer = new CSVWriter(new FileWriter(data, true));

            // Get the data from the field when the button is pressed; split it by comma
            TextView textView = findViewById(R.id.csvTextbox);
            String[] record = textView.getText().toString().split(",");

            // Write record, close file
            writer.writeNext(record);
            writer.flush();
            writer.close();
            Toast.makeText(this, "Write successful", Toast.LENGTH_SHORT).show();
        }
        catch (IOException exception){
            Toast.makeText(this, "File not found", Toast.LENGTH_LONG).show();
            exception.printStackTrace();
        }
    }
}
