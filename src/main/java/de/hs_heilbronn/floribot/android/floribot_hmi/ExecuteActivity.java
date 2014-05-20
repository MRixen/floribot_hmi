package de.hs_heilbronn.floribot.android.floribot_hmi;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ToggleButton;

import de.hs_heilbronn.floribot.android.floribot_hmi.communication.NodeExecutorService;
import de.hs_heilbronn.floribot.android.floribot_hmi.communication.SensorDataAcquisition;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;
import de.hs_heilbronn.floribot.android.floribot_hmi.gui.MySurfaceView;


public class ExecuteActivity extends BaseClass {

    public static NodeExecutorService nodeExecutorService;
    private ToggleButton sensorDriveModeEnabled;
    private ToggleButton button_manual;
    private ToggleButton button_auto;

    private SurfaceView surface;
    private SurfaceHolder holder;
    private MySurfaceView myView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_execute);

        DataSet.sensorDataAcquisition = new SensorDataAcquisition(getApplicationContext());

        sensorDriveModeEnabled = (ToggleButton) findViewById(R.id.button_sensor_switch);
        button_manual = (ToggleButton) findViewById(R.id.button_manual);
        button_auto = (ToggleButton) findViewById(R.id.button_auto);

        // Initialize surface view
        surface = (SurfaceView) findViewById(R.id.mySurface);
        surface.setZOrderOnTop(true);
        holder = surface.getHolder();
        myView = new MySurfaceView();

    }

    @Override
    protected void onStart() {
        super.onStart();
        nodeExecutorService = new NodeExecutorService();
        myView.startDrawThread(holder, this);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    public void onButtonClicked(View v){

        float[] axesData;
        int[] buttonData;

        switch (v.getId()){
            case(R.id.button_manual):
                // Disable automatic drive mode
                button_auto.setChecked(false);
                if( ((ToggleButton) v).isChecked() ) {
                    Log.d("@ExecuteActivity#onButtonClicked", "button_manual is enabled");
                    // Enable button for sensor drive mode
                    sensorDriveModeEnabled.setEnabled(true);
                    // Start publisher thread
                    DataSet.talker.startPublisherThread();
                }
                else{
                    Log.d("@ExecuteActivity#onButtonClicked", "button_manual is disabled");
                    sensorDriveModeEnabled.setEnabled(false);
                    // Stop sensor acquisition thread if is still alive
                    DataSet.sensorDataAcquisition.stopSensorDataAcquisitionThread();
                }
                break;
            case(R.id.button_auto):
                // Disable manual drive mode and sensor drive mode, etc.
                button_manual.setChecked(false);
                sensorDriveModeEnabled.setEnabled(false);
                sensorDriveModeEnabled.setChecked(false);
                // Stop sensor acquisition thread if is still alive
                DataSet.sensorDataAcquisition.stopSensorDataAcquisitionThread();
                // Start publisher thread
                DataSet.talker.startPublisherThread();
                // Send message data to handler inside the publisher thread
                axesData = new float[3];
                buttonData = new int[10];
                buttonData[DataSet.DriveMode.AUTOMATIC_DRIVE.ordinal()] = 1;
                sendDataToPublisherThread(axesData, buttonData);
                break;
            case(R.id.button_sensor_switch):
                if( ((ToggleButton) v).isChecked() ) {
                    dialog(getResources().getString(R.string.dialog_message_start_calibration));
                }
                else DataSet.sensorDataAcquisition.stopSensorDataAcquisitionThread();
                break;
            case(R.id.button_up):
                if(!sensorDriveModeEnabled.isChecked() && sensorDriveModeEnabled.isEnabled()){
                    // Send message data to handler inside the publisher thread
                    axesData = new float[3];
                    buttonData = new int[10];
                    axesData[0] = 10;
                    buttonData[ DataSet.DriveMode.MOVE_FORWARD_WITH_BUTTON.ordinal() ] = 1;
                    sendDataToPublisherThread(axesData, buttonData);
                }
                Log.d("@ExecuteActivity#onButtonClicked", "button up");
                break;
            case(R.id.button_down):
                if(!sensorDriveModeEnabled.isChecked() && sensorDriveModeEnabled.isEnabled()){
                    // Send message data to handler inside the publisher thread
                    axesData = new float[3];
                    buttonData = new int[10];
                    axesData[2] = -10;
                    buttonData[ DataSet.DriveMode.MOVE_BACKWARD_WITH_BUTTON.ordinal() ] = 1;
                    sendDataToPublisherThread(axesData, buttonData);
                }
                Log.d("@ExecuteActivity#onButtonClicked", "button down");
                break;
            case(R.id.button_left):
                if(!sensorDriveModeEnabled.isChecked() && sensorDriveModeEnabled.isEnabled()){
                    // Send message data to handler inside the publisher thread
                    axesData = new float[3];
                    buttonData = new int[10];
                    axesData[1] = 10;
                    buttonData[ DataSet.DriveMode.TURN_LEFT_WITH_BUTTON.ordinal() ] = 1;
                    sendDataToPublisherThread(axesData, buttonData);
                }
                Log.d("@ExecuteActivity#onButtonClicked", "button left");
                break;
            case(R.id.button_right):
                if(!sensorDriveModeEnabled.isChecked() && sensorDriveModeEnabled.isEnabled()){
                    // Send message data to handler inside the publisher thread
                    axesData = new float[3];
                    buttonData = new int[10];
                    axesData[1] = -10;
                    buttonData[ DataSet.DriveMode.TURN_RIGHT_WITH_BUTTON.ordinal() ] = 1;
                    sendDataToPublisherThread(axesData, buttonData);
                }
                Log.d("@ExecuteActivity#onButtonClicked", "button right");
                break;
            case(R.id.button_exit):
                // Stop publisher and return to MainActivity
                dialog(getResources().getString(R.string.dialog_message_shutdown_publisher));
                Log.d("@ExecuteActivity#onButtonClicked", "button right");
                break;
        }
    }

    private void sendDataToPublisherThread(float[] axesData, int[] buttonData) {

        Bundle bundle = new Bundle();
        Message msg = new Message();

        bundle.putFloatArray("axesData", axesData);
        bundle.putIntArray("buttonData", buttonData);

        msg.setData(bundle);
        if(DataSet.talker.t != null) {

            if (DataSet.talker.t.isAlive()) {
                DataSet.handler.sendMessage(msg);
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
                            DataSet.sensorDataAcquisition.stopSensorDataAcquisitionThread();
                            // Stop publisher thread
                            DataSet.talker.stopPublisherThread();

                            //DEBUG
                            myView.stopDrawThread();
                            //---

                            stopService(MainActivity.myService);
                            // Go back to MainActivity
                            finish();
                            overridePendingTransition(R.anim.anim_in, R.anim.anim_out);
                        }
                        // Show dialog to calibrate for start position (by manual drive with sensor) and do some stuff
                        if (message.equals(getResources().getString(R.string.dialog_message_start_calibration))) {
                            DataSet.sensorDataAcquisition.startSensorDataAcquisitionThread();
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
        dialog(getResources().getString(R.string.dialog_message_shutdown_publisher));
    }
}
