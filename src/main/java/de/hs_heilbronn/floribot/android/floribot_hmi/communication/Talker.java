/*
package de.hs_heilbronn.floribot.android.floribot_hmi.communication;

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

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;
import de.hs_heilbronn.floribot.android.floribot_hmi.ExecuteActivity;
import sensor_msgs.Joy;

public class Talker extends AbstractNodeMain {

    private final Context _context;
    public Activity _activity;

    private Publisher<Joy> publisher;
    private SensorManager sensorManager;
    private Sensor mSensor;
    private Handler loopHandlerManual = null;
    private Handler loopHandlerAutomatic = null;
    private thread t;
    private int state;
    private Handler mHandler;


    public Talker(Context context) {
    this._context = context;
}

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("rosjava/talker");
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        // Create publisher
        publisher = connectedNode.newPublisher("chatter", Joy._TYPE);
    }

    public void startSendingData(int state) {
        //
        this.state = state;

        // Start thread, if no thread is alive
        if (t == null) {
            ExecuteActivity.checkState = false;
            t = new thread();
            t.start();
        }
        else{
            // It's necessary to re-init a thread (stop old and start new one)
            // to get new parameters (AUTO, MANUAL, etc.) in the loop

            // Stop old thread
            if(state == DataSet.Buttons.MOVE_ROBOT_WITH_IMU.ordinal()){
                loopHandlerAutomatic.getLooper().quit();
            }
            ExecuteActivity.checkState = true;
            try {
                while (t.isAlive()) {
                    Log.d("@stopSendingData#While: ", "t is alive!!");
                }
                t = null;
            }catch(Exception e){
                Log.d("@stopSendingData#Exception: ", String.valueOf(e));
            }
            // Start new thread
            ExecuteActivity.checkState = false;
            t = new thread();
            t.start();


        }
    }

    public void stopSendingData(){
        // Kill sending thread
        ExecuteActivity.checkState = true;
        if(loopHandlerAutomatic != null){
            loopHandlerAutomatic.getLooper().quit();
        }
        try {
            while (t.isAlive()) {
            }
            t = null;
        }catch(Exception e){
            Log.d("@stopSendingData#Exception: ", String.valueOf(e));
        }
    }

    private class thread extends Thread {

        public MySensorListener msl = new MySensorListener();

        public void run() {
            try {
                Looper.prepare();

                if(state == DataSet.Buttons.MOVE_ROBOT_WITH_IMU.ordinal()){
                    // Handler to cancel the message loop
                    loopHandlerManual = new Handler();
                    Log.d("@Talker#run: ", "Manual drive");
                    // Get sensor object
                    sensorManager = (SensorManager) _context.getSystemService(Context.SENSOR_SERVICE);
                    // Get acc sensor
                    mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    // Register listener to sensor
                    sensorManager.registerListener(msl, mSensor, SensorManager.SENSOR_DELAY_NORMAL, loopHandlerManual);
                }

                if (state == DataSet.Buttons.AUTOMATIC_DRIVE.ordinal()) {
                    // Handler to cancel the message loop
                    loopHandlerAutomatic = new Handler();
                    Log.d("@Talker#run: ", "Automatic drive");
                    mHandler = new Handler() {
                        public void handleMessage(Message msg) {

                            Bundle bundle = msg.getData();

                            if (bundle != null) {
                                int i = bundle.getInt("message");
                                Log.d("SensorData#Run#Message: ", String.valueOf(i));

                                float[] myAxesData = {0, 0, 0};
                                int[] myButtonData = new int[10];
                                myButtonData[state] = 1;
                                // Send sensor data to robot
                                publishingData(myAxesData, myButtonData);
                            }
                        }
                    };
                    testMethodForAuto();

                }

                Looper.loop();
            } catch (Exception e) {
                Log.d("SensorData#Run#Looper: ", e.toString());
            }
        }

        private class MySensorListener implements SensorEventListener {

            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            public void onSensorChanged(SensorEvent sensorEvent) {
                if (!ExecuteActivity.checkState) {
                    if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                        float[] myAxesData = sensorEvent.values;
                        int[] myButtonData = new int[10];
                        myButtonData[state] = 1;

                        // Send sensor data to robot
                        publishingData(myAxesData, myButtonData);
                        Log.d("@onSensorChanged#run: ", Thread.currentThread().getName());
                    }
                } else {
                    //Log.d("@onShutdown: ", "Unregister sensor listener...");
                    sensorManager.unregisterListener(this);
                    loopHandlerManual.getLooper().quit();
                }
            }
        }

        private void publishingData(float[] myAxesData, int[] myButtonData){


            // Send sensor data to robot
            Joy joyTest = publisher.newMessage();
            joyTest.setAxes(myAxesData);
            joyTest.setButtons(myButtonData);
            publisher.publish(joyTest);
        }


    }

    private void testMethodForAuto(){

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d("@testMethodForAuto#thread: ", Thread.currentThread().getName());
        Log.d("testMethodForAuto#Inside: ", "ok...");
        Bundle bundle = new Bundle();
        Message msg = new Message();
        bundle.putInt("message", 1);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
*/
