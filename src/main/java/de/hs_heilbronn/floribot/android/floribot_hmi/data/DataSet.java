package de.hs_heilbronn.floribot.android.floribot_hmi.data;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.Arrays;

import de.hs_heilbronn.floribot.android.floribot_hmi.communication.ControlDataAcquisition;
import de.hs_heilbronn.floribot.android.floribot_hmi.communication.Publisher;

/**
 * Created by mr on 10.05.14.
 */
public class DataSet extends Application {


    public static ControlDataAcquisition controlDataAcquisition;
    public static Handler handlerForJoystickButton;
    public static boolean isRunning;
    public static JoystickEventExecutor.JoystickEventListener joystickEventListener = null;
    private final Context context;


    public DataSet(Context context) {
        this.context = context;
    }

    // Init talker for publisher
    public static Publisher talker;
   // public static SensorDataAcquisition sensorDataAcquisition;
    public static Handler handlerForPublishingData = null;
    public static Handler handlerForControlDataAcquisition = null;
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

    //----Parameters for surface----
    private int arrayOffset;
    private int pxWidth, pxHeight;
    private float dpWidth, dpHeight, factorHeight, factorWidth;
    float[] pointsArray;

    public float topBarHeightInDp = 30;
    public float bottomBarHeightInDp = 55;
    public float bottomBarWidthInDp = 100;
    public float bottomBarWidthInPx;

    private float lineWidth = 2;

    public static enum ThemeColor{
        // Background color, foreground color, text color
        STANDARD("#fffef2", "#0071ff", "#fffef2"),
        INVERT("#0071ff", "#fffef2", "#0071ff");

        public final String backgroundColor, foregroundColor, textColor;

        ThemeColor(String backgroundColor, String foregroundColor, String textColor) {
            this.backgroundColor = backgroundColor;
            this.foregroundColor = foregroundColor;
            this.textColor = textColor;
        }
    }

    public void SurfaceInit(){
        // Calculate display size
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        pxWidth = displayMetrics.widthPixels;
        pxHeight = displayMetrics.heightPixels;
        dpWidth = pxWidth / displayMetrics.density;
        dpHeight = pxHeight / displayMetrics.density;

        // Factor dp to px
        factorHeight = (pxHeight / dpHeight);
        factorWidth = (pxWidth / dpWidth);
        Log.d("@DatSet", "factorHeight" + factorHeight);
        Log.d("@DatSet", "factorWidth" + factorWidth);

        bottomBarWidthInPx = factorWidth * bottomBarWidthInDp;

        arrayOffset = 0;
    }

    public Bundle SurfaceDataMain(){

        // Initialize surface data
        SurfaceInit();

        float middleBarMarginTopInDp = 65;
        float middleBarWidthInDp = 390;
        float middleBarHeightInDp = 45;
        float middleBarOffsetInDp = 15;

        float middleBarMarginTopInPx = factorHeight * middleBarMarginTopInDp;
        float middleBarWidthInPx = factorHeight * middleBarWidthInDp;
        float middleBarHeightInPx = factorHeight * middleBarHeightInDp;
        float middleBarOffsetInPx = factorHeight * middleBarOffsetInDp;

        float[] middleBarPoints = new float[8];
        int pathCount = 2;


        // Get draw data for top bar
        GenerateStandardLayout();

        for(int i=0;i<=2;i++){
            // Point top left
            middleBarPoints[0] = 0;
            middleBarPoints[1] = middleBarMarginTopInPx + middleBarHeightInPx * i + middleBarOffsetInPx * i;
            // Point top right
            middleBarPoints[2] = (float) (middleBarWidthInPx - ((middleBarOffsetInPx*Math.tan(Math.PI/6)) + middleBarHeightInPx*Math.tan(Math.PI/6)) * i);
            middleBarPoints[3] = middleBarMarginTopInPx + middleBarHeightInPx * i + middleBarOffsetInPx * i;
            // Point bottom right
            middleBarPoints[4] = (float) (middleBarWidthInPx - middleBarHeightInPx*Math.tan(Math.PI/6) - (middleBarHeightInPx*Math.tan(Math.PI/6) + middleBarOffsetInPx*Math.tan(Math.PI/6))* i) ;
            middleBarPoints[5] = middleBarMarginTopInPx + middleBarHeightInPx * i + middleBarHeightInPx + middleBarOffsetInPx * i;
            // Point bottom left
            middleBarPoints[6] = 0;
            middleBarPoints[7] = middleBarMarginTopInPx + middleBarHeightInPx * i + middleBarHeightInPx + middleBarOffsetInPx * i;

            pointsArray = Arrays.copyOf(pointsArray, pointsArray.length + middleBarPoints.length + 1);
            setPointArray(middleBarPoints);
            pathCount++;
        }

        // Return surface data
        Bundle surfaceData = new Bundle();
        surfaceData.putFloatArray("pointsArray", pointsArray);
        surfaceData.putInt("pathCount", pathCount);

        return surfaceData;
    }

