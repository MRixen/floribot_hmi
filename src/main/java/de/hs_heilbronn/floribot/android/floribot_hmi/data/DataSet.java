package de.hs_heilbronn.floribot.android.floribot_hmi.data;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;

import de.hs_heilbronn.floribot.android.floribot_hmi.communication.SensorDataAcquisition;
import de.hs_heilbronn.floribot.android.floribot_hmi.communication.Talker2;

/**
 * Created by mr on 10.05.14.
 */
public class DataSet extends Application {

    public DataSet(Context context) {

    }

    public static float bottomBarHeightInDp = 60;

    // Divider to divide the total display width
    public float topBarWidthDivider = 3;
    public float bottomBarLineOffset = 90; // dp

    // Color for top and bottom bar
    public int barColor = Color.parseColor("#0071ff");
    public int barLineColor = Color.parseColor("#FFFFFF");

    // Init talker for publisher
    public static Talker2 talker;
    public static SensorDataAcquisition sensorDataAcquisition;
    public static Handler handler = null;
    public static Handler handlerForVisualization = null;


    // Init constants for buttons
    public static enum DriveMode{
        MANUAL_DRIVE,
        AUTOMATIC_DRIVE,
        MOVE_PAN_TILT_WITH_IMU,
        MOVE_ROBOT_WITH_IMU,
        TURN_LEFT_WITH_BUTTON,
        TURN_RIGHT_WITH_BUTTON,
        MOVE_FORWARD_WITH_BUTTON,
        MOVE_BACKWARD_WITH_BUTTON
    }
}
