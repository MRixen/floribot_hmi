package de.hs_heilbronn.floribot.android.floribot_hmi.data;

import android.app.Activity;
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
import android.widget.Toast;
import de.hs_heilbronn.floribot.android.floribot_hmi.R;


/**
 * Created by mr on 12.06.14.
 */
public class DataAcquisition extends Thread implements SensorEventListener {

    private final Context context;
    private final Object object = new Object();
    private Handler threadHandler;
    private SensorManager sensorManager;
    private AccelerationEvent accelerationEvent;
    private double alpha = 0;
    private Bundle stateBundle;
    private int[] buttonData = new int[10];
    private float[] axesData = new float[3];
    private Thread dataAcquisitionThread;
    private boolean calibrateSensor, stopSending, phoneInArea;
    private Activity activity;
    private double g = 10;
    private float gz;
    private int driveMode;

    public DataAcquisition(Context context) {
        this.context = context;
        activity = new Activity();
        driveMode = -1;
    }

    public void run() {
        // Get sensor object and acc sensor
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        final Sensor accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // Start acc event executor
        accelerationEvent = new AccelerationEvent(context);
        accelerationEvent.start();

        Looper.prepare();
        // Handler to cancel the message loop
        threadHandler = new Handler();

        // Handler to receive button states from executeActivity in main dataAcquisitionThread
        BaseClass.sendToDataAcquisition = new Handler() {
            public void handleMessage(Message msg) {
                stateBundle = msg.getData();
                // Get button state array from main thread
                if (stateBundle != null) {
                    if (stateBundle.containsKey(context.getResources().getString(R.string.button_state_array))) {
                        synchronized (object) {
                           // Log.d("@DataAcquisition->run", "Get button data");
                            buttonData = stateBundle.getIntArray(context.getResources().getString(R.string.button_state_array));
                        }
                    }
                    axesData = new float[3];
                    // Get axes data from main thread (only in manual drive mode with joystick buttons)
                    if (stateBundle.containsKey(context.getResources().getString(R.string.speed))) {
                        //Log.d("@DataAcquisition->run", "Get speed data");
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
                    // Send response for automatic drive mode and wait for answer from robot to enable the control buttons
                    // NOTE: This isn't implemented yet! There is no response from the robot.
                    // Set drive mode for automatic mode
                    if(buttonData[BaseClass.DriveMode.AUTOMATIC_DRIVE.ordinal()] == 1){
                        synchronized (object) {
                            sendDataToNode(buttonData, null, false);
                        }
                        driveMode = BaseClass.DriveMode.AUTOMATIC_DRIVE.ordinal();
                    }
                    // Send response for manual drive mode and wait for answer from robot to enable the control buttons
                    // NOTE: This isn't implemented yet! There is no response from the robot.
                    // Set drive mode to control with arrow buttons
                    if(buttonData[BaseClass.DriveMode.MANUAL_DRIVE.ordinal()] == 1 && buttonData[BaseClass.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal()] != 1){
                        unregisterSensorListener();
                        driveMode = BaseClass.DriveMode.MANUAL_DRIVE.ordinal();
                        synchronized (object) {
                            sendDataToNode(buttonData, null, false);
                        }
                    }
                    // Set drive mode for control with sensor
                    if(buttonData[BaseClass.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal()] == 1 && buttonData[BaseClass.DriveMode.MANUAL_DRIVE.ordinal()] == 1){
                        driveMode = BaseClass.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal();
                    }
                    // Check if the user want to calibrate sensor
                    if (stateBundle.containsKey(context.getResources().getString(R.string.start_sensor_calibration))) {
                        calibrateSensor = stateBundle.getBoolean(context.getResources().getString(R.string.start_sensor_calibration));
                        // Unregister sensor listener to provide new calibration by pressing the sensor mode button
                        unregisterSensorListener();
                        // Register sensor listener
                        sensorManager.registerListener(DataAcquisition.this, accSensor, SensorManager.SENSOR_DELAY_NORMAL, threadHandler);
                        Log.d("@DataAcquisition->registerAccEventListener", "start sensor calibration");
                    }
                }
                //Log.d("driveMode", String.valueOf(driveMode));
            }

        };


            // Registering acceleration event listener (for manual drive mode with joystick buttons)
            accelerationEvent.registerAccEventListener(new AccelerationEvent.AccEventListener() {
                @Override
                public void onAccEvent() {
                    synchronized (object) {
                        if(driveMode == BaseClass.DriveMode.MANUAL_DRIVE.ordinal() || driveMode == BaseClass.DriveMode.AUTOMATIC_DRIVE.ordinal()) {
                            //Log.d("@DataAcquisition->accelerationEvent", String.valueOf(driveMode));
                            sendDataToNode(buttonData, axesData, false);
                        }
                    }
                }
            });

        Looper.loop();
    }

    public void startThread() {
        if(dataAcquisitionThread == null){
            dataAcquisitionThread = new Thread(this);
            dataAcquisitionThread.start();
        }
    }

    public void stopThread() {
        unregisterSensorListener();
        if(accelerationEvent.getEventListener() != null) accelerationEvent.unregisterAccEventListener(null);
        threadHandler.getLooper().quit();
    }

    private void unregisterSensorListener() {
        sensorManager.unregisterListener(DataAcquisition.this);
        //calibrateSensor = false;
        stopSending = false;
        phoneInArea = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            /*Here you need to add some code to differentiate between two landscape modes (landscape and reverseLandscape)*/
            float[] sensorEventValues = event.values;

                if (calibrateSensor) {
                    gz = sensorEventValues[2];
                    alpha = -(Math.PI / 2) + Math.acos(gz / g);
                    calibrateSensor = false;
                }

                float[] sensorData = new float[3];
                double[] Ry = {-Math.sin(alpha), 0, Math.cos(alpha)};

                for (int i = 0; i < sensorEventValues.length; i++) {
                    // Calculate z value only
                    sensorData[1] += Ry[i] * sensorEventValues[i];
                }
                sensorData[0] = -sensorEventValues[1];
                // Check if phone is in calibration area
                // Phone is in calibration area when the first sensor value (x) is bigger 0 and last (z) is smaller 0
                if (!phoneInArea && !stopSending) {
                    if (Math.abs(gz) > g || ( sensorEventValues[0] > 0 || sensorEventValues[2] < 0 )) {
                        stopSending = true;
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, context.getResources().getString(R.string.phone_calibration_area), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        phoneInArea = true;
                        // dismiss dialog
                        BaseClass.sensorCalibrationListener.onCalibrationSuccess();
                    }
                }

                /*Send data when dead man button is pressed*/
                if (buttonData[BaseClass.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal()] == 1 && buttonData[BaseClass.DriveMode.MANUAL_DRIVE.ordinal()] == 0) {
                    // Send sensor data to robot if calibration is successfully
                    if (!stopSending) {
                        //Log.d("sensorEventValues", sensorData[0] + " / " + sensorData[1] + " / " + sensorData[2]);
                        synchronized (object) {
                            sendDataToNode(buttonData, sensorData, true);
                        }
                    }
                }
                /*Send null for axesData when the sensor calibration button is pressed and the dead man button is not pressed*/
                else if(buttonData[BaseClass.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal()] == 0 ||
                        (buttonData[BaseClass.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal()] == 1 && buttonData[BaseClass.DriveMode.MANUAL_DRIVE.ordinal()] == 1)){
                    synchronized (object) {
                        sendDataToNode(buttonData, null, true);
                    }
                }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void sendDataToNode(int[] buttonData, float[] axesData, boolean showVisualization) {
        Bundle bundle = new Bundle();
        Message msg1 = new Message();
        Message msg2 = new Message();

        if(axesData != null) bundle.putFloatArray(context.getResources().getString(R.string.axes_state_array), axesData);
        if(buttonData != null) bundle.putIntArray(context.getResources().getString(R.string.button_state_array), buttonData);

        msg1.setData(bundle);
        msg2.setData(bundle);

        // Show sensor visualization only in drive showVisualization with sensor
        if(showVisualization) {
            if (BaseClass.sendToNode != null)
                BaseClass.sendToNode.sendMessage(msg1);
            if (BaseClass.sendToSensorVisualization != null)
                BaseClass.sendToSensorVisualization.sendMessage(msg2);
        }
        else{
            if (BaseClass.sendToNode != null) {
                BaseClass.sendToNode.sendMessage(msg1);
            }
        }
    }
}
