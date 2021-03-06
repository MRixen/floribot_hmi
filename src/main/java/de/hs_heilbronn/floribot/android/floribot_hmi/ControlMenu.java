package de.hs_heilbronn.floribot.android.floribot_hmi;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;

import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataAcquisition;
import de.hs_heilbronn.floribot.android.floribot_hmi.gui.ControlPanel;
import de.hs_heilbronn.floribot.android.floribot_hmi.gui.SensorVisualisation;
import sensor_msgs.JoyFeedback;


public class ControlMenu extends BaseClass implements View.OnTouchListener, ControlPanel.OnControlPanelListener, SeekBar.OnSeekBarChangeListener, BaseClass.SubscriberMessageListener, BaseClass.SensorCalibrationListener, View.OnClickListener {

    private Button buttonSensor;
    private SensorVisualisation sensorVisualisation;
    private ControlPanel controlPanel;
    private RelativeLayout relativeLayout;
    private int speed;
    private SeekBar seekBar;
    private SurfaceView surface;
    private SharedPreferences sharedPreferences;
    private BaseClass.ThemeColor[] themeColors;
    private DataAcquisition dataAcquisition;
    private ToggleButton led_sensor, led_manual, led_auto;
    private Dialog dialog;
    private Button buttonExit;
    private ImageView bottomBarMiddle, bottomBarLeft;
    private CustomDialog customDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_controlmenu);

        buttonExit = (Button) findViewById(R.id.buttonExit);
        bottomBarMiddle = (ImageView) findViewById(R.id.bottom_bar_middle);
        bottomBarLeft = (ImageView) findViewById(R.id.bottom_bar_left);


        buttonSensor = (Button) findViewById(R.id.buttonSensor);

        led_auto = (ToggleButton) findViewById(R.id.led_auto);
        led_sensor = (ToggleButton) findViewById(R.id.led_sensor);
        led_manual = (ToggleButton) findViewById(R.id.led_manual);

        BaseClass.subscriberMessageListener = this;
        BaseClass.sensorCalibrationListener = this;

        surface = (SurfaceView) findViewById(R.id.surface_execute);

        sensorVisualisation = new SensorVisualisation(this);
        controlPanel = new ControlPanel(this);
        dialog = new Dialog(this, R.style.dialog_style);
        dataAcquisition = new DataAcquisition(this);

        setActionBarTitle(getResources().getString(R.string.title_activity_execute));

        customDialog = new CustomDialog(this, R.style.dialog_style);
        Button positiveButton = customDialog.getPositiveButton();
        positiveButton.setOnClickListener(this);
        Button negativeButton = customDialog.getNegativeButton();
        negativeButton.setOnClickListener(this);

    }


    @Override
    protected void onStart() {
        super.onStart();
        //nodeExecutorService = new NodeExecutorService();
        sharedPreferences = getSharedPreferences();
        themeColors = getThemeColors();
        // Start data acquisition thread
        dataAcquisition.startThread();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorVisualisation.startSensorVisualisation(surface);

        // Change button color to theme color
        // Therefor a new state list must be created
        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[] {android.R.attr.state_pressed}, getResources().getDrawable(R.drawable.button_background_pressed));
        stateListDrawable.addState(new int[] {-android.R.attr.state_pressed}, themeColors[sharedPreferences.getInt("theme", 0)].drawable[0]);
        buttonExit.setBackgroundDrawable(stateListDrawable);
        // Change button extension to theme color
        bottomBarMiddle.setBackgroundDrawable(themeColors[sharedPreferences.getInt("theme", 0)].drawable[1]);
        bottomBarLeft.setBackgroundDrawable(themeColors[sharedPreferences.getInt("theme", 0)].drawable[2]);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorVisualisation.stopSensorVisualisation();
    }

    @Override
    public void onBackPressed() {
        customDialog.showDialog(getResources().getString(R.string.dialog_message_close_connection), false);
    }

    public void onButtonClicked(View v){
        switch (v.getId()) {
            case (R.id.buttonManual):
                // Show joystick buttons to control with and disable sensor led
                led_manual.setChecked(true);
                led_auto.setChecked(false);
                led_sensor.setChecked(false);
                // Enable button for sensor drive mode
                buttonSensor.setEnabled(true);
                controlPanel.setControlPanel(R.id.fragment_container, R.layout.layout_controlpanel, R.drawable.ic_joystick_active);
                // Send data to publisher
                sendToDataAcquisition(BaseClass.DriveMode.MANUAL_DRIVE.ordinal(), 0, 0, 0, false);
                break;
            case (R.id.buttonAuto):
                speed = 0;
                led_sensor.setChecked(false);
                led_manual.setChecked(false);
                led_auto.setChecked(true);
                buttonSensor.setEnabled(false);
                controlPanel.setControlPanel(R.id.fragment_container, R.layout.layout_controlpanel, R.drawable.ic_joystick_auto_active);
                sendToDataAcquisition(BaseClass.DriveMode.AUTOMATIC_DRIVE.ordinal(), 0, 0, 0, false);
                break;
            case (R.id.buttonSensor):
                customDialog.showDialog(getResources().getString(R.string.dialog_message_start_calibration), true);
                break;
            case (R.id.buttonExit):
                // Stop publisher and return to ConnectionEstablishment
                customDialog.showDialog(getResources().getString(R.string.dialog_message_close_connection), false);
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int driveCmd;


        switch (v.getId()) {
            case (R.id.buttonDeadman):
                if (event.getAction() == MotionEvent.ACTION_DOWN && led_sensor.isChecked()) {
                    driveCmd = 1;
                    sendToDataAcquisition(-1, BaseClass.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal(), driveCmd, 0, false);
                    setBackgroundForJoystickButtons(R.drawable.ic_sensor_pressed);
                }
                if (event.getAction() == MotionEvent.ACTION_UP && led_sensor.isChecked()) {
                    driveCmd = 0;
                    sendToDataAcquisition(-1, BaseClass.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal(), driveCmd, 0, false);
                    setBackgroundForJoystickButtons(R.drawable.ic_sensor_active);
                }
                break;
            case(R.id.buttonUp):
                if(!led_sensor.isChecked() && buttonSensor.isEnabled()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        driveCmd = 1;
                        sendToDataAcquisition(-1, BaseClass.DriveMode.MOVE_FORWARD_WITH_BUTTON.ordinal(), driveCmd, speed, false);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_up);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        driveCmd = 0;
                        sendToDataAcquisition(-1, BaseClass.DriveMode.MOVE_FORWARD_WITH_BUTTON.ordinal(), driveCmd, 0, false);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);
                    }
                }
                break;
            case(R.id.buttonDown):
                if(!led_sensor.isChecked() && buttonSensor.isEnabled()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        driveCmd = 1;
                        sendToDataAcquisition(-1, BaseClass.DriveMode.MOVE_BACKWARD_WITH_BUTTON.ordinal(), driveCmd, -speed, false);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_down);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        driveCmd = 0;
                        sendToDataAcquisition(-1, BaseClass.DriveMode.MOVE_BACKWARD_WITH_BUTTON.ordinal(), driveCmd, 0, false);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);
                    }
                }
                break;
            case(R.id.buttonLeft):
                if(!led_sensor.isChecked() && buttonSensor.isEnabled() || led_auto.isChecked()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        driveCmd = 1;
                        sendToDataAcquisition(-1, BaseClass.DriveMode.TURN_LEFT_WITH_BUTTON.ordinal(), driveCmd, -speed, false);
                        if(led_auto.isChecked()) setBackgroundForJoystickButtons(R.drawable.ic_joystick_auto_left);
                        if(led_manual.isChecked()) setBackgroundForJoystickButtons(R.drawable.ic_joystick_left);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        driveCmd = 0;
                        sendToDataAcquisition(-1, BaseClass.DriveMode.TURN_LEFT_WITH_BUTTON.ordinal(), driveCmd, 0, false);
                        if(led_auto.isChecked()) setBackgroundForJoystickButtons(R.drawable.ic_joystick_auto_active);
                        if(led_manual.isChecked()) setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);
                    }
                }
                break;
            case(R.id.buttonRight):
                if(!led_sensor.isChecked() && buttonSensor.isEnabled() || led_auto.isChecked()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        driveCmd = 1;
                        sendToDataAcquisition(-1, BaseClass.DriveMode.TURN_RIGHT_WITH_BUTTON.ordinal(), driveCmd, speed, false);
                        if(led_auto.isChecked()) setBackgroundForJoystickButtons(R.drawable.ic_joystick_auto_right);
                        if(led_manual.isChecked()) setBackgroundForJoystickButtons(R.drawable.ic_joystick_right);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        driveCmd = 0;
                        sendToDataAcquisition(-1, BaseClass.DriveMode.TURN_RIGHT_WITH_BUTTON.ordinal(), driveCmd, 0, false);
                        if(led_auto.isChecked()) setBackgroundForJoystickButtons(R.drawable.ic_joystick_auto_active);
                        if(led_manual.isChecked()) setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);
                    }
                }
                break;
        }
        return false;
    }

    private void sendToDataAcquisition(int mode, int driveMode, int driveCmd, int speed, boolean calibration){
        int[] buttonData = new int[10];
        int[] axesData = new int[3];
        Bundle bundle = new Bundle();
        Message msg = new Message();

        buttonData[driveMode] = driveCmd;
        if(mode != -1) buttonData[mode] = 1;

        bundle.putIntArray(getResources().getString(R.string.button_state_array), buttonData);

        if(calibration){
            bundle.putBoolean(getResources().getString(R.string.start_sensor_calibration), calibration);
        }

        if(speed != 0){
            // Check if actual speed is different from last
            if (speed != axesData[0]) {
                for (int i = 0; i <= axesData.length - 1; i++) {
                    axesData[i] = speed;
                }
            }
            bundle.putInt(getResources().getString(R.string.speed), speed);
        }

        msg.setData(bundle);
        BaseClass.sendToDataAcquisition.sendMessage(msg);

    }
    
    private void setBackgroundForJoystickButtons(int drawableResource) {
        relativeLayout.setBackgroundDrawable(getResources().getDrawable(drawableResource));
    }

    @Override
    public void onControlPanelLoaded(int drawable) {
        // Get layout for joystick buttons
        relativeLayout = (RelativeLayout) findViewById(R.id.joystick_buttons);
        seekBar = (SeekBar) findViewById(R.id.seek_bar_speed);

        // Callback method to set button references when layout is valid
        if(led_sensor.isChecked()){
            seekBar.setVisibility(View.INVISIBLE);
            Button button_deadman = (Button) findViewById(R.id.buttonDeadman);
            button_deadman.setOnTouchListener(this);
            setBackgroundForJoystickButtons(drawable);
        }
        if(led_manual.isChecked() && !led_sensor.isChecked()){
            Button button_up = (Button) findViewById(R.id.buttonUp);
            Button button_down = (Button) findViewById(R.id.buttonDown);
            Button button_left = (Button) findViewById(R.id.buttonLeft);
            Button button_right = (Button) findViewById(R.id.buttonRight);

            button_up.setOnTouchListener(this);
            button_down.setOnTouchListener(this);
            button_left.setOnTouchListener(this);
            button_right.setOnTouchListener(this);

            seekBar.setVisibility(View.VISIBLE);
            seekBar.setOnSeekBarChangeListener(this);
            seekBar.setProgress(0);
            // Set background for joystick buttons
            setBackgroundForJoystickButtons(drawable);
        }
        if(led_auto.isChecked()){
            Button button_left = (Button) findViewById(R.id.buttonLeft);
            Button button_right = (Button) findViewById(R.id.buttonRight);
            button_left.setOnTouchListener(this);
            button_right.setOnTouchListener(this);
            seekBar.setVisibility(View.INVISIBLE);
            setBackgroundForJoystickButtons(drawable);
        }
    }

    @Override
    public void onCalibrationSuccess() {
        dialog.dismiss();
    }

    private void onExit(){
        dataAcquisition.stopThread();
        dataAcquisition = null;
        // Stop executor node
        BaseClass.node.stopNodeThread();
        //stopService(ConnectionEstablishment.nodeExecutorService);
        // Go back to ConnectionEstablishment
        dialog.dismiss();
        finish();
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
    }

    @Override
    public void onNewMessage(List<JoyFeedback> messageList) {
            /*switch(object.getId()){
                case(0):
                    // Set led for manual mode
                    if(object.getIntensity() > 0 && !(manualIntensity > 0)){
                        manualIntensity = object.getIntensity();
                        setFeedbackLed(led_manual,true);
                        Log.d("@onNewMessage", "led feedback");
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
        }*/
    }

    private void setFeedbackLed(final ToggleButton ledType, final boolean led) {
        // Run in main thread - Set feedback led
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ledType.setChecked(led);

            }
        });
    }

    @Override
    public void onClick(View v) {
        String message = customDialog.getText();
        switch(v.getId()){
            case(R.id.positive_button):
                if (message.equals(getResources().getString(R.string.dialog_message_start_calibration))) {
                    sendToDataAcquisition(DriveMode.MANUAL_DRIVE.ordinal(), BaseClass.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal(), 1, 0, true);
                    led_sensor.setChecked(true);
                    controlPanel.setControlPanel(R.id.fragment_container, R.layout.layout_controlpanel, R.drawable.ic_sensor_active);
                }
                if(message.equals(getResources().getString(R.string.dialog_message_close_connection))){
                    onExit();
                    overridePendingTransition(R.anim.anim_in, R.anim.anim_out);
                }
                else customDialog.dismiss();
                break;
            case(R.id.negative_button):
                if (message.equals(getResources().getString(R.string.dialog_message_start_calibration))) {
                    led_sensor.setChecked(false);
                    controlPanel.setControlPanel(R.id.fragment_container, R.layout.layout_controlpanel, R.drawable.ic_joystick_active);
                }
                customDialog.dismiss();
                break;
        }
    }
}
