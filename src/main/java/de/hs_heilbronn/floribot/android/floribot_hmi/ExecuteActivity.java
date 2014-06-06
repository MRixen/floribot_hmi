package de.hs_heilbronn.floribot.android.floribot_hmi;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import de.hs_heilbronn.floribot.android.floribot_hmi.communication.ControlDataAcquisition;
import de.hs_heilbronn.floribot.android.floribot_hmi.communication.NodeExecutorService;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;
import de.hs_heilbronn.floribot.android.floribot_hmi.gui.LocalLayout;


public class ExecuteActivity extends BaseClass implements View.OnTouchListener, LocalLayout.LocalLayoutManager, SeekBar.OnSeekBarChangeListener {

    private ToggleButton button_manual, button_auto, button_sensor_calibration;

    //private DataSet dataSet;
    private LocalLayout localLayout;
    private RelativeLayout relativeLayout;

    public static NodeExecutorService nodeExecutorService;
    private int speed;
    private SeekBar seekBar_speed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_execute);

        DataSet.controlDataAcquisition = new ControlDataAcquisition(getApplicationContext());


        button_manual = (ToggleButton) findViewById(R.id.button_manual);
        button_auto = (ToggleButton) findViewById(R.id.button_auto);
        button_sensor_calibration = (ToggleButton) findViewById(R.id.button_sensor_calibration);
    }

    @Override
    protected void onStart() {
        super.onStart();
        nodeExecutorService = new NodeExecutorService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set surface for execute activity
        setGlobalLayout(R.id.surface_execute);
        Bundle surfaceDataBundle = dataSet.SurfaceDataExecute();
        setSurfaceView(surfaceDataBundle);
    }

    @Override
    protected void onPause() {
        super.onPause();
        globalLayout.pause();
    }

    @Override
    public void onBackPressed() {
        shutdownDialog(getResources().getString(R.string.dialog_message_shutdown_publisher));
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case (R.id.button_sensor):
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    joystickButtonHandle(DataSet.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal(), 1, -1);
                    setBackgroundForJoystickButtons(R.drawable.ic_sensor_pressed);

                    /*buttonData = new int[10];
                    buttonData[DataSet.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal()] = 1;
                    sendDataToPublisherThread(buttonData, -1);*/
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    joystickButtonHandle(DataSet.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal(), 0, -1);
                    setBackgroundForJoystickButtons(R.drawable.ic_sensor_active);

                    /*buttonData = new int[10];
                    buttonData[DataSet.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal()] = 0;
                    sendDataToPublisherThread(buttonData, -1);*/
                }
                break;
            case(R.id.button_up):
                if(!button_sensor_calibration.isChecked() && button_sensor_calibration.isEnabled()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        joystickButtonHandle(DataSet.DriveMode.MOVE_FORWARD_WITH_BUTTON.ordinal(), 1, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_up);

                       /* buttonData = new int[10];
                        buttonData[DataSet.DriveMode.MOVE_FORWARD_WITH_BUTTON.ordinal()] = 1;
                        sendDataToPublisherThread(buttonData, speed);*/
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        joystickButtonHandle(DataSet.DriveMode.MOVE_FORWARD_WITH_BUTTON.ordinal(), 0, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);

                        /*buttonData = new int[10];
                        buttonData[DataSet.DriveMode.MOVE_FORWARD_WITH_BUTTON.ordinal()] = 0;
                        sendDataToPublisherThread(buttonData, speed);*/
                    }
                }
                break;
            case(R.id.button_down):
                if(!button_sensor_calibration.isChecked() && button_sensor_calibration.isEnabled()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        joystickButtonHandle(DataSet.DriveMode.MOVE_BACKWARD_WITH_BUTTON.ordinal(), 1, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_down);

                       /* buttonData = new int[10];
                        buttonData[DataSet.DriveMode.MOVE_BACKWARD_WITH_BUTTON.ordinal()] = 1;
                        sendDataToPublisherThread(buttonData, speed);*/
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        joystickButtonHandle(DataSet.DriveMode.MOVE_BACKWARD_WITH_BUTTON.ordinal(), 0, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);

                        /*buttonData = new int[10];
                        buttonData[DataSet.DriveMode.MOVE_BACKWARD_WITH_BUTTON.ordinal()] = 0;
                        sendDataToPublisherThread(buttonData, speed);*/
                    }
                }
                break;
            case(R.id.button_left):
                if(!button_sensor_calibration.isChecked() && button_sensor_calibration.isEnabled()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        joystickButtonHandle(DataSet.DriveMode.TURN_LEFT_WITH_BUTTON.ordinal(), 1, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_left);

                        /*buttonData = new int[10];
                        buttonData[DataSet.DriveMode.TURN_LEFT_WITH_BUTTON.ordinal()] = 1;
                        sendDataToPublisherThread(buttonData, speed);*/
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        joystickButtonHandle(DataSet.DriveMode.TURN_LEFT_WITH_BUTTON.ordinal(), 0, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);

                        /*buttonData = new int[10];
                        buttonData[DataSet.DriveMode.TURN_LEFT_WITH_BUTTON.ordinal()] = 0;
                        sendDataToPublisherThread(buttonData, speed);*/
                    }
                }
                break;
            case(R.id.button_right):
                if(!button_sensor_calibration.isChecked() && button_sensor_calibration.isEnabled()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        joystickButtonHandle(DataSet.DriveMode.TURN_RIGHT_WITH_BUTTON.ordinal(), 1, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_right);

                        /*buttonData = new int[10];
                        buttonData[DataSet.DriveMode.TURN_RIGHT_WITH_BUTTON.ordinal()] = 1;
                        sendDataToPublisherThread(buttonData, speed);*/
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        joystickButtonHandle(DataSet.DriveMode.TURN_RIGHT_WITH_BUTTON.ordinal(), 0, speed);
                        setBackgroundForJoystickButtons(R.drawable.ic_joystick_active);

                        /*buttonData = new int[10];
                        buttonData[DataSet.DriveMode.TURN_RIGHT_WITH_BUTTON.ordinal()] = 0;
                        sendDataToPublisherThread(buttonData, speed);*/
                    }
                }
                break;
        }
        return false;
    }

    private void joystickButtonHandle(int cmd, int value, int speed){
        int[] buttonData = new int[10];
        buttonData[cmd] = value;
        sendDataToPublisherThread(buttonData, speed);
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

    public void onButtonClicked(View v){

        int[] buttonData;

        switch (v.getId()){
            case(R.id.button_manual):
                // Disable automatic drive mode
                button_auto.setChecked(false);
                if( ((ToggleButton) v).isChecked() ) {
                    // Show joystick buttons to control with
                    setLocalLayout(R.layout.layout_joystick_button);
                    // Enable button for sensor drive mode
                    button_sensor_calibration.setEnabled(true);
                    // Start publisher thread
                    DataSet.talker.startPublisherThread();
                    DataSet.controlDataAcquisition.startControlDataAcquisitionThread(getResources().getString(R.string.control_mode_joystick));

                }
                else{
                    button_sensor_calibration.setEnabled(false);
                    button_sensor_calibration.setChecked(false);
                    setLocalLayout(0);
                    // Stop sensor acquisition thread if is still alive
                    DataSet.controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_sensor));
                    DataSet.controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_joystick));

                }
                break;
            case(R.id.button_auto):
                // Disable manual drive mode and sensor drive mode, etc.
                button_manual.setChecked(false);
                button_sensor_calibration.setEnabled(false);
                button_sensor_calibration.setChecked(false);
                // Hide drive control buttons
                setLocalLayout(0);
                // Stop sensor acquisition thread if is still alive
                DataSet.controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_sensor));
                // Start publisher thread
                DataSet.talker.startPublisherThread();
                buttonData = new int[10];
                buttonData[DataSet.DriveMode.AUTOMATIC_DRIVE.ordinal()] = 1;
                sendDataToPublisherThread(buttonData, -1);
                break;
            case(R.id.button_sensor_calibration):
                if( ((ToggleButton) v).isChecked() ) {
                    // Load sensor drive button
                    setLocalLayout(R.layout.layout_joystick_button);
                    DataSet.controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_joystick));
                    shutdownDialog(getResources().getString(R.string.dialog_message_start_calibration));
                }
                else{
                    setLocalLayout(R.layout.layout_joystick_button);
                    // Load joystick buttons
                    DataSet.controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_sensor));
                    DataSet.controlDataAcquisition.startControlDataAcquisitionThread(getResources().getString(R.string.control_mode_joystick));
                }
                break;
            case(R.id.button_exit):
                // Stop publisher and return to MainActivity
                shutdownDialog(getResources().getString(R.string.dialog_message_shutdown_publisher));
                break;
        }
    }

    private void setLocalLayout(int local_layout_resource) {
        if (local_layout_resource != 0) {
            localLayout = new LocalLayout(local_layout_resource);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, localLayout).commit();
        }
        else getSupportFragmentManager().beginTransaction().hide(localLayout).commit();
    }

    private void sendDataToPublisherThread(int[] buttonData, int speed) {

        Bundle bundle = new Bundle();
        Message msg = new Message();

        bundle.putIntArray(getResources().getString(R.string.button_state_array), buttonData);
        if(speed != -1) bundle.putInt(getResources().getString(R.string.speed), speed);

        msg.setData(bundle);

        if (DataSet.controlDataAcquisition.sensorDataAcquisitionThread != null) {
            if (DataSet.controlDataAcquisition.sensorDataAcquisitionThread.isAlive()) {
                DataSet.handlerForControlDataAcquisition.sendMessage(msg);
            }
        }
        else if (DataSet.controlDataAcquisition.joystickDataAcquisitionThread != null) {
            if (DataSet.controlDataAcquisition.joystickDataAcquisitionThread.isAlive()) {
                DataSet.handlerForControlDataAcquisition.sendMessage(msg);
            }
        }
    }

    public void shutdownDialog(final String message) {
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
                if (message.equals(getResources().getString(R.string.dialog_message_shutdown_publisher))) {
                    // Stop sensor acquisition thread if is still alive
                    DataSet.controlDataAcquisition.stopControlDataAcquisitionThread(getResources().getString(R.string.control_mode_sensor));
                    // Stop publisher thread
                    DataSet.talker.stopPublisherThread();

                    stopService(MainActivity.nodeExecutorService);
                    // Go back to MainActivity
                    finish();
                    overridePendingTransition(R.anim.anim_in, R.anim.anim_out);
                }
                // Show dialog to calibrate for start position (by manual drive with sensor) and do some stuff
                if (message.equals(getResources().getString(R.string.dialog_message_start_calibration))) {
                    DataSet.controlDataAcquisition.startControlDataAcquisitionThread(getResources().getString(R.string.control_mode_sensor));
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


}
