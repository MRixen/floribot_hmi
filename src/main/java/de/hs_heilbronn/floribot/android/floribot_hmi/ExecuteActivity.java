package de.hs_heilbronn.floribot.android.floribot_hmi;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;

import de.hs_heilbronn.floribot.android.floribot_hmi.communication.ControlDataAcquisition;
import de.hs_heilbronn.floribot.android.floribot_hmi.communication.NodeExecutorService;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;
import de.hs_heilbronn.floribot.android.floribot_hmi.gui.LayoutManager;
import de.hs_heilbronn.floribot.android.floribot_hmi.gui.MySurfaceView;


public class ExecuteActivity extends BaseClass implements View.OnTouchListener, LayoutManager.LayoutManagerInterface {

    public static NodeExecutorService nodeExecutorService;
    private ToggleButton sensorDriveModeEnabled;
    private ToggleButton button_manual;
    private ToggleButton button_auto;
    private Button button_deadman, button_up, button_down, button_left, button_right;
    //private SurfaceViewExecuteActivity surfaceViewExecuteActivity;
    private SurfaceView surface;
    private SurfaceHolder holder;
    private LayoutManager layoutManager;
    private MySurfaceView mySurfaceView;
    private DataSet dataSet;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_execute);


        DataSet.controlDataAcquisition = new ControlDataAcquisition(getApplicationContext());

        sensorDriveModeEnabled = (ToggleButton) findViewById(R.id.button_sensor_calibration);
        button_manual = (ToggleButton) findViewById(R.id.button_manual);
        button_auto = (ToggleButton) findViewById(R.id.button_auto);

        dataSet = new DataSet(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        nodeExecutorService = new NodeExecutorService();

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restart surface view
        //surfaceViewMainActivity.resume();
        setSurfaceView(R.id.surface_execute);
        // Set surface for main activity
        Bundle surfaceDataBundle = dataSet.SurfaceDataExecute();
        mySurfaceView.resume(surfaceDataBundle);

    }

    private void setSurfaceView(int layout_surface) {
        surface = (SurfaceView) findViewById(layout_surface);
        surface.setZOrderOnTop(false);
        holder = surface.getHolder();
        mySurfaceView = new MySurfaceView(this,holder);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mySurfaceView.pause();
    }

    public void onButtonClicked(View v){

        float[] axesData;
        int[] buttonData;

        switch (v.getId()){
            case(R.id.button_manual):
                Log.d("@button manual: ", "inside...");
                // Disable automatic drive mode
                button_auto.setChecked(false);
                if( ((ToggleButton) v).isChecked() ) {
                    Log.d("@ExecuteActivity#onButtonClicked", "button_manual is enabled");


                    // Show joystick buttons to control with
                    setLayout(R.layout.layout_joystick_button);
                    // Enable button for sensor drive mode
                    sensorDriveModeEnabled.setEnabled(true);
                    // Start publisher thread
                    DataSet.talker.startPublisherThread();
                    DataSet.controlDataAcquisition.startControlDataAcquisitionThread("JoystickControl");

                }
                else{
                    Log.d("@ExecuteActivity#onButtonClicked", "button_manual is disabled");
                    sensorDriveModeEnabled.setEnabled(false);
                    sensorDriveModeEnabled.setChecked(false);
                    setLayout(0);
                    // Stop sensor acquisition thread if is still alive
                    //DataSet.sensorDataAcquisition.stopSensorDataAcquisitionThread();
                    DataSet.controlDataAcquisition.stopControlDataAcquisitionThread("SensorControl");
                    DataSet.controlDataAcquisition.stopControlDataAcquisitionThread("JoystickControl");

                }
                break;
            case(R.id.button_auto):
                // Disable manual drive mode and sensor drive mode, etc.
                button_manual.setChecked(false);
                sensorDriveModeEnabled.setEnabled(false);
                sensorDriveModeEnabled.setChecked(false);
                // Hide drive control buttons
                setLayout(0);
                // Stop sensor acquisition thread if is still alive
                DataSet.controlDataAcquisition.stopControlDataAcquisitionThread("SensorControl");
               // DataSet.controlDataAcquisition.stopControlDataAcquisitionThread("JoystickControl");

                // Start publisher thread
                DataSet.talker.startPublisherThread();
                // Send message data to handler inside the publisher thread
                //axesData = new float[3];
                buttonData = new int[10];
                buttonData[DataSet.DriveMode.AUTOMATIC_DRIVE.ordinal()] = 1;
                sendDataToPublisherThread(null, buttonData);
                break;
            case(R.id.button_sensor_calibration):
                if( ((ToggleButton) v).isChecked() ) {
                    DataSet.controlDataAcquisition.stopControlDataAcquisitionThread("JoystickControl");
                    dialog(getResources().getString(R.string.dialog_message_start_calibration));
                    // Load sensor drive button
                    setLayout(R.layout.layout_sensor_button);
                   // DataSet.ThemeColor.valueOf("#0071ff");

                }
                else{
                    setLayout(R.layout.layout_joystick_button);
                    // Load joystick buttons
                    DataSet.controlDataAcquisition.stopControlDataAcquisitionThread("SensorControl");
                    DataSet.controlDataAcquisition.startControlDataAcquisitionThread("JoystickControl");
                }
                break;
            case(R.id.button_exit):
                // Stop publisher and return to MainActivity
                dialog(getResources().getString(R.string.dialog_message_shutdown_publisher));
                Log.d("@ExecuteActivity#onButtonClicked", "button right");
                break;
        }
    }

    private void setLayout(int layout_resource) {
        if (layout_resource != 0) {
            layoutManager = new LayoutManager(layout_resource);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, layoutManager).commit();
        }
        else getSupportFragmentManager().beginTransaction().hide(layoutManager).commit();
    }

    private void sendDataToPublisherThread(float[] axesData, int[] buttonData) {

        Bundle bundle = new Bundle();
        Message msg = new Message();

        bundle.putIntArray("buttonData", buttonData);

        msg.setData(bundle);

        if (DataSet.controlDataAcquisition.sensorDataAcquisitionThread != null) {
            if (DataSet.controlDataAcquisition.sensorDataAcquisitionThread.isAlive()) {
                DataSet.handlerForControlDataAcquisition.sendMessage(msg);
            }
        }
        else if (DataSet.controlDataAcquisition.joystickDataAcquisitionThread != null) {
            if (DataSet.controlDataAcquisition.joystickDataAcquisitionThread.isAlive()) {
                DataSet.handlerForControlDataAcquisition.sendMessage(msg);
                Log.d("@snedDataToPublisherThread:", "inside and ok");
            }
        }

    }

    // Show dialog if back button is pressed
    public void dialog(final String message) {

        new AlertDialog.Builder(this)
                .setMessage(message)

                .setPositiveButton(R.string.dialog_positive_button, new DialogInterface.OnClickListener() {


                    public void onClick(DialogInterface dialog, int id) {

                        // Show dialog to disconnect publisher and do some stuff
                        if (message.equals(getResources().getString(R.string.dialog_message_shutdown_publisher))) {
                            // Stop sensor acquisition thread if is still alive
                            DataSet.controlDataAcquisition.stopControlDataAcquisitionThread("SensorControl");
                            // Stop publisher thread
                            DataSet.talker.stopPublisherThread();

                            stopService(MainActivity.myService);
                            // Go back to MainActivity
                            finish();
                            //overridePendingTransition(R.anim.anim_in, R.anim.anim_out);
                        }
                        // Show dialog to calibrate for start position (by manual drive with sensor) and do some stuff
                        if (message.equals(getResources().getString(R.string.dialog_message_start_calibration))) {
                            DataSet.controlDataAcquisition.startControlDataAcquisitionThread("SensorControl");
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_negative_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Set sensor toggle button state to false if user click cancel
                        if (message.equals(getResources().getString(R.string.dialog_message_start_calibration))) {
                            sensorDriveModeEnabled.setChecked(false);
                        }
                    }
                })
                .show();
    }

    // Handle back button action
    @Override
    public void onBackPressed() {

        // Check if service is active
        // If service is active: Show dialog
        // If not active: Simple return with back-button
        dialog(getResources().getString(R.string.dialog_message_shutdown_publisher));
    }

    // Callback method to set button references when layout is valid
    @Override
    public void callbackFragment(boolean flag, int fragmentLayout) {
        if(fragmentLayout == R.layout.layout_sensor_button) {
            Log.d("@callbackFragment: ", "ok...");
            button_deadman = (Button) findViewById(R.id.button_deadman);
            button_deadman.setOnTouchListener(this);
        }
        if(fragmentLayout == R.layout.layout_joystick_button) {
            Log.d("@callbackFragment: ", "ok...");
            button_up = (Button) findViewById(R.id.button_up);
            button_down = (Button) findViewById(R.id.button_down);
            button_left = (Button) findViewById(R.id.button_left);
            button_right = (Button) findViewById(R.id.button_right);
            button_up.setOnTouchListener(this);
            button_down.setOnTouchListener(this);
            button_left.setOnTouchListener(this);
            button_right.setOnTouchListener(this);

        }

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int[] buttonData;

        switch (v.getId()) {
            case (R.id.button_deadman):
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //Button Pressed
                    buttonData = new int[10];
                    buttonData[DataSet.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal()] = 1;
                    sendDataToPublisherThread(null, buttonData);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    //finger was lifted
                    buttonData = new int[10];
                    buttonData[DataSet.DriveMode.MOVE_ROBOT_WITH_IMU.ordinal()] = 0;
                    sendDataToPublisherThread(null, buttonData);
                }
                break;
            case(R.id.button_up):
                if(!sensorDriveModeEnabled.isChecked() && sensorDriveModeEnabled.isEnabled()){
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        // Send message data to handler inside the publisher thread
                        //axesData = new float[3];
                        buttonData = new int[10];
                        //axesData[0] = 10;
                        buttonData[DataSet.DriveMode.MOVE_FORWARD_WITH_BUTTON.ordinal()] = 1;
                        sendDataToPublisherThread(null, buttonData);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        // Send message data to handler inside the publisher thread
                        //axesData = new float[3];
                        buttonData = new int[10];
                        //axesData[0] = 10;
                        buttonData[DataSet.DriveMode.MOVE_FORWARD_WITH_BUTTON.ordinal()] = 0;
                        sendDataToPublisherThread(null, buttonData);
                    }
                }
                Log.d("@ExecuteActivity#onButtonClicked", "button up");
                break;
            case(R.id.button_down):
                if(!sensorDriveModeEnabled.isChecked() && sensorDriveModeEnabled.isEnabled()){
                    // Send message data to handler inside the publisher thread
                    //axesData = new float[3];
                    buttonData = new int[10];
                    //axesData[2] = -10;
                    buttonData[ DataSet.DriveMode.MOVE_BACKWARD_WITH_BUTTON.ordinal() ] = 1;
                    sendDataToPublisherThread(null, buttonData);
                }
                Log.d("@ExecuteActivity#onButtonClicked", "button down");
                break;
            case(R.id.button_left):
                if(!sensorDriveModeEnabled.isChecked() && sensorDriveModeEnabled.isEnabled()){
                    // Send message data to handler inside the publisher thread
                    // axesData = new float[3];
                    buttonData = new int[10];
                    //axesData[1] = 10;
                    buttonData[ DataSet.DriveMode.TURN_LEFT_WITH_BUTTON.ordinal() ] = 1;
                    sendDataToPublisherThread(null, buttonData);
                }
                Log.d("@ExecuteActivity#onButtonClicked", "button left");
                break;
            case(R.id.button_right):
                if(!sensorDriveModeEnabled.isChecked() && sensorDriveModeEnabled.isEnabled()){
                    // Send message data to handler inside the publisher thread
                    //axesData = new float[3];
                    buttonData = new int[10];
                    //axesData[1] = -10;
                    buttonData[ DataSet.DriveMode.TURN_RIGHT_WITH_BUTTON.ordinal() ] = 1;
                    //sendDataToPublisherThread(axesData, buttonData);
                    sendDataToPublisherThread(null, buttonData);
                }
                Log.d("@ExecuteActivity#onButtonClicked", "button right");
                break;
        }
        return false;
    }
}
