package com.jamierajewski.sensorcompanion;

import android.content.BroadcastReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Button;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

import static com.jamierajewski.sensorcompanion.CalibrationActivity.MAX;
import static com.jamierajewski.sensorcompanion.CalibrationActivity.MIN;

//
// Bluetooth based on code from:
// http://www.instructables.com/id/Android-Bluetooth-Control-LED-Part-2/
// Continuously updating listview based on code from:
// https://stackoverflow.com/questions/18840896/constantly-update-list-items
//

public class DeviceListActivity extends AppCompatActivity {

    // Widgets
    Button btnPaired;
    ListView devicelist;

    // Bluetooth related
    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    public static String EXTRA_ADDRESS = "device_address";
    public static String FORMULA = "formula";
    public static String FUNCTION = "function";
    public String formula_text;
    public String formula_name;
    public float min, max;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        Intent newintent = getIntent();
        formula_text = newintent.getStringExtra(CalibrationActivity.MODE);
        formula_name = newintent.getStringExtra(CalibrationActivity.FUNCTION);
        min = newintent.getFloatExtra(CalibrationActivity.MIN, 0.0f);
        max = newintent.getFloatExtra(CalibrationActivity.MAX, 0.0f);

        //Calling widgets
        btnPaired = findViewById(R.id.paired_button);
        devicelist = findViewById(R.id.listView);

        //if the device has bluetooth
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if(myBluetooth == null)
        {
            //Show a mensag. that the device has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();

            //finish apk
            finish();
        }
        else if(!myBluetooth.isEnabled())
        {
            //Ask to the user turn the bluetooth on
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon,1);
        }


        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (!myBluetooth.isEnabled()){
                    //Ask to the user turn the bluetooth on
                    Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(turnBTon,1);
                }
                else {
                    pairedDevicesList();
                }
            }
        });
    }

    private void pairedDevicesList()
    {
        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size()>0)
        {
            for(BluetoothDevice bt : pairedDevices)
            {
                list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
            }
        }
        else
        {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked
    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick (AdapterView<?> av, View v, int arg2, long arg3)
        {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Make an intent to start next activity.
            Intent i = new Intent(DeviceListActivity.this, GraphActivity.class);

            //Change the activity.
            i.putExtra(EXTRA_ADDRESS, address);
            i.putExtra(FORMULA, formula_text);
            i.putExtra(FUNCTION, formula_name);
            i.putExtra(MIN, min);
            i.putExtra(MAX, max);
            startActivity(i);
        }
    };
}