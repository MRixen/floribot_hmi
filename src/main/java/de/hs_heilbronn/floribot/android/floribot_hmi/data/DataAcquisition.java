package de.hs_heilbronn.floribot.android.floribot_hmi.data;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import de.hs_heilbronn.floribot.android.floribot_hmi.R;
import de.hs_heilbronn.floribot.android.floribot_hmi.communication.MyCustomEvent;


/**
 * Created by mr on 12.06.14.
 */
public class DataAcquisition extends Thread implements SensorEventListener {

    private final Context context;
    private final MyCustomEvent myCustomEvent;
    private Handler loopHandler;
    private SensorManager sensorManager;

    private float[] sensorDataRot = new float[3];
    private double alpha = 0;
    private boolean calibrateSensor;
    private int[] buttonArray = new int[10];

    public DataAcquisition(Context context, MyCustomEvent myCustomEvent) {
        this.context = context;
        this.myCustomEvent = myCustomEvent;
    }

    public void run() {
        // Get sensor object and acc sensor
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        final Sensor accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Looper.prepare();
        // Handler to cancel the message loop
        loopHandler = new Handler();
        // Set event listener (listen to events on mode buttons)
        myCustomEvent.setMyCustomEventListener(new MyCustomEvent.MyCustomEventListener() {
            @Override
            public void myCustomEvent(int mode) {
                // Differentiate between control mode
                switch (mode) {
                    case (0):
                        // Manual mode with joystick buttons
                        Log.d("@DataAcquisition->run", "Manual mode with joystick buttons");
                        unregisterSensorListener();
                        break;
                    case (1):
                        // Automatic mode
                        Log.d("@DataAcquisition->run", "Auto mode");
                        unregisterSensorListener();
                        int[] buttonData = getButtonArray();
                        // Send sensor data to robot
                        float[] axesData = new float[3];
                        sendDataToPublisher(buttonData, axesData);
                        break;
                    case (3):
                        // Manual mode with sensor
                        unregisterSensorListener();
                        sensorManager.registerListener(DataAcquisition.this, accSensor, SensorManager.SENSOR_DELAY_NORMAL, loopHandler);
                        Log.d("@DataAcquisition->run", "Manual mode with sensor button");
                        break;
                }
            }


        });
        Looper.loop();
    }

    public void startThread() {
        this.start();
    }

    public void stopThread(){
        loopHandler.getLooper().quit();
    }

    private void unregisterSensorListener() {
        sensorManager.unregisterListener(DataAcquisition.this);
        calibrateSensor = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float[] sensorData = event.values;
            if (!calibrateSensor) {
                // If we want display in landscape (not in reverse landscape -> by default) then we need the positive of alpha!
                alpha = -(((Math.PI / 2) / 9.81) * sensorData[2]);
                calibrateSensor = true;
            }

            // Rotation matrix around y axes
            double[][] Rot_y = {{Math.cos(alpha), 0, Math.sin(alpha)}, {0, 1, 0}, {-Math.sin(alpha), 0, Math.cos(alpha)}};

            for(int i=0;i<sensorDataRot.length;i++) sensorDataRot[i] = 0;

            // Transform sensor vector with rotation matrix
            for (int i = sensorData.length - 1; i >= 0; i--) {
                for (int j = 0; j < sensorData.length; j++) {
                    sensorDataRot[i] += Rot_y[i][j] * sensorData[j];
                    //Log.d("Rot_y[" + i + "][" + j + "] = ", String.valueOf(Rot_y[i][j]));
                }
            }

            int[] buttonData = getButtonArray();

            // Send sensor data to robot
            sendDataToPublisher(buttonData, sensorDataRot);
        }
    }

    public int[] getButtonArray(){
        // Handler to receive button states from executeActivity in main thread
        DataSet.handlerForControlDataAcquisition = new Handler() {
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                if (bundle != null) {
                    buttonArray = bundle.getIntArray(context.getResources().getString(R.string.button_state_array));
                }
            }
        };
        return buttonArray;
    }

    private void sendDataToPublisher(int[] buttonData, float[] sensorDataRot) {
        Bundle bundle = new Bundle();
        Message msg1 = new Message();
        Message msg2 = new Message();

        bundle.putFloatArray(context.getResources().getString(R.string.axes_state_array), sensorDataRot);
        bundle.putIntArray(context.getResources().getString(R.string.button_state_array), buttonData);

        msg1.setData(bundle);
        msg2.setData(bundle);

        DataSet.handlerForPublishingData.sendMessage(msg1);
        DataSet.handlerForVisualization.sendMessage(msg2);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {


    }

}
