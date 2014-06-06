package de.hs_heilbronn.floribot.android.floribot_hmi.communication;

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
import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.JoystickEventExecutor;

/**
 * Created by mr on 17.05.14.
 *
 * Within this class there are the thread for collecting sensor data and the joystick event data
 */
public class ControlDataAcquisition {

    private final Context context;

    public Thread sensorDataAcquisitionThread, joystickDataAcquisitionThread, joystickEventExecutorThread;
    private Handler loopHandler;

    private SensorManager sensorManager;

    private int[] buttonData = new int[10];
    private float[] axesData = {0,0,0};
    private float[] sensorDataRot = new float[3];
    private double alpha = 0;
    private boolean listenToSensor;
    private boolean firstStart;

    public ControlDataAcquisition(Context context) {
        this.context = context;
    }

    public void startControlDataAcquisitionThread(String controlMode) {

        if (controlMode.equals(context.getResources().getString(R.string.control_mode_sensor))) {
            firstStart = true;
            // Start thread, if no thread is alive
            if (sensorDataAcquisitionThread == null) {
                Log.d("@ControlDataAcquisition->startControlDataAcquisitionThread: ", "Start sensor acquisition thread...");
                listenToSensor = true;
                sensorDataAcquisitionThread = new SensorDataAcquisitionThread();
                sensorDataAcquisitionThread.start();
            }
        }
        if (controlMode.equals(context.getResources().getString(R.string.control_mode_joystick))) {
            DataSet.isRunning = true;

            // Start thread, if no thread is alive
            if (joystickDataAcquisitionThread == null) {
                Log.d("@ControlDataAcquisition->startControlDataAcquisitionThread: ", "Start joystick acquisition thread...");
                joystickDataAcquisitionThread = new JoystickDataAcquisitionThread();
                joystickDataAcquisitionThread.start();
            }
        }
    }

    public void stopControlDataAcquisitionThread(String controlMode) {

        if (controlMode.equals(context.getResources().getString(R.string.control_mode_sensor))) {
            // Unregister sensor listener and stop message loop inside thread
            try {
                listenToSensor = false;
                while (sensorManager != null) {
                    // Log.d("@SensorDataAcquisition#stopSensorDataAcquisitionThread: ", "sensorManager not null");
                }
                loopHandler.getLooper().quit();
            } catch (Exception e) {
                Log.d("@ControlDataAcquisition->stopControlDataAcquisitionThread->SensorControl->StopHandlerException: ", String.valueOf(e));
            }
            // Stop thread
            try {
                while (sensorDataAcquisitionThread.isAlive()) {
                }
                sensorDataAcquisitionThread = null;
            } catch (Exception e) {
                Log.d("@ControlDataAcquisition->stopControlDataAcquisitionThread->SensorControl->StopThreadException: ", String.valueOf(e));
            }
        }
        if (controlMode.equals(context.getResources().getString(R.string.control_mode_joystick))) {
            try {
                DataSet.isRunning = false;
                joystickEventExecutorThread = null;

                loopHandler.getLooper().quit();
            } catch (Exception e) {
                Log.d("@ControlDataAcquisition->stopControlDataAcquisitionThread->JoystickControl->StopHandlerException: ", String.valueOf(e));
            }
            // Stop thread
            try {
                while (joystickDataAcquisitionThread.isAlive()) {
                    Log.d("@ControlDataAcquisition->stopControlDataAcquisitionThread->JoystickControl: ", " is alive...");
                }
                joystickDataAcquisitionThread = null;
            } catch (Exception e) {
                Log.d("@ControlDataAcquisition->stopControlDataAcquisitionThread->JoystickControl->StopThreadException: ", String.valueOf(e));
            }
        }
    }

    public class SensorDataAcquisitionThread extends Thread {

        public SensorListener sensorListener = new SensorListener();

        public void run() {
            try {
                // Get sensor object and acc sensor
                sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                Sensor accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

                Looper.prepare();
                // Handler to cancel the message loop
                loopHandler = new Handler();
                // Register listener to sensor
                sensorManager.registerListener(sensorListener, accSensor, SensorManager.SENSOR_DELAY_NORMAL, loopHandler);
                Looper.loop();
            } catch (Exception e) {
                Log.d("ControlDataAcquisition->SensorDataAcquisitionThread->Run->LoopException: ", e.toString());
            }
        }

