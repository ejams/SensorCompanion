package com.jamierajewski.sensorcompanion;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
import java.util.TimerTask;
import java.util.UUID;

import static java.lang.Float.parseFloat;


public class graph_example extends AppCompatActivity {

    private final Handler mHandler = new Handler();
    private LineGraphSeries<DataPoint> mSeries;

    private static final String TAG = "graph_example";

    // TEST //
    float count = 0;

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
        graph.getViewport().setMaxX(4);

        // Pad the y-axis labels so they are visible
        graph.getGridLabelRenderer().setLabelVerticalWidth(100);

        // first mSeries is a line
        mSeries = new LineGraphSeries<>();
        mSeries.setDrawDataPoints(true);
        mSeries.setDrawBackground(true);
        graph.addSeries(mSeries);
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
                new processData().execute(btSocket);
            }

            // Schedule next run
            //mHandler.postDelayed(this, 100);
            mHandler.post(this);
        }
    };

    /// CREATE ANOTHER ASYNCTASK TO WRITE TO FILE??? ///

    private class processData extends AsyncTask<BluetoothSocket, Void, String[]>
    {
        int offset = 0;
        int bytesRead = 0;
        byte[] data = new byte[35];

        @Override
        protected String[] doInBackground(BluetoothSocket... sockets)
        {
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
                String[] ret = str.split("\n");
                return ret;

            } catch (IOException e){
                msg("Error opening socket stream");
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] result) //after the doInBackground, it checks if everything went fine
        {
            float ret;
            //super.onPostExecute(result);
            for (String entry: result){
                try{
                    ret = Float.parseFloat(entry);

                } catch (Exception e){
                    ret = 0;
                }
                // Some buggy behaviour can cause large values to appear if the bytes come in a certain order,
                // so report it as 0
                if (ret > 5){
                    ret = 0;
                }
                //Log.i(TAG, "Recv'd - " + ret);
                mSeries.appendData(new DataPoint(count+=0.25, ret), true, 22);
            }
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
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
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