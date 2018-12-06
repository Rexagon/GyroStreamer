package ru.rtuitlab.gyrostreamer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class MainActivity extends Activity {
    private TextView mTimestampView;
    private TextView mPitchView;
    private TextView mRollView;

    private EditText mAddressInput;
    private EditText mPortInput;

    private Button mToggleButton;

    private UdpClient mUdpClient;

    private SensorManager mSensorManager;

    // Gravity rotational data
    private float accels[] = new float[3];
    private float mags[] = new float[3];

    // azimuth, pitch and roll
    private float azimuth;
    private float pitch;
    private float roll;

    private SensorEventListener mySensorEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    accels = event.values.clone();
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mags = event.values.clone();
                    break;
            }

            if (mags != null && accels != null) {
                float R[] = new float[9];
                float I[] = new float[9];

                boolean success = SensorManager.getRotationMatrix(R, I, accels, mags);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);

                    azimuth = orientation[0] * 57.2957795f;
                    pitch = orientation[1] * 57.2957795f;
                    roll = orientation[2] * 57.2957795f;

                    handleSensorsData();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initSensors();

        mTimestampView = findViewById(R.id.labelTimestamp);
        mPitchView = findViewById(R.id.labelPitch);
        mRollView = findViewById(R.id.labelRoll);

        mAddressInput = findViewById(R.id.inputHostAddress);
        mPortInput = findViewById(R.id.inputHostPort);

        mToggleButton = findViewById(R.id.buttonStart);
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUdpClient == null) {
                    startSending();
                }
                else {
                    stopStreaming();
                }
            }
        });
    }

    private void initSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mSensorManager.registerListener(mySensorEventListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        mSensorManager.registerListener(mySensorEventListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void startSending() {
        String address = mAddressInput.getText().toString();
        int port = Integer.parseInt(mPortInput.getText().toString());

        try {
            mUdpClient = new UdpClient(address, port);
            mToggleButton.setText("Stop");
            mToggleButton.setBackgroundResource(R.color.colorAccent);
        } catch (UnknownHostException e) {
            Toast.makeText(getApplicationContext(), "Unable to connect to host",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void stopStreaming() {
        mUdpClient = null;
        mToggleButton.setText("Start");
        mToggleButton.setBackgroundResource(R.color.colorPrimary);
    }

    private void handleSensorsData()
    {
        long timestamp = System.currentTimeMillis();

        if (mUdpClient != null) {
            mUdpClient.send(ByteBuffer.allocate(8+4*3)
                    .putLong(timestamp)
                    .putInt((int)(azimuth * 1000000))
                    .putInt((int)(pitch * 1000000))
                    .putInt((int)(roll * 1000000)));

            String timestampText = "Timestamp: " + timestamp;
            mTimestampView.setText(timestampText);

            String pitchText = "Pitch: " + pitch;
            mPitchView.setText(pitchText);

            String rollText = "Roll: " + roll;
            mRollView.setText(rollText);
        }
    }
}
