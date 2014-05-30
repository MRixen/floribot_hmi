package de.hs_heilbronn.floribot.android.floribot_hmi;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import java.net.URISyntaxException;

import de.hs_heilbronn.floribot.android.floribot_hmi.communication.NodeExecutorService;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;
import de.hs_heilbronn.floribot.android.floribot_hmi.gui.MySurfaceView;

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
    private EditText editTextMasterId, editTextTopicPublisher, editTextTopicSubscriber;
    private String masterId, topicPublisher, topicSubscriber;
    private WifiManager wifiManager;
    private ProgressDialog progressDialog;
    private Handler handler;
    private PowerManager pm;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    //private SurfaceViewMainActivity surfaceView;
    private SurfaceHolder holder;
    //private SurfaceViewMainActivity surfaceViewMainActivity;
    private SurfaceView surface;
    private SharedPreferences sharedPreferences;
    private TextView textViewMasterId, textViewTopicPublisher, textViewTopicSubscriber;
    private Button buttonConnect;
    private MySurfaceView mySurfaceView;
    private DataSet dataSet;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_main);

        // Get master id from input field
        editTextMasterId = (EditText) findViewById(R.id.editText_master_destination);
        editTextTopicPublisher = (EditText) findViewById(R.id.editText_topic_publisher);
        editTextTopicSubscriber = (EditText) findViewById(R.id.editText_topic_subscriber);
        textViewMasterId = (TextView) findViewById(R.id.textView_master_destination);
        textViewTopicPublisher = (TextView) findViewById(R.id.textView_topic_publisher);
        textViewTopicSubscriber = (TextView) findViewById(R.id.textView_topic_subscriber);
        buttonConnect = (Button) findViewById(R.id.connectButton);

        myService = new Intent(this, NodeExecutorService.class);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        dataSet = new DataSet(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getEditTextFieldEntries();
        savePreferences();

        mySurfaceView.pause();
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
        loadPreferences();
        // Set color for text
        DataSet.ThemeColor[] themeColors = DataSet.ThemeColor.values();
        editTextMasterId.setTextColor(Color.parseColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor));
        editTextTopicPublisher.setTextColor(Color.parseColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor));
        editTextTopicSubscriber.setTextColor(Color.parseColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor));
        textViewMasterId.setTextColor(Color.parseColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor));
        textViewTopicPublisher.setTextColor(Color.parseColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor));
        textViewTopicSubscriber.setTextColor(Color.parseColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor));
        buttonConnect.setTextColor(Color.parseColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor));

        setSurfaceView(R.id.surface_main);
        // Set surface for main activity
        Bundle surfaceDataBundle = dataSet.SurfaceDataMain();
        mySurfaceView.resume(surfaceDataBundle);
    }

    private void setSurfaceView(int layout_surface) {
        surface = (SurfaceView) findViewById(layout_surface);
        surface.setZOrderOnTop(false);
        holder = surface.getHolder();
        mySurfaceView = new MySurfaceView(this,holder);
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

                getEditTextFieldEntries();

                if (masterId.length() != 0 && topicPublisher.length() != 0 && topicSubscriber.length() != 0) {
                    handler = new Handler() {
                        public void handleMessage(Message msg) {
                            Bundle stateBundle = msg.getData();
                            String errorMessage = stateBundle.getString("errorMessage");
                            Boolean state = stateBundle.getBoolean("state");
                            if (state) {
                                progressDialog.cancel();
                                Bundle connectionData = new Bundle();
                                connectionData.putString("masterId", masterId);
                                connectionData.putString("topicPublisher", topicPublisher);
                                connectionData.putString("topicSubscriber", topicSubscriber);
                                myService.putExtra("connectionData", connectionData);
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
                } else {
                    if(masterId.length() == 0 )Toast.makeText(this, "Enter master address!", Toast.LENGTH_SHORT).show();
                    if(topicPublisher.length() == 0 )Toast.makeText(this, "Enter topic for publisher!", Toast.LENGTH_SHORT).show();
                    if(topicSubscriber.length() == 0 )Toast.makeText(this, "Enter topic for subscriber!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("masterId", masterId);
        editor.putString("topicPublisher", topicPublisher);
        editor.putString("topicSubscriber", topicSubscriber);
        //editor.putInt("theme", DataSet.currentTheme);
        editor.commit();
    }

    private void loadPreferences(){
        editTextMasterId.setText(sharedPreferences.getString("masterId", ""));
        editTextTopicPublisher.setText(sharedPreferences.getString("topicPublisher", ""));
        editTextTopicSubscriber.setText(sharedPreferences.getString("topicSubscriber", ""));
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
                if(!wifiManager.isWifiEnabled()) wifiManager.setWifiEnabled(true);

                long startTime = System.nanoTime();
                /*while (!wifiManager.isWifiEnabled()) {
                    timeElapsed = (System.nanoTime() - startTime) / DIVIDER;
                    if (timeElapsed >= 20) {
                        flag = false;
                        sendMessageToHandler("No wifi activation established", false);
                        break;
                    }
                }*/
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

               /* long startTime = System.nanoTime();
                while (!wifiLock.isHeld()) {
                    timeElapsed = (System.nanoTime() - startTime) / DIVIDER;
                    Log.d("@MainActivity#initForPublisher: ", "Elapsed time = " + timeElapsed);
                    if (timeElapsed >= 20) {
                        flag = false;
                        sendMessageToHandler("No wifi wake lock established", false);
                        break;
                    }
                }*/
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

    public void getEditTextFieldEntries() {
        masterId = editTextMasterId.getText().toString();
        topicPublisher = editTextTopicPublisher.getText().toString();
        topicSubscriber = editTextTopicSubscriber.getText().toString();
    }
}