        private class SensorListener implements SensorEventListener {

            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            public void onSensorChanged(SensorEvent sensorEvent) {

                if (listenToSensor) {
                    if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        float[] sensorData = sensorEvent.values;
                        if (firstStart) {
                            // If we want display in landscape (not in reverse landscape -> by default) then we need the positive of alpha!
                            alpha = -(((Math.PI / 2) / 9.81) * sensorData[2]);
                            firstStart = false;
                        }

                        // Rotation matrix around y axes
                        double[][] Rot_y = {{Math.cos(alpha), 0, Math.sin(alpha)}, {0, 1, 0}, {-Math.sin(alpha), 0, Math.cos(alpha)}};

                        sensorDataRot[0] = 0;
                        sensorDataRot[1] = 0;
                        sensorDataRot[2] = 0;
                        // Transform sensor vector with rotation matrix
                        for (int i = 0; i < sensorData.length; i++) {
                            for (int j = 0; j < sensorData.length; j++) {
                                sensorDataRot[i] += Rot_y[i][j] * sensorData[j];
                                //Log.d("Rot_y[" + i + "][" + j + "] = ", String.valueOf(Rot_y[i][j]));
                            }
                        }

                        // Handler to receive date and publish
                        DataSet.handlerForControlDataAcquisition = new Handler() {
                            public void handleMessage(Message msg) {
                                Bundle bundle = msg.getData();
                                if (bundle != null) {
                                    buttonData = bundle.getIntArray(context.getResources().getString(R.string.button_state_array));
                                }
                            }
                        };

                        // -----------------------------
                        // Coordinate transformation!!!
                        // -----------------------------

                        // Send sensor data to robot
                        Bundle bundle = new Bundle();
                        Message msg1 = new Message();
                        Message msg2 = new Message();

                        bundle.putFloatArray(context.getResources().getString(R.string.axes_state_array), sensorDataRot);
                        bundle.putIntArray(context.getResources().getString(R.string.button_state_array), buttonData);

                        msg1.setData(bundle);
                        msg2.setData(bundle);
                        if (DataSet.controlDataAcquisition.sensorDataAcquisitionThread != null) {
                            if (DataSet.controlDataAcquisition.sensorDataAcquisitionThread.isAlive()) {
                                DataSet.handlerForPublishingData.sendMessage(msg1);
                                DataSet.handlerForVisualization.sendMessage(msg2);
                            }
                        }
                    }
                } else {
                    Log.d("@ControlDataAcquisition->SensorDataAcquisitionThread->onSensorChanged: ", "Unregister sensor listener.");
                    sensorManager.unregisterListener(this);
                    sensorManager = null;
                }
            }
        }
    }

    public class JoystickDataAcquisitionThread extends Thread {

        public void run() {
            // Start joystick executor thread
            joystickEventExecutorThread = new JoystickEventExecutor(context);
            joystickEventExecutorThread.start();

            try {
                Looper.prepare();
                // Handler to cancel the message loop
                loopHandler = new Handler();

                // Handler to receive date and publish
                DataSet.handlerForControlDataAcquisition = new Handler() {
                    public void handleMessage(Message msg) {
                        Bundle bundle = msg.getData();
                        if (bundle != null) {
                            buttonData = bundle.getIntArray(context.getResources().getString(R.string.button_state_array));
                            float speed = (float) bundle.getInt(context.getResources().getString(R.string.speed));
                            // Check if actual speed is different from last to avoid for loop execution
                            if (speed != axesData[0]) {
                                for (int i = 0; i <= axesData.length - 1; i++) {
                                    axesData[i] = speed;
                                }
                            }

                        }
                    }
                };
                JoystickEventExecutor joystickEventExecutor = new JoystickEventExecutor(context);
                joystickEventExecutor.setJoystickEventListener(new JoystickEventExecutor.JoystickEventListener() {
                    @Override
                    public void joystickEvent() {
                        // Send sensor data to robot
                        Bundle dataBundle = new Bundle();
                        Message msg1 = new Message();
                        Message msg2 = new Message();

                        dataBundle.putFloatArray(context.getResources().getString(R.string.axes_state_array), axesData);
                        dataBundle.putIntArray(context.getResources().getString(R.string.button_state_array), buttonData);

                        msg1.setData(dataBundle);
                        msg2.setData(dataBundle);

                        // !!!!!!!!!!!!!!!!
                        // THIS CAN BE REMOVED!!!!!!
                        // !!!!!!!!!!!!!!!!
                        if (DataSet.controlDataAcquisition.joystickDataAcquisitionThread != null) {
                            if (DataSet.controlDataAcquisition.joystickDataAcquisitionThread.isAlive()) {
                                DataSet.handlerForPublishingData.sendMessage(msg1);
                            }
                        }
                    }
                });

                Looper.loop();
            } catch (Exception e) {
                Log.d("ControlDataAcquisition->JoystickDataAcquisitionThread->Run->LoopException: ", e.toString());
            }
        }
    }
}





