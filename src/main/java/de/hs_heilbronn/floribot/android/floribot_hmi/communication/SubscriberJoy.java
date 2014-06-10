package de.hs_heilbronn.floribot.android.floribot_hmi.communication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;

import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;
import sensor_msgs.JoyFeedback;

/**
 * Created by mr on 21.05.14.
 */
public class SubscriberJoy extends AbstractNodeMain implements BaseClass.subscriberInterface{

    private final Context context;
    private final String topicSubscriber;
    org.ros.node.topic.Subscriber<JoyFeedback> subscriber;
    public Thread t;
    private Handler loopHandler = null;

    public SubscriberJoy(Context context, String topicSubscriber) {
        this.context = context;
        this.topicSubscriber = topicSubscriber;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("floribot/subscriber");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        subscriber = connectedNode.newSubscriber(topicSubscriber, std_msgs.String._TYPE);
    }

    public void startSubscriberThread() {

        // Start thread, if no thread is alive
        if (t == null) {
            Log.d("@Listener->startSubscriberThread","Start thread...");
            t = new SubscriberThread();
            t.start();
        }
    }

    public void stopSubscriberThread(){
        try {
            loopHandler.getLooper().quit();
        }catch(Exception e){
            Log.d("@Subscriber#stopSubscriberThread#StopHandlerException: ", String.valueOf(e));
        }
        // Stop thread
        try {
            while (t.isAlive()) {
            }
            t = null;
        }catch(Exception e){
            Log.d("@Subscriber#stopSubscriberThread#StopThreadException: ", String.valueOf(e));
        }
    }

    @Override
    public void subscriberCallback() {

    }

    public class SubscriberThread extends Thread {

        public void run() {

            Log.d("@Subscriber->startSubscriberThread", "Subscriber thread started...");
            try {
                Looper.prepare();
                Log.d("Subscriber:", Thread.currentThread().getName());

                // Handler to cancel the message loop
                loopHandler = new Handler();

                subscriber.addMessageListener(new MessageListener<JoyFeedback>() {

                    @Override
                    public void onNewMessage(JoyFeedback message) {
                        if(message.getType() == JoyFeedback.TYPE_LED){
                            float intensity = message.getIntensity();
                            Log.d("@Subscriber: ", String.valueOf(intensity));
                            subscriberCallback();
                        }
                    }
                });

                Looper.loop();
            } catch (Exception e) {
                Log.d("@Subscriber->PublisherThread->Run->LoopException", e.toString());
            }
        }
    }
}
