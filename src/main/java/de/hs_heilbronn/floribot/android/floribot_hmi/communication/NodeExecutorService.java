package de.hs_heilbronn.floribot.android.floribot_hmi.communication;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.ros.address.InetAddressFactory;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeListener;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;

import de.hs_heilbronn.floribot.android.floribot_hmi.ExecuteActivity;
import de.hs_heilbronn.floribot.android.floribot_hmi.R;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;

/**
 * Created by mr on 13.05.14.
 * This class (e.g. service) starts the publisher when a client bind to the service.
 * At start up (e.g. onStart()) some special properties were set like wake up for wLan and screen
 */
public class NodeExecutorService extends Service implements NodeMainExecutor {


    private static final int ONGOING_NOTIFICATION_ID = 1;
    private final NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
    private final IBinder binder = new LocalBinder();


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Get parameter for connection establishment
        Bundle connectionData= intent.getBundleExtra("connectionData");
        String masterId = connectionData.getString("masterId");
        String topicPublisher = connectionData.getString("topicPublisher");
        String topicSubscriber = connectionData.getString("topicSubscriber");

        URI uri = URI.create(masterId);

        Log.d("@NodeExecutorService#onCreate: ", "Start foreground");
        Notification notification = new Notification(R.drawable.ic_launcher, getString(R.string.ticker_message), System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, ExecuteActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, getString(R.string.notification_title), getString(R.string.notification_text), pendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, notification);

        Log.d("@NodeExecutorService#startPublisher: ", "Start publisher");
        DataSet.publisher = new Publisher(getApplicationContext(), topicPublisher);
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), uri);
        nodeConfiguration.setMasterUri(uri);
        nodeMainExecutor.execute(DataSet.publisher, nodeConfiguration);

        Log.d("@NodeExecutorService#startPublisher: ", "Start subscriber");
        DataSet.subscriberString = new SubscriberString(getApplicationContext(), topicSubscriber);
        nodeConfiguration = NodeConfiguration.newPublic(String.valueOf(InetAddressFactory.newNonLoopback().getHostAddress()), uri);
        nodeConfiguration.setMasterUri(uri);
        nodeMainExecutor.execute(DataSet.subscriberString, nodeConfiguration);
        //----------------------------------------------

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("@onBind: ", "Bind to service...");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("@onUnbind: ", "Unbind from service...");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d("@onDestroy: ", "Destroy service...");
        shutdownNodeMain(DataSet.publisher);
        shutdownNodeMain(DataSet.subscriberString);
        super.onDestroy();
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return null;
    }

    @Override
    public void execute(NodeMain nodeMain, NodeConfiguration nodeConfiguration, Collection<NodeListener> nodeListeners) {

    }

    @Override
    public void execute(NodeMain nodeMain, NodeConfiguration nodeConfiguration) {
        execute(nodeMain, nodeConfiguration, null);
    }

    @Override
    public void shutdownNodeMain(NodeMain nodeMain) {
        Log.d("@shutdownNodeMain: ", "Shutdown publisher...");
        nodeMainExecutor.shutdownNodeMain(nodeMain);
    }

    @Override
    public void shutdown() {

    }

    public class LocalBinder extends Binder {
        public NodeExecutorService getService() {
            return NodeExecutorService.this;
        }
    }

}