    public Bundle SurfaceDataExecute() {

        // Initialize surface data
        SurfaceInit();

        float[] bottomBarPoints = new float[8];
        float[] svRectArray = new float[12];

        float bottomBarHeightInPx = factorHeight * bottomBarHeightInDp;
        float bottomBarExtensionWidthInDp = 400;
        float bottomBarExtensionWidthInPx = factorWidth * bottomBarExtensionWidthInDp;
        int pathCount = 2; // Number of paths to draw


        // Get draw data for top bar
        GenerateStandardLayout();

        // ----- Create path for bottom bar extension -----
        // Point top left
        bottomBarPoints[0] = pxWidth - bottomBarExtensionWidthInPx;
        bottomBarPoints[1] = pxHeight - bottomBarHeightInPx - factorHeight * topBarHeightInDp;
        // Point top right
        bottomBarPoints[2] = pxWidth - bottomBarWidthInPx - lineWidth;
        bottomBarPoints[3] = pxHeight - bottomBarHeightInPx - factorHeight * topBarHeightInDp;
        // Point bottom right
        bottomBarPoints[4] = (float) (pxWidth - bottomBarWidthInPx - bottomBarHeightInPx * Math.tan(Math.PI/6) - lineWidth );
        bottomBarPoints[5] = pxHeight;
        // Point bottom left
        bottomBarPoints[6] = (float) (pxWidth - bottomBarExtensionWidthInPx - bottomBarHeightInPx * Math.tan(Math.PI/6));
        bottomBarPoints[7] = pxHeight;

        pointsArray = Arrays.copyOf(pointsArray, pointsArray.length + bottomBarPoints.length+1);
        setPointArray(bottomBarPoints);
        pathCount++;
        // -------------------------------------

        // Create data for sensor visualization (This data is excluded from normal surface data)
        float svBorderPadding = 5;
        float svWidth = 20;
        float svBottom = 90;
        float svOffset = 10;
        // Note: Camera width need to be the full length of sensor visualization beam
        float cameraViewWidthInPx = 400;
        float cameraViewHeightInPx = 200;

        // Create rectangle for top sensor visualization (visualization for steer amount)
        // -------------------------------------
        // Distance from left display border to left side of rectangle
        svRectArray[0] = factorWidth * (svBorderPadding + svWidth + svOffset) + cameraViewWidthInPx/2;
        // Distance from top display border to top side of rectangle
        svRectArray[1] = factorHeight * (topBarHeightInDp + svBorderPadding);
        // Distance from left display border to right side of rectangle
        svRectArray[2] = svRectArray[0];
        // Distance from top display border to bottom side of rectangle
        svRectArray[3] = factorHeight * (topBarHeightInDp + svBorderPadding + svWidth);
        // -------------------------------------

        // Create rectangle for left sensor visualization (visualization for drive amount)
        // -------------------------------------
        // Distance from left display border to left side of rectangle
        svRectArray[4] = factorWidth * svBorderPadding;
        // Distance from top display border to top side of rectangle
        svRectArray[5] = factorHeight * (topBarHeightInDp  + svBorderPadding + svWidth + svOffset) + cameraViewHeightInPx/2;
        // Distance from left display border to right side of rectangle
        svRectArray[6] = factorWidth * (svBorderPadding + svWidth);
        // Distance from top display border to bottom side of rectangle
        svRectArray[7] = svRectArray[5];
        // -------------------------------------

        // -------------------------------------
        // ONLY FOR DEBUG !!!
        // -------------------------------------
        // Create rectangle for pseudo camera preview
        // -------------------------------------
        // Distance from left display border to left side of rectangle
        svRectArray[8] = factorWidth * (svBorderPadding + svWidth + svOffset);
        // Distance from top display border to top side of rectangle
        svRectArray[9] = factorHeight * (topBarHeightInDp  + svBorderPadding + svWidth + svOffset);
        // Distance from left display border to right side of rectangle
        svRectArray[10] = factorWidth * (svBorderPadding + svWidth + svOffset) + cameraViewWidthInPx;
        // Distance from top display border to bottom side of rectangle
        svRectArray[11] = factorHeight * (topBarHeightInDp  + svBorderPadding + svWidth + svOffset) + cameraViewHeightInPx;
        // -------------------------------------


        // Return surface data
        Bundle surfaceData = new Bundle();
        surfaceData.putFloatArray("pointsArray", pointsArray);
        surfaceData.putInt("pathCount", pathCount);
        surfaceData.putFloatArray("svRectArray", svRectArray);

        return surfaceData;
    }

