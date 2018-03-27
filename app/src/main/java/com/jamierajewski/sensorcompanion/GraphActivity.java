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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.udojava.evalex.Expression;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;


public class GraphActivity extends AppCompatActivity {

    private final Handler mHandler = new Handler();
    private LineGraphSeries<DataPoint> mSeries;

    // Stat-related
    Welford_Est moving_stats = new Welford_Est();

    // Tag for logging purposes
    private static final String TAG = "GraphActivity";

    // Tasks
    AsyncTask task;

    // Formula
    String formula = null;

    // BLUETOOTH-RELATED
    private boolean mRunning;
    Button finish_button;
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

        // BT
        Intent newint = getIntent();
        //receive the address of the bluetooth device
        address = newint.getStringExtra(DeviceListActivity.EXTRA_ADDRESS);
        //receive the formula for converting
        formula = newint.getStringExtra(DeviceListActivity.FORMULA);

        finish_button = findViewById(R.id.finish_button);

        new ConnectBT().execute(); //Call the class to connect

        //commands to be sent to bluetooth
        finish_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Disconnect(); //close connection
                onBackPressed();
            }
        });

        // Graph
        GraphView graph = findViewById(R.id.graph);

        initGraph(graph);
    }

    public void initGraph(GraphView graph) {
        // ***ALLOW USER TO SPECIFY THEIR OWN RANGE?*** <<--- YES
//        graph.getViewport().setYAxisBoundsManual(true);
//        graph.getViewport().setMinY(0);
//        graph.getViewport().setMaxY(5);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(3);

        graph.getGridLabelRenderer().setNumVerticalLabels(6);

        // Pad the y-axis labels so they are visible

        // **RATHER THAN USE GRAPHVIEW LABELS, CREATE MY OWN SINCE THEY'RE STATIC**
        //graph.getGridLabelRenderer().setPadding(75);
        graph.getGridLabelRenderer().setVerticalAxisTitle("Voltage (V)");
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
                        Disconnect();
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

//    public void onStartPressed(View view){
//
//        // Create an alert dialog to get the range for the graph
//        AlertDialog alertDialog = new AlertDialog.Builder(GraphActivity.this).create();
//        alertDialog.setTitle("Range");
//        alertDialog.setMessage("Please set the range: ");
//
//        final EditText input = new EditText(this);
//        input.setInputType(InputType.TYPE_CLASS_TEXT);
//        alertDialog.setView(input);
//        alertDialog.setButton(alertDialog.BUTTON_POSITIVE, "Enter",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        int lower, higher = 0;
//                        try {
//                            String[] result = (input.getText().toString()).split(",");
//                            lower = Integer.getInteger(result[0]);
//                            higher = Integer.getInteger(result[1]);
//                        } catch (Exception ex){
//                            return;
//                        }
//                        GraphView graph = findViewById(R.id.graph);
//                        graph.getViewport().setMinY(lower);
//                        graph.getViewport().setMaxY(higher);
//                        graph.draw();
//                    }
//
//                });
//        alertDialog.setButton(alertDialog.BUTTON_NEGATIVE, "Cancel",
//                new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        alertDialog.dismiss();
//                    }
//                });
//        alertDialog.show();
//    }


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

    /// CREATE ANOTHER ASYNCTASK TO WRITE TO FILE??? /// <<--- YES, CALL IT INSIDE processData

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

            try {
                InputStream socketInputStream = sockets[0].getInputStream();
                while ((bytesRead = socketInputStream.read(data, offset, data.length - offset))
                        != -1) {
                    offset += bytesRead;
                    if (offset >= data.length) {
                        break;
                    }
                }
                String str = new String(data, 0, offset, "UTF-8");
                ret = str.split("\n");

            } catch (IOException e){
                // If the socket cant be opened, return null
                msg("Error opening socket stream");
                return null;
            }

            float lastTime = 0;
            ArrayList<ArrayList> pairList = new ArrayList<>();

            for (String entry: ret) {
                try {
                    String[] parts = entry.split("-");

                    //voltage = Float.parseFloat(parts[0]);
                    Expression expression = new Expression(formula).with("x", parts[0]);
                    temp = expression.eval();
                    voltage = temp.floatValue();

                    time = Float.parseFloat(parts[1]);

                } catch (Exception e) {
                    continue;
                }

                // Ensure data is valid, then store in a float pair
                //***RATHER THAN 0-5, CHECK IF IT'S BETWEEN THE RANGE SET BY THE USER***
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

            return pairList;
        }

        // After processing, append each new point to the graph
        @Override
        protected void onPostExecute(ArrayList<ArrayList> result)
        {
            float time;
            float voltage;
            TextView mean_view = findViewById(R.id.result_mean_textview);
            TextView deviation_view = findViewById(R.id.result_stddev_textview);


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
            }
            // Attempt to help the garbage collector; not sure if it matters
            result.clear();
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