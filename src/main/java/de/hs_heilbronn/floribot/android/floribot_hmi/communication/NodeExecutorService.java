package de.hs_heilbronn.floribot.android.floribot_hmi.communication;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import org.ros.address.InetAddressFactory;
import org.ros.exception.RosRuntimeException;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeListener;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;

import de.hs_heilbronn.floribot.android.floribot_hmi.ControlMenu;
import de.hs_heilbronn.floribot.android.floribot_hmi.R;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;

/**
 * Created by mr on 13.05.14.
 * This class (e.g. service) starts the publisher when a client bind to the service.
 * At start up (e.g. onStart()) some special properties were set like wake up for wLan and screen
 */
public class NodeExecutorService extends Service implements NodeMainExecutor {

    private static final int notificationId = 1;
    private final NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
    //private final IBinder binder = new LocalBinder();
    private ResultReceiver serviceResultReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get parameter for connection establishment
        Bundle connectionData= intent.getBundleExtra(getResources().getString(R.string.shared_pref_connection_data));
        String masterId = connectionData.getString(getResources().getString(R.string.masterId));
        String topicPublisher = connectionData.getString(getResources().getString(R.string.topicPublisher));
        String topicSubscriber = connectionData.getString(getResources().getString(R.string.topicSubscriber));
        String nodeGraphName = connectionData.getString(getResources().getString(R.string.nodeGraphName));
        serviceResultReceiver = connectionData.getParcelable(getResources().getString(R.string.serviceResultReceiver));

        URI uri = URI.create(masterId);

        // Bring service in foreground and provide access from notification bar
        Log.d("@NodeExecutorService#onCreate: ", "Start foreground");
        Notification notification = new Notification(R.drawable.ic_launcher, getString(R.string.ticker_message), System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, ControlMenu.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, getString(R.string.notification_title), getString(R.string.notification_text), pendingIntent);
        startForeground(notificationId, notification);

        // Start ExecutorNode (publisher, subscriber)
        try {
            Log.d("@NodeExecutorService#startExecutorNode: ", "Start node...");
            BaseClass.node = new Node(getApplicationContext(), topicSubscriber, topicPublisher, nodeGraphName);
            NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), uri);
            nodeConfiguration.setMasterUri(uri);
            nodeMainExecutor.execute(BaseClass.node, nodeConfiguration);
            sendResult(0);
        }catch(RosRuntimeException e){
            Log.d("@NodeExecutorService#startExecutorNode: ", "RosRuntimeException->Stop service");
            sendResult(1);
            stopSelf();
        }
        return START_STICKY;
    }

    public void sendResult(int resultCode){
        Bundle bundle = new Bundle();
        bundle.putInt(getResources().getString(R.string.resultCode), resultCode);
        serviceResultReceiver.send(resultCode, bundle);
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("@onUnbind: ", "Unbind from service...");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d("@onDestroy: ", "Destroy service...");
        shutdownNodeMain(BaseClass.node);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        /*Log.d("@onBind: ", "Bind to service...");
        return binder;*/
        return null;
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return null;
    }

    @Override
    public void execute(NodeMain nodeMain, NodeConfiguration nodeConfiguration, Collection<NodeListener> nodeListeners) {  }

    @Override
    public void execute(NodeMain nodeMain, NodeConfiguration nodeConfiguration) {
        execute(nodeMain, nodeConfiguration, null);
    }

    @Override
    public void shutdownNodeMain(NodeMain nodeMain) {
        Log.d("@shutdownNodeMain: ", "Shutdown publisher...");
        nodeMainExecutor.shutdownNodeMain(nodeMain);
        shutdown();
    }

    @Override
    public void shutdown() {

    }

/*    public class LocalBinder extends Binder {
        public NodeExecutorService getService() {
            return NodeExecutorService.this;
        }
    }*/
}