    private void GenerateStandardLayout() {

        float[] topBarPoints = new float[8];
        float[] bottomBarPoints = new float[8];
        float bottomBarHeightInPx = factorHeight * bottomBarHeightInDp;


        // ----- Create path for top bar -----
        // Point top left
        topBarPoints[0] = 0;
        topBarPoints[1] = 0;
        // Point top right
        topBarPoints[2] = pxWidth;
        topBarPoints[3] = 0;
        // Point bottom right
        topBarPoints[4] = pxWidth;
        topBarPoints[5] = factorHeight * topBarHeightInDp;
        // Point bottom left
        topBarPoints[6] = 0;
        topBarPoints[7] = factorHeight * topBarHeightInDp;

        pointsArray = new float[topBarPoints.length + 1];
        setPointArray(topBarPoints);
        // -------------------------------------

        // ----- Create path for bottom bar -----
        // Point top left
        bottomBarPoints[0] = pxWidth - bottomBarWidthInPx;
        bottomBarPoints[1] = pxHeight - bottomBarHeightInPx - factorHeight * topBarHeightInDp;
        // Point top right
        bottomBarPoints[2] = pxWidth;
        bottomBarPoints[3] = pxHeight - bottomBarHeightInPx - factorHeight * topBarHeightInDp;
        // Point bottom right
        bottomBarPoints[4] = pxWidth;
        bottomBarPoints[5] = pxHeight;
        // Point bottom left
        bottomBarPoints[6] = (float) (pxWidth - bottomBarWidthInPx - bottomBarHeightInPx * Math.tan(Math.PI/6));
        bottomBarPoints[7] = pxHeight;

        pointsArray = Arrays.copyOf(pointsArray, pointsArray.length + bottomBarPoints.length + 1);
        setPointArray(bottomBarPoints);
        // -------------------------------------
    }

    private void setPointArray(float[] array) {
        int i;
        for(i=0;i<=array.length-1;i++){
            pointsArray[arrayOffset + i] = array[i];
        }
        // Mark end of path data
        pointsArray[arrayOffset + i] = -1;
        arrayOffset += array.length+1;
    }
}
