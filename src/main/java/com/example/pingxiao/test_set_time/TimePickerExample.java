package com.example.pingxiao.test_set_time;

import java.util.Calendar;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
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

    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "LEDOnOff";

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    private MediaRecorder myRecorder;
    private MediaPlayer myPlayer;
    private String outputFile = null;
    private Button startBtn;
    private Button stopBtn;
    private Button playBtn;
    private Button stopPlayBtn;
    private TextView recordertext;

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
                sendData("N");
                Toast msg = Toast.makeText(getBaseContext(),
                        "You have clicked On", Toast.LENGTH_SHORT);
                msg.show();
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("F");
                Toast msg = Toast.makeText(getBaseContext(),
                        "You have clicked Off", Toast.LENGTH_SHORT);
                msg.show();
            }
        });

        recordertext = (TextView) findViewById(R.id.recordertext);

        //STORE IT TO SOMEWHERE?
        //PUT A TIME LIMIT TO THIS
        //outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + ""; this is for sd card
        outputFile = getFilesDir() + "/audio.3gp";
        myRecorder = new MediaRecorder();
        myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        myRecorder.setOutputFile(outputFile);

        startBtn = (Button) findViewById(R.id.recordstart);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = "We are now STARTING to record...";
                debug.setText(msg);
                start(v);
            }
        });

        stopBtn = (Button) findViewById(R.id.recordstop);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop(v);
            }
        });

        playBtn = (Button) findViewById(R.id.recordplay);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(v);
            }
        });

        stopPlayBtn = (Button) findViewById(R.id.recordstopplay);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlay(v);
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

        if (myRecorder != null) {
            myRecorder.release();
            myRecorder = null;
        }

        if (myPlayer != null) {
            myPlayer.release();
            myPlayer = null;
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

        Log.d(TAG, "...Creating socket");

        //create a data stream
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In sendData() and output stream creation failed" + e.getMessage() + ".");
        }

        String msg = "We are now right before the bluetooth connection ends";
        debug.setText(msg);
    }

    private void start(View view) {
        try {

            myRecorder.prepare();
            myRecorder.start();

        } catch (IllegalStateException e) {
            errorExit("Fatal Error", "Recording start failed: " + e.getMessage() + ".");
        } catch (IOException e2) {
            errorExit("Fatal Error", "Recording start failed: " + e2.getMessage() + ".");
        }

        recordertext.setText("Recording");
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);

        Toast.makeText(getApplicationContext(), "Start recording...",
                Toast.LENGTH_SHORT).show();
    }

    private void stop(View view) {
        try {
            myRecorder.stop();
            myRecorder.release();
            myRecorder = null;
        } catch (IllegalStateException e) {
            errorExit("Fatal Error", "Recording stop failed: " + e.getMessage() + ".");
        } catch (RuntimeException e2) {
            errorExit("Fatal Error", "Recording stop failed: " + e2.getMessage() + ".");
        }
        stopBtn.setEnabled(false);
        playBtn.setEnabled(true);

        recordertext.setText("Stop");

        Toast.makeText(getApplicationContext(), "Sop recording...",
                Toast.LENGTH_SHORT).show();

    }

    private void play(View view) {
        try {
            myPlayer = new MediaPlayer();
            myPlayer.setDataSource(outputFile);
            myPlayer.prepare();
            myPlayer.start();

            playBtn.setEnabled(false);
            stopPlayBtn.setEnabled(true);
            recordertext.setText("Playing");
        } catch (Exception e) {
            errorExit("Fatal Error", "Playing failed: " + e.getMessage() + ".");
        }

    }

    private void stopPlay(View view) {
        try {
            if (myPlayer != null) {
                myPlayer.stop();
                myPlayer.release();
                myPlayer = null;
                playBtn.setEnabled(true);
                stopPlayBtn.setEnabled(false);
                recordertext.setText("Stop Playing");
                Toast.makeText(getApplicationContext(), "Stop playing the recording...",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            errorExit("Fatal Error", "Stop Playing failed: " + e.getMessage() + ".");
        }
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


        Log.d(TAG, "...Creating socket");

        //create a data stream
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In sendData() and output stream creation failed" + e.getMessage() + ".");
        }


        Log.d(TAG, "...Sending data" + message + "...");

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
            hour = hourOfDay;
            minute = minutes;
            sendData("" + hour + minute);
            updateTime(hour, minute);
        }

    };


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
