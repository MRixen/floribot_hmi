package de.hs_heilbronn.floribot.android.floribot_hmi;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;

import de.hs_heilbronn.floribot.android.floribot_hmi.communication.MyCustomEvent;
import de.hs_heilbronn.floribot.android.floribot_hmi.communication.NodeExecutorService;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataAcquisition;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;
import de.hs_heilbronn.floribot.android.floribot_hmi.gui.GlobalLayout;
import de.hs_heilbronn.floribot.android.floribot_hmi.gui.LocalLayout;
import sensor_msgs.JoyFeedback;


public class ExecuteActivity extends BaseClass implements View.OnTouchListener, CompoundButton.OnCheckedChangeListener, LocalLayout.LocalLayoutManager, SeekBar.OnSeekBarChangeListener, DataSet.SubscriberInterface{

    private Button button_sensor_calibration;

    private GlobalLayout globalLayout;
    private LocalLayout localLayout;
    private RelativeLayout relativeLayout;

    public static NodeExecutorService nodeExecutorService;
    private int speed;
    private SeekBar seekBar_speed;

    private SurfaceView surface;

    private SharedPreferences sharedPreferences;
    private DataSet dataSet;
    private DataSet.ThemeColor[] themeColors;
    //private ControlDataAcquisition controlDataAcquisition;
    private Bundle surfaceData;
    private MyCustomEvent myCustomEvent;
    private DataAcquisition dataAcquisition;
    private ToggleButton led_sensor, led_manual, led_auto;
    private float manualIntensity, autoIntensity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_execute);

        button_sensor_calibration = (Button) findViewById(R.id.button_sensor_calibration);
        led_auto = (ToggleButton) findViewById(R.id.led_auto);
        led_auto.setOnCheckedChangeListener(this);
        led_sensor = (ToggleButton) findViewById(R.id.led_sensor);
        led_manual = (ToggleButton) findViewById(R.id.led_manual);
        led_manual.setOnCheckedChangeListener(this);

        DataSet.subscriberInterface = this;

        surface = (SurfaceView) findViewById(R.id.surface_execute);

        globalLayout = new GlobalLayout(this);
        localLayout = new LocalLayout(this);
        // Generate surface layout
        dataSet = getDataSet();
        surfaceData = dataSet.SurfaceDataExecute();

        myCustomEvent = new MyCustomEvent(this);
        dataAcquisition = new DataAcquisition(this, myCustomEvent);

    }

    @Override
    protected void onStart() {
        super.onStart();
        //controlDataAcquisition = new ControlDataAcquisition(getApplicationContext());
        nodeExecutorService = new NodeExecutorService();
        sharedPreferences = getSharedPreferences();
        themeColors = getThemeColors();
        // Start executor node
        //DataSet.node.startNodeThread();

        // Start data acquisition thread
        dataAcquisition.startThread();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set surface for execute activity
        if(surfaceData != null) globalLayout.setGlobalLayout(surfaceData, surface);
    }

    @Override
    protected void onPause() {
        super.onPause();
        globalLayout.pause();
    }

    @Override
    public void onBackPressed() {
        customDialog(getResources().getString(R.string.dialog_message_close_connection));
    }

    public void onButtonClicked(View v){
        switch (v.getId()) {
            case (R.id.button_manual):
                if(led_sensor.isChecked()){
                    led_sensor.setChecked(false);
                    localLayout.setLocalLayout(R.id.fragment_container, R.layout.layout_joystick_button, R.drawable.ic_joystick_active);
                }
                // Send data to data acquisition thread
                startEvent(DataSet.DriveMode.MANUAL_DRIVE.ordinal());
                break;
            case (R.id.button_auto):
                led_sensor.setChecked(false);
                button_sensor_calibration.setEnabled(false);
                // Send data to data acquisition thread
                startEvent(DataSet.DriveMode.AUTOMATIC_DRIVE.ordinal());
                sendDataToDataAcquisition(DataSet.DriveMode.AUTOMATIC_DRIVE.ordinal(), 0, 0, -1);
                break;
            case (R.id.button_sensor_calibration):
                // This mode is only available in manual mode
                // Set sensor mode is enabled led
                led_sensor.setChecked(true);
                localLayout.setLocalLayout(R.id.fragment_container, R.layout.layout_joystick_button, R.drawable.ic_sensor_active);
                customDialog(getResources().getString(R.string.dialog_message_start_calibration));
                break;
            case (R.id.button_exit):
                // Stop publisher and return to MainActivity
                customDialog(getResources().getString(R.string.dialog_message_close_connection));
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int driveCmd;

        switch (v.getId()) {
            case (R.id.button_sensor):
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    driveCmd = 1;
                    sendDataToDataAcquisition(DataSet.DriveMode.MANUAL_DRIVE.ordinal(), DataSet.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal(), driveCmd, -1);
                    setBackgroundForJoystickButtons(R.drawable.ic_sensor_pressed);

                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    driveCmd = 0;
                    sendDataToDataAcquisition(DataSet.DriveMode.MANUAL_DRIVE.ordinal(), DataSet.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal(), driveCmd, -1);
                    setBackgroundForJoystickButtons(R.drawable.ic_sensor_active);
                }
                break;
            case(R.id.button_up):
                if(!led_sensor.isChecked() && button_sensor_calibration.isEnabled()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        driveCmd = 1;
                        sendDataToDataAcquisition(DataSet.DriveMode.MANUAL_DRIVE.ordinal(), DataSet.DriveMode.MOVE_FORWARD_WITH_BUTTON.ordinal(), driveCmd, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_up);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        driveCmd = 0;
                        sendDataToDataAcquisition(DataSet.DriveMode.MANUAL_DRIVE.ordinal(), DataSet.DriveMode.MOVE_FORWARD_WITH_BUTTON.ordinal(), driveCmd, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);
                    }
                }
                break;
            case(R.id.button_down):
                if(!led_sensor.isChecked() && button_sensor_calibration.isEnabled()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        driveCmd = 1;
                        sendDataToDataAcquisition(DataSet.DriveMode.MANUAL_DRIVE.ordinal(), DataSet.DriveMode.MOVE_BACKWARD_WITH_BUTTON.ordinal(), driveCmd, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_down);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        driveCmd = 0;
                        sendDataToDataAcquisition(DataSet.DriveMode.MANUAL_DRIVE.ordinal(), DataSet.DriveMode.MOVE_BACKWARD_WITH_BUTTON.ordinal(), driveCmd, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);
                    }
                }
                break;
            case(R.id.button_left):
                if(!led_sensor.isChecked() && button_sensor_calibration.isEnabled()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        driveCmd = 1;
                        sendDataToDataAcquisition(DataSet.DriveMode.MANUAL_DRIVE.ordinal(), DataSet.DriveMode.TURN_LEFT_WITH_BUTTON.ordinal(), driveCmd, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_left);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        driveCmd = 0;
                        sendDataToDataAcquisition(DataSet.DriveMode.MANUAL_DRIVE.ordinal(), DataSet.DriveMode.TURN_LEFT_WITH_BUTTON.ordinal(), driveCmd, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);
                    }
                }
                break;
            case(R.id.button_right):
                if(!led_sensor.isChecked() && button_sensor_calibration.isEnabled()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        driveCmd = 1;
                        sendDataToDataAcquisition(DataSet.DriveMode.MANUAL_DRIVE.ordinal(), DataSet.DriveMode.TURN_RIGHT_WITH_BUTTON.ordinal(), driveCmd, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_right);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        driveCmd = 0;
                        sendDataToDataAcquisition(DataSet.DriveMode.MANUAL_DRIVE.ordinal(), DataSet.DriveMode.TURN_RIGHT_WITH_BUTTON.ordinal(), driveCmd, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);
                    }
                }
                break;
        }
        return false;
    }

    private void sendDataToDataAcquisition(int mode, int driveCmd, int cmdValue, int speed){
        int[] buttonData = new int[10];
        buttonData[driveCmd] = cmdValue;
        buttonData[mode] = 1;

        Bundle bundle = new Bundle();
        Message msg = new Message();

        bundle.putIntArray(getResources().getString(R.string.button_state_array), buttonData);
        if(speed != -1) bundle.putInt(getResources().getString(R.string.speed), speed);

        msg.setData(bundle);
        DataSet.handlerForControlDataAcquisition.sendMessage(msg);

    }
    
    private void setBackgroundForJoystickButtons(int drawableResource) {
        relativeLayout.setBackgroundDrawable(getResources().getDrawable(drawableResource));
    }

    @Override
    public void localLayoutCallback(int drawable) {
        // Get layout for joystick buttons
        relativeLayout = (RelativeLayout) findViewById(R.id.joystick_buttons);

        // Callback method to set button references when layout is valid
        if(led_sensor.isChecked()){
            seekBar_speed.setVisibility(View.INVISIBLE);
            Button sensor = (Button) findViewById(R.id.button_sensor);
            sensor.setOnTouchListener(this);
            setBackgroundForJoystickButtons(drawable);
        }
        if(led_manual.isChecked() && !led_sensor.isChecked()){
            Button button_up = (Button) findViewById(R.id.button_up);
            Button button_down = (Button) findViewById(R.id.button_down);
            Button button_left = (Button) findViewById(R.id.button_left);
            Button button_right = (Button) findViewById(R.id.button_right);
            seekBar_speed = (SeekBar) findViewById(R.id.seek_bar_speed);

            button_up.setOnTouchListener(this);
            button_down.setOnTouchListener(this);
            button_left.setOnTouchListener(this);
            button_right.setOnTouchListener(this);
            // Show seek bar (is set to invisible by pressing sensor button)
            seekBar_speed.setVisibility(View.VISIBLE);
            seekBar_speed.setOnSeekBarChangeListener(this);
            // Set background for joystick buttons
            setBackgroundForJoystickButtons(drawable);
            // Enable button for sensor drive mode
            button_sensor_calibration.setEnabled(true);
        }
    }


    public void customDialog(final String message) {
        final Dialog dialog = new Dialog(this, R.style.dialog_style);
        dialog.setContentView(R.layout.layout_dialog_publisher_exit);
        dialog.setCancelable(false);
        dialog.setTitle(getString(R.string.dialog_title_publisher_exit));

        Window dialogWindow = dialog.getWindow();
        dialogWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.ModernRed)));
        dialogWindow.setTitleColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);

        TextView textView = (TextView) dialog.findViewById(R.id.textView_exit_publisher);
        textView.setText(message);

        Button positiveButton = (Button) dialog.findViewById(R.id.positive_button);
        Button negativeButton = (Button) dialog.findViewById(R.id.negative_button);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set sensor toggle button state to false if user click cancel
                if (message.equals(getResources().getString(R.string.dialog_message_start_calibration))) {
                    led_sensor.setChecked(false);
                }
                dialog.dismiss();
            }
        });
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show dialog to disconnect publisher and do some stuff
                if (message.equals(getResources().getString(R.string.dialog_message_close_connection))) {
                    // Stop acquisition threads if they are still alive
                    dataAcquisition.stopThread();
                    dataAcquisition = null;
                    // Stop executor node
                    DataSet.node.stopNodeThread();

                    stopService(MainActivity.nodeExecutorService);
                    // Go back to MainActivity
                    dialog.dismiss();
                    finish();
                    overridePendingTransition(R.anim.anim_in, R.anim.anim_out);
                }
                // Show dialog to calibrate for start position (by manual drive with sensor) and do some stuff
                if (message.equals(getResources().getString(R.string.dialog_message_start_calibration))) {
                   //controlDataAcquisition.startControlDataAcquisitionThread(getResources().getString(R.string.control_mode_manual_sensor));
                    dialog.dismiss();
                    startEvent(DataSet.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal());
                    //setBackgroundForJoystickButtons(R.drawable.ic_sensor_active);
                    //seekBar_speed.setVisibility(View.INVISIBLE);
                }
            }
        });
        dialog.show();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        this.speed = progress;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release wifi lock and display wake lock
        PowerManager.WakeLock wakeLock = getWakeLock();
        if(wakeLock != null) {
            // Turn off power management
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
        WifiManager.WifiLock wifiLock = getWifiLock();
        if(wifiLock != null) {
            // Turn off wifi management
            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
        }
    }

    @Override
    public void subscriberCallback(List<JoyFeedback> messageList) {
        Log.d("@ExecuteActivity->subscriberCallback", "Change led state");

       int mSize = messageList.size();
        for(int i=0;i<mSize;i++){
            JoyFeedback object = messageList.get(i);
            switch(object.getId()){
                case(0):
                    // Set led for manual mode
                    if(object.getIntensity() > 0 && !(manualIntensity > 0)){
                        manualIntensity = object.getIntensity();
                        setFeedbackLed(led_manual,true);
                    }
                    else{
                        manualIntensity = object.getIntensity();
                        setFeedbackLed(led_manual, false);
                    }
                    break;
                case(1):
                    // Set led for auto mode
                    if(object.getIntensity() > 0 && !(autoIntensity > 0)){
                       autoIntensity = object.getIntensity();
                       setFeedbackLed(led_auto, true);
                    }
                    else{
                        autoIntensity = object.getIntensity();
                        setFeedbackLed(led_auto, false);
                    }
                    break;
            }
        }
    }

    private void setFeedbackLed(final ToggleButton ledType, final boolean led) {
        // Run in main thread - Set feedback led
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ledType.setChecked(led);
                Log.d("@setFeedbackLed->run", "is checked");
            }
        });
    }


    public void startEvent(int mode){
        MyCustomEvent.MyCustomEventListener myCustomEventListener = myCustomEvent.getMyCustomEventListener();
        if(myCustomEventListener != null) {
            myCustomEventListener.myCustomEvent(mode);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch(buttonView.getId()){
            case(R.id.led_manual):
                if(isChecked) {
                    // Show joystick buttons to control with and disable sensor led
                    localLayout.setLocalLayout(R.id.fragment_container, R.layout.layout_joystick_button, R.drawable.ic_joystick_active);
                }
                break;
            case(R.id.led_auto):
                if(isChecked) {
                    // Hide control buttons
                    if (localLayout != null)
                        localLayout.setLocalLayout(R.id.fragment_container, 0, 0);
                }
                break;
        }
    }
}
