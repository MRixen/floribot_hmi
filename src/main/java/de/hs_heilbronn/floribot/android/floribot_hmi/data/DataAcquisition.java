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
    private final Object object = new Object();
    private Handler loopHandler;
    private SensorManager sensorManager;
    private AccEvent accEvent;
    private double alpha = 0;
    private boolean calibrateSensor;
    private Bundle stateBundle;
    private int[] buttonData = new int[10];
    private float[] axesData = new float[3];
    private boolean startCalibration;
    private Thread thread;

    public DataAcquisition(Context context, MyCustomEvent myCustomEvent) {
        this.context = context;
        this.myCustomEvent = myCustomEvent;
    }

    public void run() {
        //Log.d("@DataAcquisition->run", Thread.currentThread().getName());
        // Get sensor object and acc sensor
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        final Sensor accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // Start acc event executor
        accEvent = new AccEvent(context);
        accEvent.start();

        Looper.prepare();
        // Handler to cancel the message loop
        loopHandler = new Handler();

        // Handler to receive button states from executeActivity in main thread
        DataSet.handlerForControlDataAcquisition = new Handler() {
            public void handleMessage(Message msg) {

                stateBundle = msg.getData();

                // Get button state array from main thread
                if (stateBundle != null) {
                    if (stateBundle.containsKey(context.getResources().getString(R.string.button_state_array))) {
                        synchronized (object) {
                            Log.d("@DataAcquisition->run", "Get button data");
                            buttonData = stateBundle.getIntArray(context.getResources().getString(R.string.button_state_array));
                        }
                    }
                    // Get axes data from main thread (only in manual drive mode with joystick buttons)
                    if (stateBundle.containsKey(context.getResources().getString(R.string.speed))) {
                        Log.d("@DataAcquisition->run", "Get speed data");
                        float speed = (float) stateBundle.getInt(context.getResources().getString(R.string.speed));
                        // Check if actual speed is different from last to avoid for loop execution
                        synchronized (object) {
                            if (speed != axesData[0]) {
                                for (int i = 0; i <= axesData.length - 1; i++) {
                                    axesData[i] = speed;
                                }
                            }
                        }
                    }
                    // Check if the user want to calibrate sensor
                    if (stateBundle.containsKey(context.getResources().getString(R.string.start_sensor_calibration))) {
                        startCalibration = stateBundle.getBoolean(context.getResources().getString(R.string.start_sensor_calibration));
                        // Unregister sensor listener to provide new calibration by pressing the sensor mode button
                        unregisterSensorListener();
                        // Register sensor listener
                        sensorManager.registerListener(DataAcquisition.this, accSensor, SensorManager.SENSOR_DELAY_NORMAL, loopHandler);
                        Log.d("@DataAcquisition->registerAccEventListener", "start sensor calibration");
                    }
                    // Send response for automatic drive mode
                    if(DataSet.DriveMode.AUTOMATIC_DRIVE.ordinal() == 1){
                        synchronized (object) {
                            sendDataToNode(buttonData, null);
                        }
                    }
                    // Send response for manual drive mode
                    else if(buttonData[DataSet.DriveMode.MANUAL_DRIVE.ordinal()] == 1){
                        synchronized (object) {
                            sendDataToNode(buttonData, null);
                        }
                    }
                }
            }
        };

        // Registering acceleration event listener (for manual drive mode with joystick buttons)
        accEvent.registerAccEventListener(new AccEvent.AccEventListener() {
            @Override
            public void customEvent() {
                synchronized (object) {
                    // Send only data if one of the joystick button is pressed
                    if(buttonData[DataSet.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal()] != 1 &&
                            ( buttonData[DataSet.DriveMode.MOVE_FORWARD_WITH_BUTTON.ordinal()] == 1 ||
                                    buttonData[DataSet.DriveMode.MOVE_BACKWARD_WITH_BUTTON.ordinal()] == 1 ||
                                    buttonData[DataSet.DriveMode.TURN_LEFT_WITH_BUTTON.ordinal()] == 1 ||
                                    buttonData[DataSet.DriveMode.TURN_RIGHT_WITH_BUTTON.ordinal()] == 1)){
                        Log.d("@DataAcquisition->run", "send data to publisher");
                        sendDataToNode(buttonData, axesData);
                    }

                }
            }
        });
        Looper.loop();
    }

    public void startThread() {
        if(thread == null){
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stopThread() {
        unregisterSensorListener();
        if(accEvent.getEventListener() != null) accEvent.unregisterAccEventListener(null);
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

            // Transform sensor vector with rotation matrix
            float[] axesData = new float[3];
            int counter = axesData.length;

            for (int i = 0; i < sensorData.length; i++) {
                for (int j = 0; j < sensorData.length; j++) {
                    // Calculate second and third value only
                    if(i > 0) axesData[counter] += Rot_y[i][j] * sensorData[j];
                }
                counter--;
                Log.d("for", String.valueOf(counter));
            }
            // The axis are interchanged
            // Correct configuration should be [roll, pitch, yaw]
            // Actual configuration is [yaw, pitch, roll]
            // Therefor the axis need to be rearrange --> CHECK PLAUSIBILITY!!!
            axesData[0] = axesData[2];
            axesData[2] = 0;



            // Send sensor data to robot
            synchronized (object) {
                // Send only if the sensor joystick button is pressed
                if(buttonData[DataSet.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal()] == 1) {
                    sendDataToNode(buttonData, axesData);
                }
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void sendDataToNode(int[] buttonData, float[] axesData) {
        Bundle bundle = new Bundle();
        Message msg1 = new Message();
        Message msg2 = new Message();

        if(axesData != null) bundle.putFloatArray(context.getResources().getString(R.string.axes_state_array), axesData);
        if(buttonData != null) bundle.putIntArray(context.getResources().getString(R.string.button_state_array), buttonData);

        msg1.setData(bundle);
        msg2.setData(bundle);

        if(DataSet.handlerForPublishingData != null) DataSet.handlerForPublishingData.sendMessage(msg1);
        if(DataSet.handlerForVisualization != null) DataSet.handlerForVisualization.sendMessage(msg2);
    }
}
