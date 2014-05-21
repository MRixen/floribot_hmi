package de.hs_heilbronn.floribot.android.floribot_hmi.communication;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
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

        String masterId = intent.getStringExtra("masterAddress");
        URI uri = URI.create(masterId);

        Log.d("@NodeExecutorService#onCreate: ", "Start foreground");
        Notification notification = new Notification(R.drawable.ic_launcher, getString(R.string.ticker_message), System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, ExecuteActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, getString(R.string.notification_title), getString(R.string.notification_text), pendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, notification);

        Log.d("@NodeExecutorService#startPublisher: ", "Start publisher");
        DataSet.talker = new Talker2(getApplicationContext());
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), uri);
        nodeConfiguration.setMasterUri(uri);

        nodeMainExecutor.execute(DataSet.talker, nodeConfiguration);
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
        shutdownNodeMain(DataSet.talker);
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
