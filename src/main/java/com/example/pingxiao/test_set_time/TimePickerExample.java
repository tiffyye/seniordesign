package com.example.pingxiao.test_set_time;

import java.util.Calendar;
import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import android.util.Log;
import android.content.Intent;
import android.widget.Toast;

import org.w3c.dom.Text;

public class TimePickerExample extends Activity {

    static final int TIME_DIALOG_ID = 1111;
    private TextView output;
    private TextView debug;
    public Button btnClick;
    public Button btnOn, btnOff;

    private int hour;
    private int minute;

    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");// whats this
    private static String address = "20:14:10:15:11:47";

    /*PINGXIAO BLUETOOTH MAC ADDRESS: D0:E1:40:9C:2D:FB
     *SNEGHA PHONE NEXUS: 48:59:29:56:12:58
     * HC-06: 20:14:10:15:11:47
     */

    //USE BLUESTEM TO CHECK FOR BLUETOOTH

    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "LEDOnOff";


    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_picker_example);

        output = (TextView) findViewById(R.id.output);
        debug = (TextView) findViewById(R.id.debug);
        final Calendar c = Calendar.getInstance();
        hour = c.get(Calendar.HOUR_OF_DAY);
        minute = c.get(Calendar.MINUTE);

        updateTime(hour, minute);

        addButtonClickListener();

        btnOn = (Button) findViewById(R.id.btnOn);
        btnOff = (Button) findViewById(R.id.btnOff);

        //get the bluetooth adapter for host device -> the tablet
       btAdapter = BluetoothAdapter.getDefaultAdapter();

        //check if it exists
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth Not Supported. Aborting.");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth is Enabled");
            } else {
                //enable the device - dialog for request would appear, waiting for response
                btAdapter.enable();
                Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        //Update: BluetoothConnection() this would be placed in OnResume()

        //define actions of the two buttons
        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("1");
                Toast msg = Toast.makeText(getBaseContext(),
                        "You have clicked On", Toast.LENGTH_SHORT);
                msg.show();
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("0");
                Toast msg = Toast.makeText(getBaseContext(),
                        "You have clicked Off", Toast.LENGTH_SHORT);
                msg.show();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "...In onResume(), attempting client connect...");
        BluetoothConnection();
        //Note: Before you click on any buttons, this part would be ready already (datastream is ready)
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()");

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and unable to close socket: " + e2.getMessage() + ".");
        }


    }

   private void BluetoothConnection() { //ADD THE FEATURE WHERE THE USER CAN SEARCH WHEN FIRST PAIRING
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        //SHOULD I CONNECT AS A CLIENT OR A SERVER
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            btSocket = null;
            errorExit("Fatal Error", "In BluetoothConnection() and socket create failed: " + e.getMessage() + ".");
        }

        btAdapter.cancelDiscovery();



        //establish connection
        Log.d(TAG, "...Connecting remote");
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
                errorExit("Fatal Error", "In BluetoothConnection() and unable to connect socket" + e.getMessage() + ".");
            } catch (IOException e2) {
                errorExit("Fatal Error", "In BluetoothConnection() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }


        //create a data stream
        Log.d(TAG, "...Creating socket");

        try {
            outStream = btSocket.getOutputStream();
            //String msg = "We are now right after creating the outstream in THE bluetoothconnection";
            //debug.setText(msg);
        } catch (IOException e) {
            String msg = "We are now right before creating the outstream in THE bluetoothconnection";
            debug.setText(msg);
            //errorExit("Fatal Error", "In BluetoothConnection() and output stream creation failed" + e.getMessage() + ".");
        }

       String msg = "We are now right before the bluetooth connection ends";
       debug.setText(msg);
    }

    //DO WE NEED TO HAVE ONDESTROY AND ONSTOP
    private void errorExit(String title, String message) {
        Toast msg = Toast.makeText(getBaseContext(),
                title + "-" + message, Toast.LENGTH_SHORT);
        msg.show();
        finish();
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        Log.d(TAG, "...Sending data" + message + "...");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In sendData() and output stream creation failed" + e.getMessage() + ".");
        }

        try {
            outStream.write(msgBuffer);
            //outStream.write(1);
        } catch (IOException e) {
            String msg = "In OnCreate and an exception occurred during write" + e.getMessage();
            errorExit("Fatal Error", msg);
        }

    }


    public void addButtonClickListener() {

        btnClick = (Button) findViewById(R.id.btnClick);
        btnClick.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showDialog(TIME_DIALOG_ID);
            }
        });
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case TIME_DIALOG_ID:
                // set time picker as current time
                return new TimePickerDialog(this, timePickerListener, hour, minute,
                        false);
        }
        return null;
    }

    private TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener() {

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minutes) {
            hour   = hourOfDay;
            minute = minutes;
            updateTime(hour,minute);
        }

    };

    private static String utilTime(int value) {

        if (value < 10)
            return "0" + String.valueOf(value);
        else
            return String.valueOf(value);
    }

    // Used to convert 24hr format to 12hr format with AM/PM values
    private void updateTime(int hours, int mins) {

        String timeSet = "";
        if (hours > 12) {
            hours -= 12;
            timeSet = "PM";
        } else if (hours == 0) {
            hours += 12;
            timeSet = "AM";
        } else if (hours == 12)
            timeSet = "PM";
        else
            timeSet = "AM";


        String minutes = "";
        if (mins < 10)
            minutes = "0" + mins;
        else
            minutes = String.valueOf(mins);

        // Append in a StringBuilder
        String aTime = new StringBuilder().append(hours).append(':')
                .append(minutes).append(" ").append(timeSet).toString();

        output.setText(aTime);
    }
}
