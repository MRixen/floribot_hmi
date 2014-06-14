package de.hs_heilbronn.floribot.android.floribot_hmi.communication;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.util.List;

import de.hs_heilbronn.floribot.android.floribot_hmi.R;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;
import sensor_msgs.Joy;
import sensor_msgs.JoyFeedback;
import sensor_msgs.JoyFeedbackArray;

/**
 * Created by mr on 12.06.14.
 */
public class Node extends AbstractNodeMain {
    private org.ros.node.topic.Subscriber<JoyFeedbackArray> subscriber;
    private org.ros.node.topic.Publisher<Joy> publisher;
    public Thread t;
    private Handler loopHandler = null;
    private final String topicSubscriber, topicPublisher, nodeGraphName;
    private final Context context;

    public Node(Context context, String topicSubscriber, String topicPublisher, String nodeGraphName) {
        this.context = context;
        this.topicSubscriber = topicSubscriber;
        this.topicPublisher = topicPublisher;
        this.nodeGraphName = nodeGraphName;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(nodeGraphName);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        // Create publisher
        publisher = connectedNode.newPublisher(topicPublisher, sensor_msgs.Joy._TYPE);
        // Create subscriber
        subscriber = connectedNode.newSubscriber(topicSubscriber, sensor_msgs.JoyFeedbackArray._TYPE);

        startNodeThread();
    }

    public void startNodeThread() {

        // Start thread, if no thread is alive
        if (t == null) {
            Log.d("@ExecutorNode->startNodeThread", "Start thread...");
            t = new NodeThread();
            t.start();
        }
    }

    public void stopNodeThread(){
        try {
            loopHandler.getLooper().quit();
        }catch(Exception e){
            Log.d("@Subscriber#stopNodeThread#StopHandlerException: ", String.valueOf(e));
        }
        // Stop thread
        try {
            while (t.isAlive()) {
            }
            t = null;
        }catch(Exception e){
            Log.d("@Subscriber#stopNodeThread#StopThreadException: ", String.valueOf(e));
        }
    }

    public class NodeThread extends Thread {

        public void run() {

            Log.d("@Subscriber->startNodeThread", "Node started...");

                Looper.prepare();
            Log.d("Subscriber:", Thread.currentThread().getName());

            // Handler to cancel the message loop
            loopHandler = new Handler();

            // Handler to receive data and publish
            DataSet.handlerForPublishingData = new Handler() {
                public void handleMessage(Message msg) {

                    Bundle bundle = msg.getData();

                    if (bundle != null) {
                        float[] axesData = new float[3];
                        int[] buttonData = new int[10];

                        if (bundle.containsKey(context.getResources().getString(R.string.axes_state_array)))
                            axesData = bundle.getFloatArray(context.getResources().getString(R.string.axes_state_array));
                        if (bundle.containsKey(context.getResources().getString(R.string.button_state_array)))
                            buttonData = bundle.getIntArray(context.getResources().getString(R.string.button_state_array));
                        // Send sensor data to robot
                        Joy joyTest = publisher.newMessage();
                        joyTest.setAxes(axesData);
                        joyTest.setButtons(buttonData);
                        publisher.publish(joyTest);

                    }
                }
            };

            // Receive data from topic
            subscriber.addMessageListener(new MessageListener<JoyFeedbackArray>() {

                @Override
                public void onNewMessage(JoyFeedbackArray message) {
                    List<JoyFeedback> messageList = message.getArray();
                    DataSet.subscriberInterface.subscriberCallback(messageList);
                    Log.d("@Subscriber->addMessageListener", "addMessageListener...");
                }
            });


            Looper.loop();

        }

        public Publisher<Joy> getPublisher() {
            return publisher;
        }
    }
}
