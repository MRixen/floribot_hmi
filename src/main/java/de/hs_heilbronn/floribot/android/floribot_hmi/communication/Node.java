package de.hs_heilbronn.floribot.android.floribot_hmi.communication;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.ros.internal.node.RegistrantListener;
import org.ros.internal.node.client.Registrar;
import org.ros.internal.node.topic.SubscriberIdentifier;
import org.ros.internal.node.xmlrpc.XmlRpcEndpoint;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeListener;
import org.ros.node.topic.DefaultPublisherListener;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.PublisherListener;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import de.hs_heilbronn.floribot.android.floribot_hmi.R;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.AccelerationEvent;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;
import sensor_msgs.Joy;
import sensor_msgs.JoyFeedback;
import sensor_msgs.JoyFeedbackArray;

/**
 * Created by mr on 12.06.14.
 */
public class Node extends AbstractNodeMain implements RegistrantListener<Joy>{
    private org.ros.node.topic.Subscriber<JoyFeedbackArray> subscriber;
    private org.ros.node.topic.Publisher<Joy> publisher;

    public Thread nodeThread;
    private Handler threadHandler = null;
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
    public void onStart(final ConnectedNode connectedNode) {
        // Create publisher, subscriber and start thread
        publisher = connectedNode.newPublisher(topicPublisher, Joy._TYPE);
        subscriber = connectedNode.newSubscriber(topicSubscriber, sensor_msgs.JoyFeedbackArray._TYPE);

        startNodeThread();
    }

    public void startNodeThread() {
        // Start thread, if no thread is alive
        if (nodeThread == null) {
            Log.d("@ExecutorNode->startNodeThread", "Start thread...");
            nodeThread = new NodeThread();
            nodeThread.start();
        }
    }

    public void stopNodeThread(){
        try {
            threadHandler.getLooper().quit();
        }catch(Exception e){
            Log.d("@Subscriber#stopNodeThread#StopHandlerException: ", String.valueOf(e));
        }
        // Stop thread
        try {
            while (nodeThread.isAlive()) {
            }
            nodeThread = null;
        }catch(Exception e){
            Log.d("@Subscriber#stopNodeThread#StopThreadException: ", String.valueOf(e));
        }
    }

    @Override
    public void onMasterRegistrationSuccess(Joy joy) {
        Log.d("onMasterRegistrationSuccess", "onMasterRegistrationSuccess");

    }

    @Override
    public void onMasterRegistrationFailure(Joy joy) {
        Log.d("onMasterRegistrationSuccess", "onMasterRegistrationSuccess");
    }

    @Override
    public void onMasterUnregistrationSuccess(Joy joy) {
        Log.d("onMasterRegistrationSuccess", "onMasterRegistrationSuccess");
    }

    @Override
    public void onMasterUnregistrationFailure(Joy joy) {
        Log.d("onMasterRegistrationSuccess", "onMasterRegistrationSuccess");
    }


    public class NodeThread extends Thread {
        public void run() {
            Log.d("@Subscriber->startNodeThread", "Node started...");

            Looper.prepare();
            Log.d("Subscriber:", Thread.currentThread().getName());

            // Handler to cancel the message loop
            threadHandler = new Handler();

            // Handler to receive data and publish
            BaseClass.sendToNode = new Handler() {
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
                        Joy joyMessage = publisher.newMessage();
                        joyMessage.setAxes(axesData);
                        joyMessage.setButtons(buttonData);
                        publisher.publish(joyMessage);
                    }
                }
            };

            // Receive data from topic
            subscriber.addMessageListener(new MessageListener<JoyFeedbackArray>() {

                @Override
                public void onNewMessage(JoyFeedbackArray message) {
                    List<JoyFeedback> messageList = message.getArray();
                    BaseClass.subscriberMessageListener.onNewMessage(messageList);

                }
            });
            Looper.loop();
        }
    }

}
