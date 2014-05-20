package de.hs_heilbronn.floribot.android.floribot_hmi.communication;

import android.content.Context;
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
import sensor_msgs.Joy;

/**
 * Created by mr on 17.05.14.
 */
public class Talker2  extends AbstractNodeMain {

    private final Context context;
    private DataSet.DriveMode driveMode;
    public Thread t;
    private Handler loopHandler = null;
    private Publisher<Joy> publisher;

    public Talker2(Context context) {
        this.context = context;
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

    public void startPublisherThread() {

        // Start thread, if no thread is alive
        if (t == null) {
            Log.d("@Talker2#startPublisherThread: ","Start thread...");
            t = new PublisherThread();
            t.start();
        }
    }

    public void stopPublisherThread(){
        // Stop message loop inside thread
        try {
            loopHandler.getLooper().quit();
        }catch(Exception e){
            Log.d("@Talker2#stopPublisherThread#StopHandlerException: ", String.valueOf(e));
        }
        // Stop thread
        try {
            while (t.isAlive()) {
            }
            t = null;
        }catch(Exception e){
            Log.d("@Talker2#stopPublisherThread#StopThreadException: ", String.valueOf(e));
        }
    }

    public class PublisherThread extends Thread {

        public void run() {
            try {
                Looper.prepare();

                    // Handler to cancel the message loop
                    loopHandler = new Handler();

                    // Handler to receive date and publish
                    DataSet.handler = new Handler() {
                        public void handleMessage(Message msg) {

                            Bundle bundle = msg.getData();

                            if (bundle != null) {
                                float[] axesData = bundle.getFloatArray("axesData");
                                int[] buttonData = bundle.getIntArray("buttonData");

                                // Send sensor data to robot
                                Joy joyTest = publisher.newMessage();
                                joyTest.setAxes(axesData);
                                joyTest.setButtons(buttonData);
                                publisher.publish(joyTest);
                            }
                        }
                    };

                Looper.loop();
            } catch (Exception e) {
                Log.d("SensorData#Run#Looper: ", e.toString());
            }
        }
    }
}
