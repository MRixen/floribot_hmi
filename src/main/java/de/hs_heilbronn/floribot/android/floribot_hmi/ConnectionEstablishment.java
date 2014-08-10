package de.hs_heilbronn.floribot.android.floribot_hmi;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import de.hs_heilbronn.floribot.android.floribot_hmi.communication.NodeExecutorService;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;

import static android.os.Process.myPid;

public class ConnectionEstablishment extends BaseClass {

    private EditText editTextMasterId, editTextTopicPublisher,
            editTextTopicSubscriber, editTextNodeGraphName;
    private TextView textViewMasterId, textViewTopicPublisher,
            textViewTopicSubscriber, textViewNodeName,
            textViewMasterIdExampleText, textViewTopicPublisherExampleText,
            textViewTopicSubscriberExampleText, textViewNodeNameExampleText;
    private Button buttonConnect;
    private String masterId, topicPublisher, topicSubscriber, nodeGraphName;
    private WifiManager wifiManager;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private Handler connectionInitHandler;
    private ProgressDialog progressDialog;
    public static Intent nodeExecutorService;
    private SharedPreferences sharedPreferences;
    private BaseClass.ThemeColor[] themeColors;
    private ServiceResultReceiver serviceResultReceiver;
    private boolean serviceIsRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_connectionestablishment);

        // Get edit text and text view fields objects for masterID, etc.
        editTextMasterId = (EditText) findViewById(R.id.editText_master_destination);
        editTextTopicPublisher = (EditText) findViewById(R.id.editText_topic_publisher);
        editTextTopicSubscriber = (EditText) findViewById(R.id.editText_topic_subscriber);
        editTextNodeGraphName = (EditText) findViewById(R.id.editText_node_name);

        textViewMasterId = (TextView) findViewById(R.id.textView_master_destination);
        textViewTopicPublisher = (TextView) findViewById(R.id.textView_topic_publisher);
        textViewTopicSubscriber = (TextView) findViewById(R.id.textView_topic_subscriber);
        textViewNodeName = (TextView) findViewById(R.id.textView_node_name);

        textViewNodeNameExampleText = (TextView) findViewById(R.id.textView_node_name_example);
        textViewMasterIdExampleText = (TextView) findViewById(R.id.textView_master_destination_example);
        textViewTopicPublisherExampleText = (TextView) findViewById(R.id.textView_topic_publisher_example);
        textViewTopicSubscriberExampleText = (TextView) findViewById(R.id.textView_topic_subscriber_example);

        buttonConnect = (Button) findViewById(R.id.connectButton);

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Here you can call your method for new option in the settings activity
        if(serviceIsRunning) stopService(nodeExecutorService);
        loadPreferences();
        setTheme();
        serviceResultReceiver = new ServiceResultReceiver(null);
        releaseWakeLocks();

        if (checkTabletUsage()) {
            buttonConnect.setEnabled(false);
            final CustomDialog customDialog = new CustomDialog(this, R.style.dialog_style);
            Button negativeButton = customDialog.getNegativeButton();
            negativeButton.setVisibility(View.INVISIBLE);
            Button positiveButton = customDialog.getPositiveButton();
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch(v.getId()){
                        case(R.id.positive_button):
                            customDialog.dismiss();
                            onDestroy();
                            break;
                    }
                }
            });
            customDialog.showDialog(getResources().getString(R.string.dialog_message_on_tablet), false);
        }
    }

    private void setTheme() {
        // Change text color of connect button
        buttonConnect.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        // Change text color of text fields
        editTextMasterId.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        editTextTopicPublisher.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        editTextTopicSubscriber.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        editTextNodeGraphName.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);

        textViewMasterId.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        textViewTopicPublisher.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        textViewTopicSubscriber.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        textViewNodeName.setTextColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);

        textViewMasterId.setBackgroundColor(themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor);
        textViewTopicPublisher.setBackgroundColor(themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor);
        textViewTopicSubscriber.setBackgroundColor(themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor);
        textViewNodeName.setBackgroundColor(themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor);

        textViewMasterIdExampleText.setBackgroundColor(themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor);
        textViewTopicPublisherExampleText.setBackgroundColor(themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor);
        textViewTopicSubscriberExampleText.setBackgroundColor(themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor);
        textViewNodeNameExampleText.setBackgroundColor(themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor);

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
        LayerDrawable[] bgTextFields = new LayerDrawable[4];
        bgTextFields[0] = (LayerDrawable) editTextMasterId.getBackground();
        bgTextFields[1] = (LayerDrawable) editTextTopicPublisher.getBackground();
        bgTextFields[2] = (LayerDrawable) editTextTopicSubscriber.getBackground();
        bgTextFields[3] = (LayerDrawable) editTextNodeGraphName.getBackground();
        // Change background color of editText fields
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
    protected void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(myPid());
    }

    public void onButtonClicked(View v) {
        switch (v.getId()) {
            case (R.id.connectButton):
                boolean isTablet = checkTabletUsage();


                    boolean paramValid = parameterValidation();
                    if (paramValid) {
                        connectionInitHandler = new Handler() {
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
                                    Toast.makeText(ConnectionEstablishment.this, errorMessage, Toast.LENGTH_LONG).show();
                                    Log.d("@ConnectionEstablishment->handleMessage: ", "ErrorMessage: " + errorMessage);
                                }
                            }
                        };
                        progressDialog.show();
                        connectionInit();
                    }

                break;
        }
    }

    private boolean parameterValidation() {
        getEditTextFieldEntries();
        if (masterId.length() != 0 && topicPublisher.length() != 0 && topicSubscriber.length() != 0 && nodeGraphName.length() != 0) {
            if (!masterId.contains(" ") && !topicPublisher.contains(" ") && !topicSubscriber.contains(" ") && !nodeGraphName.contains(" ")) return true;
            else Toast.makeText(this, getResources().getString(R.string.toast_whitespace), Toast.LENGTH_LONG).show();
            }
        else{
            if (masterId.length() == 0)
                Toast.makeText(this, getResources().getString(R.string.toast_enter_master), Toast.LENGTH_SHORT).show();
            if (topicPublisher.length() == 0 || topicSubscriber.length() == 0)
                Toast.makeText(this, getResources().getString(R.string.toast_enter_topic), Toast.LENGTH_SHORT).show();
            if (nodeGraphName.length() == 0)
                Toast.makeText(this, getResources().getString(R.string.toast_enter_node_graph_name), Toast.LENGTH_SHORT).show();
        }

        return false;


    }

    private boolean checkTabletUsage() {
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        return screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    public void connectionInit() {

        Thread ConnectionInitThread = new Thread(){
            public URI uriTemp;

            @Override
            public void run(){
                boolean resultCode = true;
                long timeElapsed;

                String TAG = "wakeLockTag";

                // Check uri syntax
                try {
                    uriTemp = new URI(masterId);
                } catch (URISyntaxException e) {
                    resultCode = false;
                    sendMessageToHandler(getResources().getString(R.string.connection_establishment_error_uri_syntax), resultCode);
                }
                //----------------------------------------------

                // Enable display wake lock
                if(resultCode) {
                    Log.d("@ConnectionEstablishment->connectionInit: ", "Uri syntax ok.");

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
                    Log.d("@ConnectionEstablishment->connectionInit: ", "Display wake lock ok.");
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
                    Log.d("@ConnectionEstablishment->connectionInit: ", "Wifi on ok");

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

                // Try to ping to remote pc
                if(resultCode) {
                    Log.d("@ConnectionEstablishment->connectionInit: ", "Wifi wake lock ok");
                    Runtime runtime = Runtime.getRuntime();

                    try{
                        InetAddress inetAddress = InetAddress.getByName("MR-Ubuntu");
                        String masterIp = inetAddress.getHostAddress();

                        Process  executedProgram = runtime.exec("/system/bin/ping -c 1 " + masterIp);
                        int exitValue = executedProgram.waitFor();

                        if(exitValue == 0)resultCode = true;
                        else{
                            resultCode = false;
                            sendMessageToHandler(getResources().getString(R.string.connection_establishment_ping_error), false);
                        }
                    }
                    catch (InterruptedException ignore){
                        ignore.printStackTrace();
                        Log.d("connectionInit->InterruptedException", String.valueOf(ignore));
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        Log.d("connectionInit->IOException", String.valueOf(e));
                    }

                }

                //
                if(resultCode) {
                    Log.d("@ConnectionEstablishment->connectionInit: ", "Ping to master ok");
                    sendMessageToHandler("Connection establishment successful.", resultCode);
                }
            }

            private void sendMessageToHandler(String errorMessage, Boolean state){
                Bundle bundle = new Bundle();
                Message msg = new Message();
                bundle.putString(getResources().getString(R.string.error_message_node_init), errorMessage);
                bundle.putBoolean(getResources().getString(R.string.state_init_publisher), state);
                msg.setData(bundle);
                connectionInitHandler.sendMessage(msg);
            }
        };
        ConnectionInitThread.start();
    }

    public void getEditTextFieldEntries() {
        masterId = editTextMasterId.getText().toString();
        topicPublisher = editTextTopicPublisher.getText().toString();
        topicSubscriber = editTextTopicSubscriber.getText().toString();
        nodeGraphName = editTextNodeGraphName.getText().toString();
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getResources().getString(R.string.masterId), masterId);
        editor.putString(getResources().getString(R.string.topicPublisher), topicPublisher);
        editor.putString(getResources().getString(R.string.topicSubscriber), topicSubscriber);
        editor.putString(getResources().getString(R.string.nodeGraphName), nodeGraphName);
        editor.apply();
    }

    private void loadPreferences(){
        editTextMasterId.setText(sharedPreferences.getString(getResources().getString(R.string.masterId), ""));
        editTextTopicPublisher.setText(sharedPreferences.getString(getResources().getString(R.string.topicPublisher), ""));
        editTextTopicSubscriber.setText(sharedPreferences.getString(getResources().getString(R.string.topicSubscriber), ""));
        editTextNodeGraphName.setText(sharedPreferences.getString(getResources().getString(R.string.nodeGraphName), ""));
    }

    private void releaseWakeLocks() {
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

    class ServiceResultReceiver extends ResultReceiver
    {
        public ServiceResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            switch(resultCode){
                case(0):
                    Log.d("@ConnectionEstablishment->handleMessage: ", "Service started.");
                    serviceIsRunning = true;
                    Intent controlMenu = new Intent(ConnectionEstablishment.this, ControlMenu.class);
                    startActivity(controlMenu);
                    overridePendingTransition(R.anim.anim_in, R.anim.anim_out);
                    break;
                case(1):
                    Log.d("@ConnectionEstablishment->handleMessage: ", "Service stopped.");
                    Toast.makeText(ConnectionEstablishment.this,getResources().getString(R.string.serviceStopped),Toast.LENGTH_LONG).show();
                    releaseWakeLocks();
                    break;
            }
        }


    }


}
