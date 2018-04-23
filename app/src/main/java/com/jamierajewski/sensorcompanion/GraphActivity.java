package com.jamierajewski.sensorcompanion;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVWriter;
import com.udojava.evalex.Expression;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;



// ***TODO*** //
// - Refactor this ugly behemoth
// - Find solution to self-scaling y-axis based on given formula

public class GraphActivity extends AppCompatActivity {

    private final Handler mHandler = new Handler();
    private LineGraphSeries<DataPoint> mSeries;

    // Exported CSV file
    private String FILENAME;

    // Stat-related
    Welford_Est moving_stats = new Welford_Est();

    // Tag for logging purposes
    //private static final String TAG = "GraphActivity";

    // Tasks
    AsyncTask task;

    // Formula
    String formula_text;
    String formula_name;
    float min, max;

    // BLUETOOTH-RELATED
    private boolean mRunning;
    Button stopButton;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        // Create a unique filename by getting a timestamp in simple format down to
        // the second
        FILENAME = new SimpleDateFormat("yyyyMMddHHmmss'.csv'").format(new Date());

        // BT
        Intent newint = getIntent();
        //receive the address of the bluetooth device
        address = newint.getStringExtra(DeviceListActivity.EXTRA_ADDRESS);
        //receive the formula for converting
        formula_text = newint.getStringExtra(DeviceListActivity.FORMULA);
        // receive the formula name for inserting as the y-axis and for logging
        formula_name = newint.getStringExtra(DeviceListActivity.FUNCTION);
        // receive the min and max for the y-axis
        min = newint.getFloatExtra(CalibrationActivity.MIN, 0.0f);
        max = newint.getFloatExtra(CalibrationActivity.MAX, 0.0f);

        String[] headers = {formula_name, "Time", "Mean", "Std. Deviation"};

        stopButton = findViewById(R.id.stopButton);

        new ConnectBT().execute(); //Call the class to connect

