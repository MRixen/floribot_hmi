package de.hs_heilbronn.floribot.android.floribot_hmi;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
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

import de.hs_heilbronn.floribot.android.floribot_hmi.communication.ControlDataAcquisition;
import de.hs_heilbronn.floribot.android.floribot_hmi.communication.NodeExecutorService;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;
import de.hs_heilbronn.floribot.android.floribot_hmi.gui.GlobalLayout;
import de.hs_heilbronn.floribot.android.floribot_hmi.gui.LocalLayout;
import sensor_msgs.JoyFeedback;


public class ExecuteActivity extends BaseClass implements View.OnTouchListener, LocalLayout.LocalLayoutManager, SeekBar.OnSeekBarChangeListener, DataSet.SubscriberInterface{

    private ToggleButton button_manual, button_auto, button_sensor_calibration;

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
    private ControlDataAcquisition controlDataAcquisition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_execute);

        button_manual = (ToggleButton) findViewById(R.id.button_manual);
        button_auto = (ToggleButton) findViewById(R.id.button_auto);
        button_sensor_calibration = (ToggleButton) findViewById(R.id.button_sensor_calibration);

        DataSet.subscriberInterface = this;

        surface = (SurfaceView) findViewById(R.id.surface_execute);

        globalLayout = new GlobalLayout(this);
        localLayout = new LocalLayout(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        controlDataAcquisition = new ControlDataAcquisition(getApplicationContext());
        nodeExecutorService = new NodeExecutorService();
        sharedPreferences = getSharedPreferences();
        dataSet = getDataSet();
        themeColors = getThemeColors();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set surface for execute activity
        globalLayout.setGlobalLayout(dataSet.SurfaceDataExecute(), surface);
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
        switch (v.getId()){
            case(R.id.button_manual):
                // Disable automatic drive mode
                button_auto.setChecked(false);
               controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_auto));
                if( ((ToggleButton) v).isChecked() ) {
                    // Show joystick buttons to control with
                    localLayout.setLocalLayout(R.id.fragment_container, R.layout.layout_joystick_button);
                    // Enable button for sensor drive mode
                    button_sensor_calibration.setEnabled(true);
                    // Start publisher thread
                    DataSet.publisher.startPublisherThread();
                    DataSet.subscriber.startSubscriberThread();
                   controlDataAcquisition.startControlDataAcquisitionThread(getResources().getString(R.string.control_mode_manual_joystick));
                }
                else{
                    button_sensor_calibration.setEnabled(false);
                    button_sensor_calibration.setChecked(false);
                    localLayout.setLocalLayout(R.id.fragment_container, 0);
                    // Stop sensor acquisition thread if is still alive
                   controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_manual_sensor));
                   controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_manual_joystick));

                }
                break;
            case(R.id.button_auto):
                // Disable manual drive mode and sensor drive mode, etc.
                button_manual.setChecked(false);
                button_sensor_calibration.setEnabled(false);
                button_sensor_calibration.setChecked(false);
                // Hide drive control buttons
                if(localLayout != null) localLayout.setLocalLayout(R.id.fragment_container, 0);
                if( ((ToggleButton) v).isChecked() ) {
                    // Stop acquisition threads if they are still alive
                   controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_manual_sensor));
                   controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_manual_joystick));
                    // Start publisher thread
                    DataSet.publisher.startPublisherThread();
                    DataSet.subscriber.startSubscriberThread();
                    // Start automatic mode thread
                   controlDataAcquisition.startControlDataAcquisitionThread(getResources().getString(R.string.control_mode_auto));
                }
            else{
                   controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_auto));
            }
            break;
            case(R.id.button_sensor_calibration):
                // This mode is only available in manual mode
                if( ((ToggleButton) v).isChecked() ) {
                    // Load sensor drive button
                    localLayout.setLocalLayout(R.id.fragment_container, R.layout.layout_joystick_button);
                   controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_manual_joystick));
                    customDialog(getResources().getString(R.string.dialog_message_start_calibration));
                }
                else{
                    localLayout.setLocalLayout(R.id.fragment_container, R.layout.layout_joystick_button);
                    // Load joystick buttons
                   controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_manual_sensor));
                   controlDataAcquisition.startControlDataAcquisitionThread(getResources().getString(R.string.control_mode_manual_joystick));
                }
                break;
            case(R.id.button_exit):
                // Stop publisher and return to MainActivity
                customDialog(getResources().getString(R.string.dialog_message_close_connection));
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int cmdValue;

        switch (v.getId()) {
            case (R.id.button_sensor):
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    cmdValue = 1;
                    sendDataToPublisher(DataSet.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal(), cmdValue, -1);
                    setBackgroundForJoystickButtons(R.drawable.ic_sensor_pressed);

                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    cmdValue = 0;
                    sendDataToPublisher(DataSet.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal(), cmdValue, -1);
                    setBackgroundForJoystickButtons(R.drawable.ic_sensor_active);
                }
                break;
            case(R.id.button_up):
                if(!button_sensor_calibration.isChecked() && button_sensor_calibration.isEnabled()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        cmdValue = 1;
                        sendDataToPublisher(DataSet.DriveMode.MOVE_FORWARD_WITH_BUTTON.ordinal(), cmdValue, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_up);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        cmdValue = 0;
                        sendDataToPublisher(DataSet.DriveMode.MOVE_FORWARD_WITH_BUTTON.ordinal(), cmdValue, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);
                    }
                }
                break;
            case(R.id.button_down):
                if(!button_sensor_calibration.isChecked() && button_sensor_calibration.isEnabled()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        cmdValue = 1;
                        sendDataToPublisher(DataSet.DriveMode.MOVE_BACKWARD_WITH_BUTTON.ordinal(), cmdValue, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_down);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        cmdValue = 0;
                        sendDataToPublisher(DataSet.DriveMode.MOVE_BACKWARD_WITH_BUTTON.ordinal(), cmdValue, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);
                    }
                }
                break;
            case(R.id.button_left):
                if(!button_sensor_calibration.isChecked() && button_sensor_calibration.isEnabled()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        cmdValue = 1;
                        sendDataToPublisher(DataSet.DriveMode.TURN_LEFT_WITH_BUTTON.ordinal(), cmdValue, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_left);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        cmdValue = 0;
                        sendDataToPublisher(DataSet.DriveMode.TURN_LEFT_WITH_BUTTON.ordinal(), cmdValue, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);
                    }
                }
                break;
            case(R.id.button_right):
                if(!button_sensor_calibration.isChecked() && button_sensor_calibration.isEnabled()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        cmdValue = 1;
                        sendDataToPublisher(DataSet.DriveMode.TURN_RIGHT_WITH_BUTTON.ordinal(), cmdValue, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_right);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        cmdValue = 0;
                        sendDataToPublisher(DataSet.DriveMode.TURN_RIGHT_WITH_BUTTON.ordinal(), cmdValue, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);
                    }
                }
                break;
        }
        return false;
    }

    private void sendDataToPublisher(int cmd, int cmdValue, int speed){
        int[] buttonData = new int[10];
        buttonData[cmd] = cmdValue;

        Bundle bundle = new Bundle();
        Message msg = new Message();

        bundle.putIntArray(getResources().getString(R.string.button_state_array), buttonData);
        if(speed != -1) bundle.putInt(getResources().getString(R.string.speed), speed);

        msg.setData(bundle);

        if (controlDataAcquisition.manualSensorModeThread != null) {
            if (controlDataAcquisition.manualSensorModeThread.isAlive()) {
                DataSet.handlerForControlDataAcquisition.sendMessage(msg);
            }
        }
        else if (controlDataAcquisition.manualJoystickModeThread != null) {
            if (controlDataAcquisition.manualJoystickModeThread.isAlive()) {
                DataSet.handlerForControlDataAcquisition.sendMessage(msg);
            }
        }
    }
    
    private void setBackgroundForJoystickButtons(int drawableResource) {
        relativeLayout.setBackgroundDrawable(getResources().getDrawable(drawableResource));
    }

    @Override
    public void localLayoutCallback() {
        // Get layout for joystick buttons
        relativeLayout = (RelativeLayout) findViewById(R.id.joystick_buttons);

        // Callback method to set button references when layout is valid
        if(button_sensor_calibration.isChecked()){
            Button sensor = (Button) findViewById(R.id.button_sensor);
            sensor.setOnTouchListener(this);
            // Set background for sensor button
        }
        if(button_manual.isChecked()) {
            Button button_up = (Button) findViewById(R.id.button_up);
            Button button_down = (Button) findViewById(R.id.button_down);
            Button button_left = (Button) findViewById(R.id.button_left);
            Button button_right = (Button) findViewById(R.id.button_right);
            seekBar_speed = (SeekBar) findViewById(R.id.seek_bar_speed);

            button_up.setOnTouchListener(this);
            button_down.setOnTouchListener(this);
            button_left.setOnTouchListener(this);
            button_right.setOnTouchListener(this);
            seekBar_speed.setOnSeekBarChangeListener(this);
            // Set background for joystick buttons
            setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);

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
                    button_sensor_calibration.setChecked(false);
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
                   controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_manual_sensor));
                   controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_manual_joystick));
                   controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_auto));
                    // Stop publisher thread
                    DataSet.publisher.stopPublisherThread();
                    DataSet.subscriber.stopSubscriberThread();

                    stopService(MainActivity.nodeExecutorService);
                    // Go back to MainActivity
                    dialog.dismiss();
                    finish();
                    overridePendingTransition(R.anim.anim_in, R.anim.anim_out);
                }
                // Show dialog to calibrate for start position (by manual drive with sensor) and do some stuff
                if (message.equals(getResources().getString(R.string.dialog_message_start_calibration))) {
                   controlDataAcquisition.startControlDataAcquisitionThread(getResources().getString(R.string.control_mode_manual_sensor));
                    dialog.dismiss();
                    setBackgroundForJoystickButtons(R.drawable.ic_sensor_active);
                    seekBar_speed.setVisibility(View.INVISIBLE);
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
    public void subscriberCallback(List<JoyFeedback> messageList) {
        Log.d("@ExecuteActivity->subscriberCallback", "Change led state");

        ImageView led_manual = (ImageView) findViewById(R.id.led_manual);
        ImageView led_auto = (ImageView) findViewById(R.id.led_auto);

        int mSize = messageList.size();
        for(int i=0;i<mSize;i++){
            JoyFeedback object = messageList.get(i);
            switch(object.getId()){
                case(0):
                    // Set led for manual mode
                    if(object.getIntensity() > 0) setFeedbackLed(led_manual, R.drawable.ic_led_on);
                    else setFeedbackLed(led_manual, R.drawable.ic_led_off);
                    break;
                case(1):
                    // Set led for auto mode
                    if(object.getIntensity() > 0) setFeedbackLed(led_auto, R.drawable.ic_led_on);
                    else setFeedbackLed(led_auto, R.drawable.ic_led_off);
                    break;
            }
        }
    }

    private void setFeedbackLed(final ImageView ledType, final int led) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ledType.setBackgroundDrawable(getResources().getDrawable(led));
            }
        });
    }
}
