package de.hs_heilbronn.floribot.android.floribot_hmi;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.provider.Settings;
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

public class MainActivity extends BaseClass {


    private EditText editTextMasterId, editTextTopicPublisher, editTextTopicSubscriber;
    private TextView textViewMasterId, textViewTopicPublisher, textViewTopicSubscriber;
    private Button buttonConnect;
    private String masterId, topicPublisher, topicSubscriber, nodeGraphName;

    private WifiManager wifiManager;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;


    private Handler handler;
    private ProgressDialog progressDialog;

    public static Intent nodeExecutorService;
    private SharedPreferences sharedPreferences;

    private BaseClass.ThemeColor[] themeColors;
    private int currentTheme;

    private ServiceResultReceiver serviceResultReceiver;

    private ServiceConnection mConnection = new ServiceConnection() {
        //Class for interacting with the main interface of the service.
        public void onServiceConnected(ComponentName className, IBinder service) {
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_main);

        // Get edit text and text view fields objects for masterID, etc.
        editTextMasterId = (EditText) findViewById(R.id.editText_master_destination);
        editTextTopicPublisher = (EditText) findViewById(R.id.editText_topic_publisher);
        editTextTopicSubscriber = (EditText) findViewById(R.id.editText_topic_subscriber);
        textViewMasterId = (TextView) findViewById(R.id.textView_master_destination);

        textViewTopicPublisher = (TextView) findViewById(R.id.textView_topic_publisher);
        textViewTopicSubscriber = (TextView) findViewById(R.id.textView_topic_subscriber);

        buttonConnect = (Button) findViewById(R.id.connect_button);

        // Get objects for progressbar (adjustment for amount of acceleration)
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.dialog_init_publisher));
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        sharedPreferences = getSharedPreferences();

        setActionBarTitle(getResources().getString(R.string.title_activity_main));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Get object of service
        nodeExecutorService = new Intent(this, NodeExecutorService.class);
        // Get object for theme colors to text color, etc.
        themeColors = getThemeColors();
        currentTheme = sharedPreferences.getInt("theme", 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadPreferences();
        setTheme();
       // initActionBar();



        serviceResultReceiver = new ServiceResultReceiver(null);

        // Turn off power management
        if(wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
        // Turn off wifi management
        if(wifiLock != null) {
            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
        }

    }

    private void setTheme() {

        // Change text color of connect button
        buttonConnect.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);

        // Change text color of text fields
        editTextMasterId.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        editTextTopicPublisher.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        editTextTopicSubscriber.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);

        textViewMasterId.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        textViewTopicPublisher.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        textViewTopicSubscriber.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);

        textViewMasterId.setBackgroundColor(themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor);
        textViewTopicPublisher.setBackgroundColor(themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor);
        textViewTopicSubscriber.setBackgroundColor(themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor);

        // Change background color of text fields
        // Get shape id's from layer list of editText fields
        int[] shapes = new int[3];
        shapes[0] = R.id.shape_one;
        shapes[1] = R.id.shape_two;
        shapes[2] = R.id.shape_three;
        // Get stroke width for layer list components
        int[] strokes = new int[3];
        strokes[0] = getResources().getDimensionPixelSize(R.dimen.strokeWidthOne);
        strokes[1] = getResources().getDimensionPixelSize(R.dimen.strokeWidthTwo);
        strokes[2] = getResources().getDimensionPixelSize(R.dimen.strokeWidthThree);
        // Get background (layer list) of editText fields
        LayerDrawable[] bgTextFields = new LayerDrawable[3];
        bgTextFields[0] = (LayerDrawable) editTextMasterId.getBackground();
        bgTextFields[1] = (LayerDrawable) editTextTopicPublisher.getBackground();
        bgTextFields[2] = (LayerDrawable) editTextTopicSubscriber.getBackground();
        // Change shapes background color of the layer lists from editText fields
        for(int i=0;i<bgTextFields.length;i++){
            for(int j=0;j<shapes.length;j++) {
                GradientDrawable layerTextFields = (GradientDrawable) bgTextFields[i].findDrawableByLayerId(shapes[j]);
                layerTextFields.setStroke(strokes[j], themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor);
                if(j != 1) layerTextFields.setColor(themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor);
            }
        }
        // Change button color to theme color
        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[] {android.R.attr.state_pressed}, getResources().getDrawable(R.drawable.button_background_pressed));
        stateListDrawable.addState(new int[] {-android.R.attr.state_pressed}, themeColors[sharedPreferences.getInt("theme", 0)].drawable[0]);
        buttonConnect.setBackgroundDrawable(stateListDrawable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getEditTextFieldEntries();
        savePreferences();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(myPid());
    }

    public void onButtonClicked(View v) {
        switch (v.getId()) {
            case (R.id.connect_button):

                getEditTextFieldEntries();

                if (masterId.length() != 0 && topicPublisher.length() != 0 && topicSubscriber.length() != 0 && nodeGraphName.length() != 0) {
                    handler = new Handler() {
                        public void handleMessage(Message msg) {
                            Bundle stateBundle = msg.getData();
                            String errorMessage = stateBundle.getString(getResources().getString(R.string.error_message_node_init));
                            Boolean state = stateBundle.getBoolean(getResources().getString(R.string.state_init_publisher));
                            if (state) {
                                progressDialog.cancel();
                                Bundle connectionData = new Bundle();
                                connectionData.putString(getResources().getString(R.string.masterId), masterId);
                                connectionData.putString(getResources().getString(R.string.topicPublisher), topicPublisher);
                                connectionData.putString(getResources().getString(R.string.topicSubscriber), topicSubscriber);

                                connectionData.putString(getResources().getString(R.string.nodeGraphName), sharedPreferences.getString(getResources().getString(R.string.nodeGraphName), ""));
                                connectionData.putParcelable(getResources().getString(R.string.serviceResultReceiver), serviceResultReceiver);
                                nodeExecutorService.putExtra(getResources().getString(R.string.shared_pref_connection_data), connectionData);
                                // Start service
                                startService(nodeExecutorService);
                            } else {
                                progressDialog.cancel();
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                Log.d("@MainActivity->handleMessage: ", "ErrorMessage: " + errorMessage);
                            }
                        }
                    };
                    progressDialog.show();
                    nodeInitialization();
                } else {
                    if(masterId.length() == 0 && topicSubscriber.length() == 0 & topicPublisher.length() == 0) Toast.makeText(this, getResources().getString(R.string.toast_enter_all), Toast.LENGTH_SHORT).show();
                    else {
                        if (masterId.length() == 0)
                            Toast.makeText(this, getResources().getString(R.string.toast_enter_master), Toast.LENGTH_SHORT).show();
                        if (topicPublisher.length() == 0 || topicSubscriber.length() == 0)
                            Toast.makeText(this, getResources().getString(R.string.toast_enter_topic), Toast.LENGTH_SHORT).show();
                        if(nodeGraphName.length() == 0)
                            Toast.makeText(this, getResources().getString(R.string.toast_enter_node_graph_name), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    public void nodeInitialization() {

        Thread t = new Thread(){
            @Override
            public void run(){
                boolean resultCode = true;
                long timeElapsed;

                String TAG = "wakeLockTag";

                // Check uri syntax
                try {
                    new URI(masterId);
                } catch (URISyntaxException e) {
                    resultCode = false;
                    sendMessageToHandler(getResources().getString(R.string.connection_establishment_error_uri_syntax), resultCode);
                }
                //----------------------------------------------

                // Enable display wake lock
                if(resultCode) {
                    Log.d("@MainActivity->nodeInitialization: ", "Uri syntax ok.");

                    // Control power management
                    powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                    wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
                    wakeLock.acquire();


                    long startTime = System.nanoTime();
                    while (!wakeLock.isHeld()) {
                        timeElapsed = (System.nanoTime() - startTime) / getResources().getInteger(R.integer.DIVIDER);
                        if (timeElapsed >= getResources().getInteger(R.integer.deadTimeWakeLockPower)) {
                            resultCode = false;
                            sendMessageToHandler(getResources().getString(R.string.connection_establishment_error_display_wake_lock), resultCode);
                            break;
                        }
                    }
                }

                // Check network state

                //----------------------------------------------

                // Enable wifi
                if(resultCode) {
                    Log.d("@MainActivity->nodeInitialization: ", "Display wake lock ok.");
                    // Check if airplane mode is on (need to be on to prevent call events)
                    if(Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0) {
                        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                        if (!wifiManager.isWifiEnabled()) wifiManager.setWifiEnabled(true);

                        long startTime = System.nanoTime();
                        while (!wifiManager.isWifiEnabled()) {
                            timeElapsed = (System.nanoTime() - startTime) / getResources().getInteger(R.integer.DIVIDER);
                            if (timeElapsed >= getResources().getInteger(R.integer.deadTimeEnableWifi)) {
                                resultCode = false;
                                sendMessageToHandler(getResources().getString(R.string.connection_establishment_error_wifi_activation), resultCode);
                                break;
                            }
                        }
                    }
                    else{
                        resultCode = false;
                        sendMessageToHandler(getResources().getString(R.string.connection_establishment_error_airplane), resultCode);
                    }
                }
                //----------------------------------------------

                // Enable wifi lock
                if(resultCode) {
                    Log.d("@MainActivity->nodeInitialization: ", "Wifi on ok");

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
                        resultCode = false;
                        sendMessageToHandler(getResources().getString(R.string.connection_establishment_error_wifi_lock), resultCode);
                        break;
                    }
                }
                }
                //----------------------------------------------

                //
                if(resultCode) {
                    Log.d("@MainActivity->nodeInitialization: ", "Wifi wake lock ok.");
                    sendMessageToHandler("Connection establishment successful.", resultCode);
                }
            }

            private void sendMessageToHandler(String errorMessage, Boolean state){
                Bundle bundle = new Bundle();
                Message msg = new Message();
                bundle.putString(getResources().getString(R.string.error_message_node_init), errorMessage);
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
        nodeGraphName = sharedPreferences.getString(getResources().getString(R.string.nodeGraphName), "");

    }

    private void savePreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getResources().getString(R.string.masterId), masterId);
        editor.putString(getResources().getString(R.string.topicPublisher), topicPublisher);
        editor.putString(getResources().getString(R.string.topicSubscriber), topicSubscriber);
        editor.commit();
    }

    private void loadPreferences(){
        editTextMasterId.setText(sharedPreferences.getString(getResources().getString(R.string.masterId), ""));
        editTextTopicPublisher.setText(sharedPreferences.getString(getResources().getString(R.string.topicPublisher), ""));
        editTextTopicSubscriber.setText(sharedPreferences.getString(getResources().getString(R.string.topicSubscriber), ""));
    }

    class ServiceResultReceiver extends ResultReceiver
    {
        public ServiceResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            switch(resultCode){
                case(0):
                    Log.d("@MainActivity->handleMessage: ", "Start service.");
                    Intent executeActivity = new Intent(MainActivity.this, ExecuteActivity.class);
                    startActivity(executeActivity);
                    overridePendingTransition(R.anim.anim_in, R.anim.anim_out);
                    break;
                case(1):
                    Log.d("@MainActivity->handleMessage: ", "Service stopped.");
                    Toast.makeText(MainActivity.this,getResources().getString(R.string.serviceStopped),Toast.LENGTH_SHORT).show();
                    // Turn off power management
                    if (wakeLock.isHeld()) {
                        wakeLock.release();
                    }
                    // Turn off wifi management
                    if (wifiLock.isHeld()) {
                        wifiLock.release();
                    }
                    break;
            }
        }
    }
}
