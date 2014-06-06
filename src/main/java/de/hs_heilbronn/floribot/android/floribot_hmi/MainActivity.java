package de.hs_heilbronn.floribot.android.floribot_hmi;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import java.net.URISyntaxException;

import de.hs_heilbronn.floribot.android.floribot_hmi.communication.NodeExecutorService;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;

import static android.os.Process.myPid;

public class MainActivity extends BaseClass implements BaseClass.ThemeManager {
    private EditText editTextMasterId, editTextTopicPublisher, editTextTopicSubscriber;
    private TextView textViewMasterId, textViewTopicPublisher, textViewTopicSubscriber;
    private Button buttonConnect;
    private String masterId, topicPublisher, topicSubscriber;

    private WifiManager wifiManager;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    //private SharedPreferences sharedPreferences;

    //private DataSet dataSet;
    private Handler handler;
    private ProgressDialog progressDialog;

    public static Intent nodeExecutorService;

    private ServiceConnection mConnection = new ServiceConnection() {
        //Class for interacting with the main interface of the service.
        public void onServiceConnected(ComponentName className, IBinder service) {
            NodeExecutorService mService = ((NodeExecutorService.LocalBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
        }
    };
    private Bundle surfaceDataBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_main);

        editTextMasterId = (EditText) findViewById(R.id.editText_master_destination);
        editTextTopicPublisher = (EditText) findViewById(R.id.editText_topic_publisher);
        editTextTopicSubscriber = (EditText) findViewById(R.id.editText_topic_subscriber);

        textViewMasterId = (TextView) findViewById(R.id.textView_master_destination);
        textViewTopicPublisher = (TextView) findViewById(R.id.textView_topic_publisher);
        textViewTopicSubscriber = (TextView) findViewById(R.id.textView_topic_subscriber);

        buttonConnect = (Button) findViewById(R.id.connectButton);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.dialog_init_publisher));
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);

        //sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //dataSet = new DataSet(this);

        nodeExecutorService = new Intent(this, NodeExecutorService.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPreferences();
        // Set color for text

        editTextMasterId.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        editTextTopicPublisher.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        editTextTopicSubscriber.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);

        textViewMasterId.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        textViewTopicPublisher.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        textViewTopicSubscriber.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);

        buttonConnect.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);

        // Set surface for main activity
        setGlobalLayout(R.id.surface_main);
        surfaceDataBundle = dataSet.SurfaceDataMain();
        setSurfaceView(surfaceDataBundle);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getEditTextFieldEntries();
        savePreferences();
        globalLayout.pause();
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
    protected void onDestroy() {
        super.onDestroy();
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
                            String errorMessage = stateBundle.getString(getResources().getString(R.string.error_message_init_publisher));
                            Boolean state = stateBundle.getBoolean(getResources().getString(R.string.state_init_publisher));
                            if (state) {
                                progressDialog.cancel();
                                Bundle connectionData = new Bundle();
                                connectionData.putString(getResources().getString(R.string.shared_pref_master), masterId);
                                connectionData.putString(getResources().getString(R.string.shared_pref_topic_publisher), topicPublisher);
                                connectionData.putString(getResources().getString(R.string.shared_pref_topic_subscriber), topicSubscriber);
                                nodeExecutorService.putExtra(getResources().getString(R.string.shared_pref_connection_data), connectionData);
                                // Start service
                                startService(nodeExecutorService);
                                Log.d("@MainActivity#handleMessage: ", "Start service.");
                                Intent executeActivity = new Intent(MainActivity.this, ExecuteActivity.class);
                                startActivity(executeActivity);
                                overridePendingTransition(R.anim.anim_in, R.anim.anim_out);
                            } else {
                                progressDialog.cancel();
                                Log.d("@MainActivity#handleMessage: ", "ErrorMessage: " + errorMessage);
                            }
                        }
                    };
                    progressDialog.show();
                    initForPublisher();
                } else {
                    if(masterId.length() == 0 )Toast.makeText(this, getResources().getString(R.string.toast_enter_master), Toast.LENGTH_SHORT).show();
                    if(topicPublisher.length() == 0 )Toast.makeText(this, getResources().getString(R.string.toast_enter_topic_publisher), Toast.LENGTH_SHORT).show();
                    if(topicSubscriber.length() == 0 )Toast.makeText(this, getResources().getString(R.string.toast_enter_topic_subscriber), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void initForPublisher() {

        Thread t = new Thread(){
            @Override
            public void run(){
                boolean flag = true;
                long timeElapsed;

                String TAG = "wakeLockTag";

                // Check uri syntax
                try {
                    new URI(masterId);
                } catch (URISyntaxException e) {
                    Log.d("@NodeExecutorService#onStartCommand: ", "Invalid master ID!");
                    flag = false;
                    sendMessageToHandler("Invalid master ID!", false);
                }
                //----------------------------------------------

                if(flag) {
                    Log.d("@MainActivity#initForPublisher: ", "Uri syntax ok.");

                    // Control power management
                    powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                    wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
                    wakeLock.acquire();

                    long startTime = System.nanoTime();
                    while (!wakeLock.isHeld()) {
                        timeElapsed = (System.nanoTime() - startTime) / getResources().getInteger(R.integer.DIVIDER);
                        if (timeElapsed >= getResources().getInteger(R.integer.deadTimeWakeLockPower)) {
                            flag = false;
                            sendMessageToHandler("No display wake lock established!", false);
                            break;
                        }
                    }
                }
                //----------------------------------------------

                if(flag) {
                    Log.d("@MainActivity#initForPublisher: ", "Display wake lock ok.");

                    // Wifi activation
                    wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                    if(!wifiManager.isWifiEnabled()) wifiManager.setWifiEnabled(true);

                    long startTime = System.nanoTime();
                while (!wifiManager.isWifiEnabled()) {
                    timeElapsed = (System.nanoTime() - startTime) / getResources().getInteger(R.integer.DIVIDER);
                    if (timeElapsed >= getResources().getInteger(R.integer.deadTimeEnableWifi)) {
                        flag = false;
                        sendMessageToHandler("No wifi activation established!", false);
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
                    timeElapsed = (System.nanoTime() - startTime) / getResources().getInteger(R.integer.DIVIDER);
                    if (timeElapsed >= getResources().getInteger(R.integer.deadTimeWakeLockWifi)) {
                        flag = false;
                        sendMessageToHandler("No wifi wake lock established", false);
                        break;
                    }
                }
                }
                //----------------------------------------------

                if(flag) {
                    Log.d("@MainActivity#initForPublisher: ", "Wifi wake lock ok.");
                    sendMessageToHandler("Publisher initialization successful.", true);
                }
            }

            private void sendMessageToHandler(String errorMessage, Boolean state){
                Bundle bundle = new Bundle();
                Message msg = new Message();
                bundle.putString(getResources().getString(R.string.error_message_init_publisher), errorMessage);
                bundle.putBoolean(getResources().getString(R.string.state_init_publisher), state);
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

    private void savePreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getResources().getString(R.string.shared_pref_master), masterId);
        editor.putString(getResources().getString(R.string.shared_pref_topic_publisher), topicPublisher);
        editor.putString(getResources().getString(R.string.shared_pref_topic_subscriber), topicSubscriber);
        editor.putInt("theme", current_theme);
        editor.commit();
    }

    private void loadPreferences(){
        editTextMasterId.setText(sharedPreferences.getString(getResources().getString(R.string.shared_pref_master), ""));
        editTextTopicPublisher.setText(sharedPreferences.getString(getResources().getString(R.string.shared_pref_topic_publisher), ""));
        editTextTopicSubscriber.setText(sharedPreferences.getString(getResources().getString(R.string.shared_pref_topic_subscriber), ""));
    }

    @Override
    public void themeCallback(int current_theme) {
        this.current_theme = current_theme;
        savePreferences();
        globalLayout.pause();
        setSurfaceView(surfaceDataBundle);
    }
}
