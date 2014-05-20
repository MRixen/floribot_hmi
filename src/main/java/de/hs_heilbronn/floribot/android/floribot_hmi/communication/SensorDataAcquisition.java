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

import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;

/**
 * Created by mr on 17.05.14.
 */
public class SensorDataAcquisition {

    private final Context context;
    private Handler loopHandler;
    private SensorManager sensorManager;
    private Sensor mSensor;
    private int[] buttonData = new int[10];
    private float[] axesData = new float[3];
    public Thread t;
    private boolean listenToSensor;
    private float[] axesDataOld = new float[3];
    private boolean firstRunFlag;
    private double alpha = 0;
    float[] sensorDataRot = new float[3];
    private boolean firstStart;

    public SensorDataAcquisition(Context context) {
        this.context = context;
    }

    public void startSensorDataAcquisitionThread() {

        firstStart = true;
        // Start thread, if no thread is alive
        if (t == null) {
            Log.d("@SensorDataAcquisition#startSensorDataAcquisitionThread: ","Start sensor acquisition thread...");
            listenToSensor = true;
            t = new SensorDataAcquisitionThread();
            t.start();
        }
    }

    public void stopSensorDataAcquisitionThread(){
        // Unregister sensor listener and stop message loop inside thread
        try {
            listenToSensor = false;
            while(sensorManager != null){
               // Log.d("@SensorDataAcquisition#stopSensorDataAcquisitionThread: ", "sensorManager not null");
            }
            loopHandler.getLooper().quit();
        }catch(Exception e){
            Log.d("@SensorDataAcquisition#stopSensorDataAcquisitionThread#StopHandlerException: ", String.valueOf(e));
        }
        // Stop thread
        try {
            while (t.isAlive()) {
            }
            t = null;
        }catch(Exception e){
            Log.d("@SensorDataAcquisition#stopSensorDataAcquisitionThread#StopThreadException: ", String.valueOf(e));
        }
    }

    public class SensorDataAcquisitionThread extends Thread{

    public MySensorListener msl = new MySensorListener();

        public void run () {

        try {
            firstRunFlag = true;
            // Get sensor object
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            // Get acc sensor
            mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            Looper.prepare();
            // Handler to cancel the message loop
            loopHandler = new Handler();
            // Register listener to sensor
            sensorManager.registerListener(msl, mSensor, SensorManager.SENSOR_DELAY_NORMAL, loopHandler);
            Looper.loop();
        } catch (Exception e) {
            Log.d("SensorDataAcquisition#Run#LoopException: ", e.toString());
        }
    }

        private class MySensorListener implements SensorEventListener {

            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            public void onSensorChanged(SensorEvent sensorEvent) {

                if(listenToSensor) {
                    if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {



                        float[] sensorData = sensorEvent.values;
                        if (firstStart) {
                            // If we want landscape then we need the positive!!!
                            alpha = -( ((Math.PI / 2) / 9.81) * sensorData[2]) ;
                            Log.d("alpha: ", String.valueOf(alpha));
                            Log.d("firstStart: ", String.valueOf(firstStart));
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
                                Log.d("Rot_y[" + i + "][" + j + "] = ", String.valueOf(Rot_y[i][j]));
                            }
                        }

                        buttonData[DataSet.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal()] = 1;

                        // -----------------------------
                        // Coordinate transformation!!!
                        // -----------------------------

                        // Send sensor data to robot
                        Bundle bundle = new Bundle();
                        Message msg1 = new Message();
                        Message msg2 = new Message();

                        bundle.putFloatArray("axesData", sensorDataRot);
                        bundle.putIntArray("buttonData", buttonData);

                        msg1.setData(bundle);
                        msg2.setData(bundle);
                        if (DataSet.sensorDataAcquisition.t != null) {
                            if (DataSet.sensorDataAcquisition.t.isAlive()) {
                                DataSet.handler.sendMessage(msg1);
                                DataSet.handlerForVisualization.sendMessage(msg2);
                            }
                        }
                        //Log.d("@onSensorChanged#run: ", Thread.currentThread().getName());
                    }
                }
                else {
                    Log.d("@SensorDataAcquisition#onSensorChanged: ", "Unregister sensor listener...");
                    sensorManager.unregisterListener(this);
                    sensorManager = null;
                }
            }
        }
    }
}