        //commands to be sent to bluetooth
        stopButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Disconnect(); //close connection
                onBackPressed();
            }
        });

        // Write the headers of the CSV
        writeHeaders(headers);

        // Graph
        GraphView graph = findViewById(R.id.graph);
        initGraph(graph, min, max);
    }

    // File is located in Android/sensorcompanion/Measurements/
    public void writeHeaders(String[] headers){
        // Create file if it doesn't exist, and fill in headers
        File file = new File(getExternalFilesDir(null),"Measurements");
        if(!file.exists()){
            file.mkdir();
        }

        File data = new File(file, FILENAME);
        try{
            CSVWriter writer = new CSVWriter(new FileWriter(data, true));
            // Write column names
            writer.writeNext(headers, false);
            writer.close();
        } catch (Exception ex){
            Toast.makeText(this, "ERROR - CANNOT CREATE CSV WRITER", Toast.LENGTH_SHORT).show();
            ex.printStackTrace();
        }
    }


    public void initGraph(GraphView graph, float min, float max) {
        // ***ALLOW USER TO SPECIFY THEIR OWN RANGE?*** <<--- YES
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(min);
        graph.getViewport().setMaxY(max);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(3);

        graph.getGridLabelRenderer().setNumVerticalLabels(6);

        // Pad the y-axis labels so they are visible

        // **RATHER THAN USE GRAPHVIEW LABELS, CREATE MY OWN SINCE THEY'RE STATIC?**
        //graph.getGridLabelRenderer().setPadding(75);
        graph.getGridLabelRenderer().setVerticalAxisTitle(formula_name);
        graph.getGridLabelRenderer().setLabelVerticalWidth(50);
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");
        graph.getGridLabelRenderer().setLabelHorizontalHeight(50);

        // enable scaling and scrolling
        graph.getViewport().setScalable(true);

        // first mSeries is a line
        mSeries = new LineGraphSeries<>();
        //mSeries.setDrawDataPoints(true);
        mSeries.setDrawBackground(true);
        graph.addSeries(mSeries);
    }

    @Override
    public void onBackPressed(){
        // Create an alert dialog to warn the user about deletion
        AlertDialog alertDialog = new AlertDialog.Builder(GraphActivity.this).create();
        alertDialog.setTitle("WARNING");
        alertDialog.setMessage("Are you sure you want to quit?");
        alertDialog.setButton(alertDialog.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        saveAlert();
                        alertDialog.dismiss();
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

    public void saveAlert(){
        // Create an alert dialog to warn the user about deletion
        AlertDialog alertDialog = new AlertDialog.Builder(GraphActivity.this).create();
        alertDialog.setTitle("Save Data");
        alertDialog.setMessage("Would you like to save this measurement?");
        alertDialog.setButton(alertDialog.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        saveConfirmed();
                        alertDialog.dismiss();
                    }

                });
        alertDialog.setButton(alertDialog.BUTTON_NEGATIVE, "No",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // DELETE FILE HERE
                        try{
                            File dir = new File(getExternalFilesDir(null),"Measurements");
                            File file = new File(dir, FILENAME);
                            file.delete();
                        } finally {
                            Disconnect();
                        }
                        alertDialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public void saveConfirmed(){
        // Create an alert dialog to warn the user about deletion
        AlertDialog alertDialog = new AlertDialog.Builder(GraphActivity.this).create();
        alertDialog.setTitle("Save Data");
        alertDialog.setMessage("Data saved in CSV format at /Android/data/com.jamierajewski.sensorCompanion/files");
        alertDialog.setButton(alertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Disconnect();
                    }

                });
        alertDialog.show();
    }


    // CITATION - http://alias-i.com/lingpipe/docs/api/com/aliasi/stats/OnlineNormalEstimator.html
    // Modified for float instead of double; add std. dev
    // An implementation of Welford's Algorithm, allowing for running mean and standard deviation
    public class Welford_Est{
        long n = 0;
        float mu = 0;
        float sq = 0;

        void update(float x) {
            ++n;
            float muNew = mu + (x - mu)/n;
            sq += (x - mu) * (x - muNew);
            mu = muNew;
        }
        float mean() { return mu; }
        float var() { return n > 1 ? sq/n : 0; }
        float deviation() {return (float)Math.sqrt(var());}
    }

    public void writeDataCSV(String[] toWrite){

        // The file should have already been created prior, but still check if it exists to make sure
        try{
            File file = new File(getExternalFilesDir(null),"Measurements");
            if(!file.exists()){
                file.mkdir();
            }

            // Create CSV writer
            File data = new File(file, FILENAME);
            CSVWriter writer = new CSVWriter(new FileWriter(data, true));

            for (String entry : toWrite){
                if (entry == null){
                    break;
                }
                String[] temp = entry.split("-");
                writer.writeNext(temp, false);
            }

            writer.close();


        } catch (Exception ex){
            Toast.makeText(this, "Error creating or reading file", Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }

    }


    //////////////// ***BLUETOOTH-RELATED*** /////////////////

    // Continually update the list of data
    Runnable mUpdater = new Runnable() {
        @Override
        public void run() {
            if (isBtConnected) {
                // Check if still in focus
                if (!mRunning) return;
                task = new processData().execute(btSocket);
            }

            // Schedule next run
            mHandler.postDelayed(this, 200);
        }
    };

    // **SPLIT THIS INTO SMALLER FUNCTIONS THAT GET CALLED INSIDE THE ASYNCTASK**
    private class processData extends AsyncTask<BluetoothSocket, ArrayList, ArrayList<ArrayList>>
    {
        int offset = 0;
        int bytesRead = 0;
        byte[] data = new byte[35];

        @Override
        protected ArrayList<ArrayList> doInBackground(BluetoothSocket... sockets)
        {
            BigDecimal temp;
            float voltage;
            float time;
            String[] ret;
            String str;

            try {
                InputStream socketInputStream = sockets[0].getInputStream();
                while ((bytesRead = socketInputStream.read(data, offset, data.length - offset))
                        != -1) {
                    offset += bytesRead;
                    if (offset >= data.length) {
                        break;
                    }
                }
                str = new String(data, 0, offset, "UTF-8");
                str = str.replace("\n", "");

            } catch (IOException e){
                // If the socket cant be opened, return null
                msg("Error opening socket stream");
                return null;
            }

            //***ONLY SPLIT AFTER VERIFYING INPUT***//
            ret = verifyData(str);

            float lastTime = 0;
            ArrayList<ArrayList> pairList = new ArrayList<>();

            for (String entry: ret) {
                // Exclude garbage entries
                if (!entry.equals("")){
                    try {
                        String[] parts = entry.split("-");
                        Expression expression = new Expression(formula_text).with("x", parts[0]);
                        temp = expression.eval();
                        voltage = temp.floatValue();

                        time = Float.parseFloat(parts[1]);

                    } catch (Exception e) {
                        continue;
                    }

                    // Ensure data is valid, then store in a float pair
                    //***RATHER THAN 0-5, CHECK IF IT'S BETWEEN THE RANGE SET BY THE USER <- NO***
                    //if (voltage > 0.00 && voltage <= 5.00 && time > lastTime) {
                    if (time > lastTime) {
                        lastTime = time;
                        ArrayList<Float> pair = new ArrayList<>();
                        pair.add(time);
                        pair.add(voltage);

                        // Now store that pair in the list to be returned
                        pairList.add(pair);
                    } else {
                        continue;
                    }
                }
            }
            return pairList;
        }

        // Helper function to ensure only complete data is received, and remove anything else
        private String[] verifyData(String data){

            if (!data.endsWith("&")){
                data = data.substring(0, data.lastIndexOf("&"));
            }

            if (!data.startsWith("&")){
                data = data.substring(data.indexOf("&"));
            }

            String[] res = data.split("&");

            return res;
        }

        // After processing, append each new point to the graph
        @Override
        protected void onPostExecute(ArrayList<ArrayList> result)
        {
            float time;
            float voltage;
            int count = 0;
            TextView mean_view = findViewById(R.id.result_mean_textview);
            TextView deviation_view = findViewById(R.id.result_stddev_textview);
            String[] toWrite = new String[37];

            for (ArrayList<Float> pair : result){
                time = pair.get(0);
                voltage = pair.get(1);

                //Log.i(TAG, "Recv'd: " + voltage + " " + time);
                mSeries.appendData(new DataPoint(time, voltage), true, 30);

                // Add to the mean
                moving_stats.update(voltage);

                // Update textviews with new stats
                mean_view.setText(String.valueOf(moving_stats.mean()));
                deviation_view.setText(String.valueOf(moving_stats.deviation()));
                // Re-combine the verified data back to a string to be written to the file
                String temp = Float.toString(voltage) + "-" + Float.toString(time) + "-" + moving_stats.mean() + "-" + moving_stats.deviation();
                toWrite[count] = temp;
                count++;
            }
            // Attempt to help the garbage collector; not sure if it matters
            result.clear();

            // Finally, pass the string array to the write function to write this data set to the file
            writeDataCSV(toWrite);
        }
    }

    // When the app resumes focus, begin updating again
    @Override
    protected void onResume(){
        super.onResume();
        mRunning = true;
        // Start first run manually
        mHandler.post(mUpdater);
    }

    // If the app is taken out of view, stop updating the list
    @Override
    protected void onPause(){
        super.onPause();
        progress.dismiss();
        mRunning = false;
    }

    private void Disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        // Stop the processing thread / log to file
        mRunning = false;
        task.cancel(false);

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(GraphActivity.this, "Connecting...", "Please wait!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice temp = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = temp.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Ensure the sensor is transmitting.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }
}