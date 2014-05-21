package de.hs_heilbronn.floribot.android.floribot_hmi;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.net.URI;
import java.net.URISyntaxException;

import de.hs_heilbronn.floribot.android.floribot_hmi.communication.NodeExecutorService;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;
import de.hs_heilbronn.floribot.android.floribot_hmi.gui.SurfaceViewMainActivity;

import static android.os.Process.myPid;

public class MainActivity extends BaseClass {

    public static boolean threadInterruption;
    public static Intent myService;
    private NodeExecutorService myServiceClass;

    //Class for interacting with the main interface of the service.
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            myServiceClass = ((NodeExecutorService.LocalBinder) service).getService();
            Log.d("@onServiceConnected: ", "....");
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
        }
    };
    private EditText textViewMasterAddress;
    private String masterId;
    private WifiManager wifiManager;
    private ProgressDialog progressDialog;
    private Handler handler;
    private PowerManager pm;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private SurfaceViewMainActivity surfaceView;
    private SurfaceHolder holder;
    private SurfaceViewMainActivity surfaceViewMainActivity;
    private SurfaceView surface;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //---DEBUG
        surface = (SurfaceView) findViewById(R.id.surface_main);
        surface.setZOrderOnTop(false);
        holder = surface.getHolder();
        surfaceViewMainActivity = new SurfaceViewMainActivity(this,holder);
        surfaceViewMainActivity.resume();
        //-----

        //--------

        // Get master id from input field
        textViewMasterAddress = (EditText) findViewById(R.id.editText_master_destination);
        myService = new Intent(this, NodeExecutorService.class);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);

    }

    @Override
    protected void onPause() {
        super.onPause();
        surfaceViewMainActivity.pause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(wakeLock != null) {
            // Turn off power management
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
        if(wifiLock != null) {
            // Turn off wifi management
            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        surfaceViewMainActivity.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("onDestroy: ", "ok...");
        android.os.Process.killProcess(myPid());
    }

    public void onButtonClicked(View v) {
        switch (v.getId()) {
            case (R.id.connectButton):

                masterId = textViewMasterAddress.getText().toString();
                //masterId = "http://192.168.137.191:11311";

                if (masterId.length() != 0) {
                    handler = new Handler() {
                        public void handleMessage(Message msg) {
                            Bundle bundle = msg.getData();
                            String errorMessage = bundle.getString("errorMessage");
                            Boolean state = bundle.getBoolean("state");
                            if (state) {
                                progressDialog.cancel();
                                myService.putExtra("masterAddress", masterId);
                                // Start service
                                startService(myService);
                                Log.d("@MainActivity#HandleMessage: ", "Start service...");
                                Intent executeActivity = new Intent(MainActivity.this, ExecuteActivity.class);
                                startActivity(executeActivity);
                                //overridePendingTransition(R.anim.anim_in, R.anim.anim_out);
                            } else {
                                progressDialog.cancel();
                                Log.d("@MainActivity#ErrorMessage: ", errorMessage);
                            }
                        }
                    };
                    progressDialog.show();
                    initForPublisher();
                } else Toast.makeText(this, "Enter master address!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void initForPublisher() {

        Thread t = new Thread(){
        @Override
        public void run(){
            boolean flag = true;
            long timeElapsed;
            final int DIVIDER = 1000000000;
            String TAG = "wakeLockTag";

                // Check uri syntax
                try {
                    new URI(masterId);
                } catch (URISyntaxException e) {
                    Log.d("@NodeExecutorService#onStartCommand: ", "Invalid master ID");
                    flag = false;
                    sendMessageToHandler("Invalid master ID", false);
                }
                //----------------------------------------------

            if(flag) {
                Log.d("@MainActivity#initForPublisher: ", "Uri syntax ok");

                // Control power management
                pm = (PowerManager) getSystemService(POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
                wakeLock.acquire();

                long startTime = System.nanoTime();
                while (!wakeLock.isHeld()) {
                    timeElapsed = (System.nanoTime() - startTime) / DIVIDER;
                    if (timeElapsed >= 10) {
                        flag = false;
                        sendMessageToHandler("No display wake lock established", false);
                        break;
                    }
                }
            }
                //----------------------------------------------

            if(flag) {
                Log.d("@MainActivity#initForPublisher: ", "Display wake lock ok");

                // Wifi activation
                wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                wifiManager.setWifiEnabled(true);

                long startTime = System.nanoTime();
                while (!wifiManager.isWifiEnabled()) {
                    timeElapsed = (System.nanoTime() - startTime) / DIVIDER;
                    if (timeElapsed >= 20) {
                        flag = false;
                        sendMessageToHandler("No wifi activation established", false);
                        break;
                    }
                }
            }
                //----------------------------------------------

            if(flag) {
                Log.d("@MainActivity#initForPublisher: ", "Wifi on ok");

                // Wifi wake lock
                int wifiLockType = WifiManager.WIFI_MODE_FULL;

                // Check if WIFI_MODE_FULL_HIGH_PERF is supported (supported since API Level 12)
                try {
                    wifiLockType = WifiManager.class.getField("WIFI_MODE_FULL_HIGH_PERF").getInt(null);
                } catch (Exception e) {
                    Log.w(TAG, "Unable to acquire high performance wifi lock.");
                }

                wifiLock = wifiManager.createWifiLock(wifiLockType, TAG);
                wifiLock.acquire();

                long startTime = System.nanoTime();
                while (!wifiLock.isHeld()) {
                    timeElapsed = (System.nanoTime() - startTime) / DIVIDER;
                    Log.d("@MainActivity#initForPublisher: ", "Elapsed time = " + timeElapsed);
                    if (timeElapsed >= 20) {
                        flag = false;
                        sendMessageToHandler("No wifi wake lock established", false);
                        break;
                    }
                }
            }
                //----------------------------------------------

            if(flag) {
                Log.d("@MainActivity#initForPublisher: ", "Wifi wake lock ok");
                sendMessageToHandler("Everything is ok", true);
            }
            }

            private void sendMessageToHandler(String errorMessage, Boolean state){
                Bundle bundle = new Bundle();
                Message msg = new Message();
                bundle.putString("errorMessage", errorMessage);
                bundle.putBoolean("state", state);
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        };
        t.start();
    }

}
