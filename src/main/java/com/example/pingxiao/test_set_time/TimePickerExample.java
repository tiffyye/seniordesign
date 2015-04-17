package com.example.pingxiao.test_set_time;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
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
import java.io.File;
import java.util.UUID;

import android.util.Log;
import android.content.Intent;
import android.widget.Toast;

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
    private Button sendAudioBtn;
    //private Button deleteMsgBtn;
    private TextView text;

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

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth Not Supported. Aborting.");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth is Enabled");
            } else {
                btAdapter.enable();
                Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        //Update: BluetoothConnection() this would be placed in OnResume()

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

        //PUT A TIME LIMIT TO THIS
        //outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + ""; this is for sd card
        text = (TextView) findViewById(R.id.textoutput);



        startBtn = (Button) findViewById(R.id.start);

        startBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                start(v);
            }
        });

        stopBtn = (Button) findViewById(R.id.stop);
        stopBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                stop(v);
            }
        });

        playBtn = (Button) findViewById(R.id.play);
        playBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                play(v);
            }
        });

        stopPlayBtn = (Button) findViewById(R.id.stopPlay);
        stopPlayBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                stopPlay(v);
            }
        });

        sendAudioBtn = (Button) findViewById(R.id.sendAudio);
        stopPlayBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                sendAudio(v);
            }
        });

        /*deleteMsgBtn = (Button) findViewById(R.id.deleteMsg);
        deleteMsgBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                deleteMsg(v);
            }
        });*/

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

    private void sendAudioData(){
        //NEED TO TEST
        File audiofile = new File(getFilesDir(), "/audio.3gp");
        byte[] finalAudioByteArray = null;

        if(audiofile.exists())
        {
          try {
              InputStream input_stream = new BufferedInputStream(new FileInputStream(audiofile));
              ByteArrayOutputStream buffer = new ByteArrayOutputStream();
              byte[] data = new byte[1024*16]; // 16K WHAT IF WE HAVE A REALLY BIG SIZE
              int bytes_read;
              while ((bytes_read = input_stream.read(data, 0, data.length)) != -1) {
                  buffer.write(data, 0, bytes_read);
              }
              input_stream.close();
              finalAudioByteArray = buffer.toByteArray();
          } catch (Exception e) {
              e.printStackTrace();
          }

            try {
                outStream = btSocket.getOutputStream();
            } catch (IOException e) {
                errorExit("Fatal Error", "In sendAudioData() and output stream creation failed" + e.getMessage() + ".");
            }


            Log.d(TAG, "...Sending audio data...");

            try {
                outStream.write(finalAudioByteArray);
            } catch (IOException e) {
                String msg = "In sendAudioData() and an exception occurred during write" + e.getMessage();
                errorExit("Fatal Error", msg);
            }


        }


    }

    private void setUpAudio(){
        outputFile = getFilesDir() + "/audio.3gp";
        myRecorder = new MediaRecorder();
        myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        myRecorder.setOutputFile(outputFile);
    }

    public void sendAudio(View view) {
        try {
        sendAudioData();

        text.setText("Recording Point: Sending Audio now...");
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        playBtn.setEnabled(true);
        stopPlayBtn.setEnabled(false);

        Toast.makeText(getApplicationContext(), "Send Audio data...",
                Toast.LENGTH_SHORT).show();
        } catch (Exception e) {

            e.printStackTrace();
        }

    }


    public void start(View view) {
        try {
            setUpAudio();
            myRecorder.prepare();
            myRecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }

        text.setText("Recording Point: Recording");
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        playBtn.setEnabled(false);
        stopPlayBtn.setEnabled(false);

        Toast.makeText(getApplicationContext(), "Start recording...",
                Toast.LENGTH_SHORT).show();
        //MEMORY ISSUE??
    }

    public void stop(View view) {
        try {
            myRecorder.stop();
            myRecorder.release();
            myRecorder = null;
            //this is for re-recording

            stopBtn.setEnabled(false);
            playBtn.setEnabled(true);
            startBtn.setEnabled(true);
            stopPlayBtn.setEnabled(false);
            //deleteMsgBtn.setEnabled(true);
            text.setText("Recording Point: Stop recording");

            Toast.makeText(getApplicationContext(), "Stop recording...",
                    Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void play(View view) {
        try {
            myPlayer = new MediaPlayer();
            myPlayer.setDataSource(outputFile);
            myPlayer.prepare();
            myPlayer.start();

            playBtn.setEnabled(false);
            stopPlayBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            startBtn.setEnabled(false);
            //deleteMsgBtn.setEnabled(false);
            text.setText("Recording Point: Playing");

            Toast.makeText(getApplicationContext(), "Start play the recording...",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public void stopPlay(View view) {
        try {
            if (myPlayer != null) {
                myPlayer.stop();
                myPlayer.release();
                myPlayer = null;

                playBtn.setEnabled(true);
                stopPlayBtn.setEnabled(false);
                stopBtn.setEnabled(false);
                startBtn.setEnabled(true);

                text.setText("Recording Point: Stop playing");

                Toast.makeText(getApplicationContext(), "Stop playing the recording...",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
    //DO WE NEED TO HAVE ONDESTROY AND ONSTOP
    private void errorExit(String title, String message) {
        Toast msg = Toast.makeText(getBaseContext(),
                title + "-" + message, Toast.LENGTH_SHORT);
        msg.show();
        finish();
    }

    private void BluetoothConnection() { //ADD THE FEATURE WHERE THE USER CAN SEARCH WHEN FIRST PAIRING
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            btSocket = null;
            errorExit("Fatal Error", "In BluetoothConnection() and socket create failed: " + e.getMessage() + ".");
        }

        btAdapter.cancelDiscovery();

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

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In sendData() and output stream creation failed" + e.getMessage() + ".");
        }

        // String msg = "We are now right before the bluetooth connection ends";
        // debug.setText(msg);
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();


        Log.d(TAG, "...Creating socket");

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

        String aTime = new StringBuilder().append(hours).append(':')
                .append(minutes).append(" ").append(timeSet).toString();

        output.setText(aTime);

    }
}
