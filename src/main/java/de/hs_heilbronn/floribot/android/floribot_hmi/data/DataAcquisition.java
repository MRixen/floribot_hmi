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
    private boolean stopSending, phoneInArea;
    private Activity activity;

    public DataAcquisition(Context context, MyCustomEvent myCustomEvent) {
        this.context = context;
        this.myCustomEvent = myCustomEvent;
        activity = new Activity();
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
        BaseClass.handlerForControlDataAcquisition = new Handler() {
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
                    if(BaseClass.DriveMode.AUTOMATIC_DRIVE.ordinal() == 1){
                        synchronized (object) {
                            sendDataToNode(buttonData, null);
                        }
                    }
                    // Send response for manual drive mode
                    else if(buttonData[BaseClass.DriveMode.MANUAL_DRIVE.ordinal()] == 1){
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
                    if(buttonData[BaseClass.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal()] != 1 &&
                            ( buttonData[BaseClass.DriveMode.MOVE_FORWARD_WITH_BUTTON.ordinal()] == 1 ||
                                    buttonData[BaseClass.DriveMode.MOVE_BACKWARD_WITH_BUTTON.ordinal()] == 1 ||
                                    buttonData[BaseClass.DriveMode.TURN_LEFT_WITH_BUTTON.ordinal()] == 1 ||
                                    buttonData[BaseClass.DriveMode.TURN_RIGHT_WITH_BUTTON.ordinal()] == 1)){
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
        stopSending = false;
        phoneInArea = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float[] sensorData = event.values;
            if (!calibrateSensor) {
                // If we want display in landscape (not in reverse landscape -> by default) then we need the positive of alpha!
                //alpha = -(((Math.PI / 2) / 9.81) * sensorData[2]);
                double gz = sensorData[2];
                double g = 9.81;
                alpha = -(Math.PI/2)+Math.acos(gz / g);
                calibrateSensor = true;
            }

            // Rotation matrix around y axes
            //double[][] Rot_y = {{Math.cos(alpha), 0, Math.sin(alpha)}, {0, 1, 0}, {-Math.sin(alpha), 0, Math.cos(alpha)}};
           /* // Transform sensor vector with rotation matrix
            float[] axesData = new float[3];*/
            //int counter = axesData.length-1;

            /*for (int i = 0; i < sensorData.length; i++) {
                for (int j = 0; j < sensorData.length; j++) {
                    // Calculate second and third value only
                    if(i > 0) axesData[counter] += Rot_y[i][j] * sensorData[j];
                }
                counter--;
            }*/


            double[] Ry = {-Math.sin(alpha), 0, Math.cos(alpha)};
            float[] axesData = new float[3];

            // WII CONTROLLER
            // Rotation around x stands for robot translation
            // Rotation around y stands for robot rotation

            // PHONE
            // Rotation around y stands for robot translation
            // Rotation around -x stands for robot rotation
            for (int i = 0; i < sensorData.length; i++) {
                    // Calculate z value only
                    axesData[1] += Ry[i] * sensorData[i];
            }

            axesData[0] = -sensorData[1];
            /*axesData[0] = -axesData[2];
            axesData[2] = 0;*/

            //
            // CHECK PLAUSIBILITY
            // Check if phone is in calibration area
            // Phone is in calibration area when the first sensor value is smaller than 1
            if(!phoneInArea && !stopSending) {
                if (Math.abs(axesData[1]) >= 1) {
                    stopSending = true;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context,context.getResources().getString(R.string.phone_calibration_area),Toast.LENGTH_LONG).show();
                        }
                    });
                } else phoneInArea = true;
            }

            // Send sensor data to robot if calibration is successfully
            if(!stopSending) {
                Log.d("sensorData", axesData[0] + " / " + axesData[1] + " / " + axesData[2]);
                synchronized (object) {
                    // Send only if the sensor joystick button is pressed
                    if (buttonData[BaseClass.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal()] == 1) {
                        sendDataToNode(buttonData, axesData);
                    }
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

        if(BaseClass.handlerForPublishingData != null) BaseClass.handlerForPublishingData.sendMessage(msg1);
        if(BaseClass.handlerForVisualization != null) BaseClass.handlerForVisualization.sendMessage(msg2);
    }
}
