package com.jamierajewski.sensorcompanion;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

//
// Bluetooth code based on example from:
// http://www.instructables.com/id/Android-Bluetooth-Control-LED-Part-2/
//

public class ReceiveData extends AppCompatActivity {

    ScrollView dataScrollview;
    TextView text;

    Button btnDis;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Used in the process of continuous updating
    private boolean mRunning;
    Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_data);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the Receive Data activity
        setContentView(R.layout.activity_receive_data);

        //call the widgets
        btnDis = findViewById(R.id.btn_disconnect);
        dataScrollview = findViewById(R.id.receivedDataScrollview);
        text = findViewById(R.id.text);
        text.setMovementMethod(new ScrollingMovementMethod());

        new ConnectBT().execute(); //Call the class to connect

        //---Inside mUpdater, check to ensure that the connection is
        //---fully established before reading data

        //commands to be sent to bluetooth
        btnDis.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Disconnect(); //close connection
            }
        });
    }

    // Receive the serial data and convert it to a string !!!<<--- CHANGE THIS TO CONVERT TO INT
    public void receiveData(BluetoothSocket socket) throws IOException {
        InputStream socketInputStream = socket.getInputStream();
        byte[] buffer = new byte[256];
        int bytes;

        bytes = socketInputStream.read(buffer);
        String message = new String(buffer, 0, bytes);
        //Display(message);
        text.append(message + "\n"); // <<-- Add to the text in the scroll view here
        dataScrollview.fullScroll(View.FOCUS_DOWN);
    }

    // Continually update the list of data
    Runnable mUpdater = new Runnable() {
        @Override
        public void run() {
            if (isBtConnected) {
                // Check if still in focus
                if (!mRunning) return;

                // Update scrollview here
                try {
                    receiveData(btSocket);
                } catch (IOException e) {
                    Thread t = Thread.currentThread();
                    t.getUncaughtExceptionHandler().uncaughtException(t, e);
                }
            }

            // Schedule next run
            mHandler.postDelayed(this, 250); // <<-- SET TIME TO REFRESH HERE
        }
    };

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
            progress = ProgressDialog.show(ReceiveData.this, "Connecting...", "Please wait!!!");  //show a progress dialog
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



