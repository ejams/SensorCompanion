package com.jamierajewski.sensorcompanion;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.UUID;

import static java.lang.Float.parseFloat;


public class graph_example extends AppCompatActivity {

    private final Handler mHandler = new Handler();
    private LineGraphSeries<DataPoint> mSeries;

    private static final String TAG = "graph_example";

    // TEST //
    AsyncTask task;

    // BLUETOOTH-RELATED
    private boolean mRunning;
    Button disconnect_btn;
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
        setContentView(R.layout.activity_graph_example);

        // BT
        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        disconnect_btn = findViewById(R.id.disconnect_button);

        new ConnectBT().execute(); //Call the class to connect

        //commands to be sent to bluetooth
        disconnect_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Stop the processing thread / log to file
                mRunning = false;
                task.cancel(true);
                Disconnect(); //close connection
            }
        });

        // Graph
        GraphView graph = findViewById(R.id.graph);

        initGraph(graph);
    }

    public void initGraph(GraphView graph) {
        // ***ALLOW USER TO SPECIFY THEIR OWN RANGE?***
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(5);

        // ***FIND BEST DOMAIN FOR THE VIEWPORT BASED ON THE DATA?***
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(3);

        //graph.getGridLabelRenderer().setNumHorizontalLabels(9);
        graph.getGridLabelRenderer().setNumVerticalLabels(6);

        // Pad the y-axis labels so they are visible
        //graph.getGridLabelRenderer().setPadding(75);
        graph.getGridLabelRenderer().setVerticalAxisTitle("Voltage (V)");
        graph.getGridLabelRenderer().setLabelVerticalWidth(50);
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");
        graph.getGridLabelRenderer().setLabelHorizontalHeight(50);

        // first mSeries is a line
        mSeries = new LineGraphSeries<>();
        //mSeries.setDrawDataPoints(true);
        mSeries.setDrawBackground(true);
        graph.addSeries(mSeries);
    }

    @Override
    public void onBackPressed(){
        // Create an alert dialog to warn the user about deletion
        AlertDialog alertDialog = new AlertDialog.Builder(graph_example.this).create();
        alertDialog.setTitle("WARNING");
        alertDialog.setMessage("Are you sure you want to delete this entry?");
        alertDialog.setButton(alertDialog.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Stop the processing thread / log to file
                        mRunning = false;
                        task.cancel(true);

                        // Disconnect from the sensor
                        Disconnect();

                        // Return to previous page
                        Intent intent = new Intent(graph_example.this, DeviceList.class);
                        finish();
                        startActivity(intent);
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

    //////////////// ***BLUETOOTH-RELATED*** /////////////////

    // Continually update the list of data
    Runnable mUpdater = new Runnable() {
        @Override
        public void run() {
            if (isBtConnected) {
                // Check if still in focus
                if (!mRunning) return;

                // Receive data and process it
//                try {
                    //receiveData(btSocket);
                task = new processData().execute(btSocket);
            }

            // Schedule next run
            mHandler.postDelayed(this, 200);
            //mHandler.post(this);
        }
    };

    /// CREATE ANOTHER ASYNCTASK TO WRITE TO FILE??? ///

    private class processData extends AsyncTask<BluetoothSocket, ArrayList, ArrayList<ArrayList>>
    {
        int offset = 0;
        int bytesRead = 0;
        byte[] data = new byte[35];

        @Override
        protected ArrayList<ArrayList> doInBackground(BluetoothSocket... sockets)
        {
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

                //return ret;

            } catch (IOException e){
                // If the socket cant be opened, return null
                msg("Error opening socket stream");
                return null;
            }

            //super.onPostExecute(result);
            float lastTime = 0;
            ArrayList<ArrayList> pairList = new ArrayList<>();

            for (String entry: ret) {
                try {
                    String[] parts = entry.split("-");

                    voltage = Float.parseFloat(parts[0]);
                    time = Float.parseFloat(parts[1]);

                } catch (Exception e) {
                    continue;
                }

                // Ensure data is valid, then store in a float pair
                if (voltage > 0.00 && voltage <= 5.00 && time > lastTime) {
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

        @Override
        protected void onPostExecute(ArrayList<ArrayList> result) //after the doInBackground, it checks if everything went fine
        {
            float time;
            float voltage;

            for (ArrayList<Float> pair : result){
                time = pair.get(0);
                voltage = pair.get(1);
                //Log.i(TAG, "Recv'd: " + voltage + " " + time);
                mSeries.appendData(new DataPoint(time, voltage), true, 30);
            }
            result.clear();
            //mSeries.appendData(new DataPoint(count+=0.10, voltage), true, 10);
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
        finish(); //return to the first layout

    }


    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(graph_example.this, "Connecting...", "Please wait!!!");  //show a progress dialog
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
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
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
//    @Override
//    public void onResume() {
//        super.onResume();
//        mTimer = new Runnable() {
//            @Override
//            public void run() {
//                graphLastXValue += 0.25d;
//                mSeries.appendData(new DataPoint(graphLastXValue, getRandom()), true, 22);
//                mHandler.postDelayed(this, 330);
//            }
//        };
//        mHandler.postDelayed(mTimer, 1500);
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        mHandler.removeCallbacks(mTimer);
//    }
//
//    double mLastRandom = 2;
//    Random mRand = new Random();
//    private double getRandom() {
//        return mLastRandom += mRand.nextDouble()*0.5 - 0.25;
//    }
//}