package de.hs_heilbronn.floribot.android.floribot_hmi.data;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;

import java.util.Arrays;
import java.util.List;

import de.hs_heilbronn.floribot.android.floribot_hmi.R;
import de.hs_heilbronn.floribot.android.floribot_hmi.communication.ControlDataAcquisition;
import de.hs_heilbronn.floribot.android.floribot_hmi.communication.Publisher;
import de.hs_heilbronn.floribot.android.floribot_hmi.communication.Subscriber;
import sensor_msgs.JoyFeedback;

/**
 * Created by mr on 10.05.14.
 *
 * This class provides global data for all classes
 */
public class DataSet extends Application {

    private static Context context;

    public static ControlDataAcquisition controlDataAcquisition;
    public static CustomEventExecutor.CustomEventListener customEventListener = null;

    public static Handler handlerForPublishingData = null, handlerForControlDataAcquisition = null, handlerForVisualization = null;
    public static Publisher publisher;

    public static boolean isRunning;
    public static Subscriber subscriber;
    public static SubscriberInterface subscriberInterface;
    private int arrayOffset, pxWidth, pxHeight;
    private float factorHeight, factorWidth, bottomBarWidthInPx, offsetToBottomBarExtensionInPx, bottomBarHeightInPx;
    float[] pointsArray;


    public DataSet(Context context) {
        this.context = context;
    }

    public static enum DriveMode{
        // Constants for control buttons
        MANUAL_DRIVE,
        AUTOMATIC_DRIVE,
        MOVE_PAN_TILT_WITH_IMU,
        MOVE_ROBOT_WITH_IMU,
        NOT_ASSIGNED_ONE,
        NOT_ASSIGNED_TWO,
        TURN_LEFT_WITH_BUTTON,
        TURN_RIGHT_WITH_BUTTON,
        MOVE_FORWARD_WITH_BUTTON,
        MOVE_BACKWARD_WITH_BUTTON
    }

    public static enum ThemeColor{
        // Background color, foreground color, text color
        BlueLight(context.getResources().getColor(R.color.ModernWhite), context.getResources().getColor(R.color.ModernBlue), context.getResources().getColor(R.color.ModernWhite)),
        GreenLight(context.getResources().getColor(R.color.White), context.getResources().getColor(R.color.ModernGreen), context.getResources().getColor(R.color.White));

        public final int backgroundColor, foregroundColor, textColor;

        ThemeColor(int backgroundColor, int foregroundColor, int textColor) {
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
        float dpWidth = pxWidth / displayMetrics.density;
        float dpHeight = pxHeight / displayMetrics.density;

        // Factor dp to px
        factorHeight = (pxHeight / dpHeight);
        factorWidth = (pxWidth / dpWidth);

        bottomBarWidthInPx = factorWidth * getRes(R.integer.bottomBarWidthInDp);
        offsetToBottomBarExtensionInPx = factorWidth * getRes(R.integer.offsetToBottomBarExtensionInDp);
        bottomBarHeightInPx = factorHeight * getRes(R.integer.bottomBarHeightInDp);

        arrayOffset = 0;
    }

    public Bundle SurfaceDataMain(){

        // Initialize surface data
        SurfaceInit();

        float beamMarginTop = context.getResources().getDimensionPixelSize(R.dimen.beamMarginTop);
        float beamWidth = context.getResources().getDimensionPixelSize(R.dimen.beamWidth);
        float beamHeight = context.getResources().getDimensionPixelSize(R.dimen.beamHeight);
        float beamOffset = context.getResources().getDimensionPixelSize(R.dimen.beamOffset);


        float[] middleBarPoints = new float[8];
        int pathQuantity = 2;


        // Get draw data for top bar
        GenerateStandardLayout();

        for(int i=0;i<=2;i++){
            // Point top left
            middleBarPoints[0] = beamOffset;
            middleBarPoints[1] = beamMarginTop + beamHeight * i + beamOffset * i;
            // Point top right
            middleBarPoints[2] = beamWidth;
            middleBarPoints[3] = beamMarginTop + beamHeight * i + beamOffset * i;
            // Point bottom right
            middleBarPoints[4] = beamWidth;
            middleBarPoints[5] = beamMarginTop + beamHeight * i + beamHeight + beamOffset * i;
            // Point bottom left
            middleBarPoints[6] = beamOffset;
            middleBarPoints[7] = beamMarginTop + beamHeight * i + beamHeight + beamOffset * i;

            pointsArray = Arrays.copyOf(pointsArray, pointsArray.length + middleBarPoints.length + 1);
            setPointArray(middleBarPoints);
            pathQuantity++;
        }

        // Return surface data
        Bundle surfaceData = new Bundle();
        surfaceData.putFloatArray(context.getResources().getString(R.string.glPointArray), pointsArray);
        surfaceData.putInt(context.getResources().getString(R.string.glPathQuantity), pathQuantity);

        return surfaceData;
    }

    public Bundle SurfaceDataExecute() {

        // Initialize surface data
        SurfaceInit();

        float[] bottomBarPoints = new float[8];
        float[] svRectArray = new float[12];

        float bottomBarExtensionWidthInPx = factorWidth * getRes(R.integer.bottomBarExtensionWidthInDp);
        int pathQuantity = 2; // Number of paths to draw

        // Get draw data for top bar
        GenerateStandardLayout();

        // ----- Create path for bottom bar extension -----
        // Point top left
        bottomBarPoints[0] = pxWidth - bottomBarExtensionWidthInPx;
        bottomBarPoints[1] = pxHeight - bottomBarHeightInPx - factorHeight * getRes(R.integer.topBarHeightInDp);
        // Point top right
        bottomBarPoints[2] = pxWidth - bottomBarWidthInPx - offsetToBottomBarExtensionInPx;
        bottomBarPoints[3] = pxHeight - bottomBarHeightInPx - factorHeight * getRes(R.integer.topBarHeightInDp);
        // Point bottom right
        bottomBarPoints[4] = (float) (pxWidth - bottomBarWidthInPx - bottomBarHeightInPx * Math.tan(Math.PI/6) - offsetToBottomBarExtensionInPx );
        bottomBarPoints[5] = pxHeight;
        // Point bottom left
        bottomBarPoints[6] = (float) (pxWidth - bottomBarExtensionWidthInPx - bottomBarHeightInPx * Math.tan(Math.PI/6));
        bottomBarPoints[7] = pxHeight;

        pointsArray = Arrays.copyOf(pointsArray, pointsArray.length + bottomBarPoints.length+1);
        setPointArray(bottomBarPoints);
        pathQuantity++;
        // -------------------------------------

        // Create data for sensor visualization (This data is excluded from normal surface data)               
        // Note: Camera width need to be the full length of sensor visualization beam
        float cameraViewWidthInPx = getRes(R.integer.cameraViewWidthInPx);
        float cameraViewHeightInPx = getRes(R.integer.cameraViewHeightInPx);

        // Create rectangle for top sensor visualization (visualization for steer amount)
        // -------------------------------------
        // Distance from left display border to left side of rectangle
        svRectArray[0] = factorWidth * (getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp) + getRes(R.integer.svOffsetInDp));
        // Distance from top display border to top side of rectangle
        svRectArray[1] = factorHeight * (getRes(R.integer.topBarHeightInDp) + getRes(R.integer.svBorderMarginInDp));
        // Distance from left display border to right side of rectangle
        svRectArray[2] = svRectArray[0] + cameraViewWidthInPx;
        // Distance from top display border to bottom side of rectangle
        svRectArray[3] = factorHeight * (getRes(R.integer.topBarHeightInDp) + getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp));
        // -------------------------------------

        // Create rectangle for left sensor visualization (visualization for drive amount)
        // -------------------------------------
        // Distance from left display border to left side of rectangle
        svRectArray[4] = factorWidth * getRes(R.integer.svBorderMarginInDp);
        // Distance from top display border to top side of rectangle
        svRectArray[5] = factorHeight * (getRes(R.integer.topBarHeightInDp)  + getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp) + getRes(R.integer.svOffsetInDp));
        // Distance from left display border to right side of rectangle
        svRectArray[6] = factorWidth * (getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp));
        // Distance from top display border to bottom side of rectangle
        svRectArray[7] = svRectArray[5] + cameraViewHeightInPx;
        // -------------------------------------

        // -------------------------------------
        // ONLY FOR DEBUG !!!
        // -------------------------------------
        // Create rectangle for pseudo camera preview
        // -------------------------------------
        // Distance from left display border to left side of rectangle
        svRectArray[8] = factorWidth * (getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp) + getRes(R.integer.svOffsetInDp));
        // Distance from top display border to top side of rectangle
        svRectArray[9] = factorHeight * (getRes(R.integer.topBarHeightInDp)  + getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp) + getRes(R.integer.svOffsetInDp));
        // Distance from left display border to right side of rectangle
        svRectArray[10] = factorWidth * (getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp) + getRes(R.integer.svOffsetInDp)) + cameraViewWidthInPx;
        // Distance from top display border to bottom side of rectangle
        svRectArray[11] = factorHeight * (getRes(R.integer.topBarHeightInDp)  + getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp) + getRes(R.integer.svOffsetInDp)) + cameraViewHeightInPx;
        // -------------------------------------


        // Return surface data
        Bundle surfaceData = new Bundle();
        surfaceData.putFloatArray(context.getResources().getString(R.string.glPointArray), pointsArray);
        surfaceData.putInt(context.getResources().getString(R.string.glPathQuantity), pathQuantity);
        surfaceData.putFloatArray(context.getResources().getString(R.string.svArray), svRectArray);

        return surfaceData;
    }
    
    private int getRes(int res){
        return context.getResources().getInteger(res);
    }

    private void GenerateStandardLayout() {

        float[] topBarPoints = new float[8];
        float[] bottomBarPoints = new float[8];

        // ----- Create path for top bar -----
        // Point top left
        topBarPoints[0] = 0;
        topBarPoints[1] = 0;
        // Point top right
        topBarPoints[2] = pxWidth;
        topBarPoints[3] = 0;
        // Point bottom right
        topBarPoints[4] = pxWidth;
        topBarPoints[5] = factorHeight * getRes(R.integer.topBarHeightInDp);
        // Point bottom left
        topBarPoints[6] = 0;
        topBarPoints[7] = factorHeight * getRes(R.integer.topBarHeightInDp);

        pointsArray = new float[topBarPoints.length + 1];
        setPointArray(topBarPoints);
        // -------------------------------------

        // ----- Create path for bottom bar -----
        // Point top left
        bottomBarPoints[0] = pxWidth - bottomBarWidthInPx;
        bottomBarPoints[1] = pxHeight - bottomBarHeightInPx - factorHeight * getRes(R.integer.topBarHeightInDp);
        // Point top right
        bottomBarPoints[2] = pxWidth;
        bottomBarPoints[3] = pxHeight - bottomBarHeightInPx - factorHeight * getRes(R.integer.topBarHeightInDp);
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

    // INterface for communication between subscriber and main thread
    public interface SubscriberInterface {
        public void subscriberCallback(List<JoyFeedback> message);
    }
}
