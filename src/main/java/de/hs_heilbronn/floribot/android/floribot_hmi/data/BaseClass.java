package de.hs_heilbronn.floribot.android.floribot_hmi.data;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.hs_heilbronn.floribot.android.floribot_hmi.AboutActivity;
import de.hs_heilbronn.floribot.android.floribot_hmi.HelpActivity;
import de.hs_heilbronn.floribot.android.floribot_hmi.R;
import de.hs_heilbronn.floribot.android.floribot_hmi.SettingsActivity;
import de.hs_heilbronn.floribot.android.floribot_hmi.communication.Node;
import sensor_msgs.JoyFeedback;

/**
 * Created by mr on 10.05.14.
 *
 * This class provides global data for all classes
 */
public class BaseClass extends ActionBarActivity {

    private static Context context;
    private int pxWidth, pxHeight;
    private float factorHeight, factorWidth;
    float[] pointsArray;

    public static Node node;
    public static Handler handlerForPublishingData = null, handlerForControlDataAcquisition = null, handlerForVisualization = null;
    public static SubscriberInterface subscriberInterface;

    private SharedPreferences sharedPreferences;
    private Dialog dialog;
    private Window dialogWindow;

    private ActionBar bar;
    private BaseClass.ThemeColor[] themeColors;
    private static int[] bgColor, fgColor, tColor;
    private static Drawable[] drawableDataGreen, drawableDataBlue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bgColor = new int[2];
        fgColor = new int[2];
        tColor = new int[2];
        drawableDataGreen = new Drawable[2];
        drawableDataBlue = new Drawable[2];
        bgColor[0] = getResources().getColor(R.color.ModernWhite);
        bgColor[1] = getResources().getColor(R.color.White);
        fgColor[0] = getResources().getColor(R.color.ModernBlue);
        fgColor[1] = getResources().getColor(R.color.ModernGreen);
        tColor[0] = getResources().getColor(R.color.ModernWhite);
        tColor[1] = getResources().getColor(R.color.White);
        drawableDataGreen[0] = getResources().getDrawable(R.drawable.button_background_not_pressed_modern_green);
        drawableDataGreen[1] = getResources().getDrawable(R.drawable.button_extension_modern_green);
        drawableDataBlue[0] = getResources().getDrawable(R.drawable.button_background_not_pressed_modern_blue);
        drawableDataBlue[1] = getResources().getDrawable(R.drawable.button_extension_modern_blue);

        themeColors = ThemeColor.values();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Set dialog layout and style
        dialog = new Dialog(this, R.style.dialog_style);
        dialogWindow = dialog.getWindow();

        context = this;



    }

    @Override
    protected void onResume() {
        super.onResume();
        initActionBar();
    }

    public static enum DriveMode{
        // Constants for control buttons
        MANUAL_DRIVE,
        AUTOMATIC_DRIVE,
        MOVE_PAN_TILT_WITH_IMU,
        MOVE_ROBOT_WITH_IMU,
        NOT_ASSIGNED_ONE,
        NOT_ASSIGNED_TWO,
        TURN_RIGHT_WITH_BUTTON,
        TURN_LEFT_WITH_BUTTON,
        MOVE_FORWARD_WITH_BUTTON,
        MOVE_BACKWARD_WITH_BUTTON
    }

    public static enum ThemeColor{
        // Background color, foreground color, text color
        BlueLight(bgColor[0], fgColor[0], tColor[0], drawableDataBlue),
        GreenLight(bgColor[1], fgColor[1], tColor[1], drawableDataGreen);

        public final int backgroundColor, foregroundColor, textColor;
        public final Drawable[] drawable;

        ThemeColor(int backgroundColor, int foregroundColor, int textColor, Drawable[] drawable) {
            this.backgroundColor = backgroundColor;
            this.foregroundColor = foregroundColor;
            this.textColor = textColor;
            this.drawable = drawable;
        }
    }

    public void SurfaceInit(){
        // Calculate display size
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        Log.d("@SurfaceInit", Thread.currentThread().getName());

        pxWidth = displayMetrics.widthPixels;
        pxHeight = displayMetrics.heightPixels;
        float dpWidth = pxWidth / displayMetrics.density;
        float dpHeight = pxHeight / displayMetrics.density;

        // Factor dp to px
        factorHeight = (pxHeight / dpHeight);
        factorWidth = (pxWidth / dpWidth);
    }

    public Bundle getSurfaceDataExecute() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<Bundle> result = executorService.submit(new Callable<Bundle>() {
            @Override
            public Bundle call() throws Exception {
                Log.d("@DataSet->SurfaceDataExecute", Thread.currentThread().getName());
                // Initialize surface data
                SurfaceInit();


                float[] svRectArray = new float[12];


                int pathQuantity = 0; // Number of paths to draw

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
                svRectArray[1] = factorHeight * (getRes(R.integer.svMarginTopInDp) + getRes(R.integer.svBorderMarginInDp));
                // Distance from left display border to right side of rectangle
                svRectArray[2] = svRectArray[0] + cameraViewWidthInPx;
                // Distance from top display border to bottom side of rectangle
                svRectArray[3] = factorHeight * (getRes(R.integer.svMarginTopInDp) + getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp));
                // -------------------------------------

                // Create rectangle for left sensor visualization (visualization for drive amount)
                // -------------------------------------
                // Distance from left display border to left side of rectangle
                svRectArray[4] = factorWidth * getRes(R.integer.svBorderMarginInDp);
                // Distance from top display border to top side of rectangle
                svRectArray[5] = factorHeight * (getRes(R.integer.svMarginTopInDp)  + getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp) + getRes(R.integer.svOffsetInDp));
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
                svRectArray[9] = factorHeight * (getRes(R.integer.svMarginTopInDp)  + getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp) + getRes(R.integer.svOffsetInDp));
                // Distance from left display border to right side of rectangle
                svRectArray[10] = factorWidth * (getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp) + getRes(R.integer.svOffsetInDp)) + cameraViewWidthInPx;
                // Distance from top display border to bottom side of rectangle
                svRectArray[11] = factorHeight * (getRes(R.integer.svMarginTopInDp)  + getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp) + getRes(R.integer.svOffsetInDp)) + cameraViewHeightInPx;
                // -------------------------------------
                pathQuantity++;


                // Return surface data
                Bundle surfaceData = new Bundle();
                surfaceData.putFloatArray(context.getResources().getString(R.string.glPointArray), pointsArray);
                surfaceData.putInt(context.getResources().getString(R.string.glPathQuantity), pathQuantity);
                surfaceData.putFloatArray(context.getResources().getString(R.string.svArray), svRectArray);

                return surfaceData;
            }
        });

        try {
            return result.get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private int getRes(int res){
        return context.getResources().getInteger(res);
    }

    // Interface for communication between subscriber and main thread
    public interface SubscriberInterface {
        public void subscriberCallback(List<JoyFeedback> message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.overflow_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case(R.id.popup_properties):
                Intent settingsIntent = new Intent(this, SettingsActivity.class);

                startActivity(settingsIntent);
                break;
            case(R.id.popup_help):
                Intent HelpIntent = new Intent(this, HelpActivity.class);
                startActivity(HelpIntent);
                break;
            case(R.id.popup_about):
                Intent AboutIntent = new Intent(this, AboutActivity.class);
                startActivity(AboutIntent);
                break;
        }
        return true;
    }

    public SharedPreferences getSharedPreferences(){
        return sharedPreferences;
    }

    public void initActionBar() {
        // Set up the action bar to show a dropdown list.

        bar = getSupportActionBar();
        // Set action bar parameters

        bar.setDisplayUseLogoEnabled(false);
        bar.setDisplayHomeAsUpEnabled(false);
        bar.setBackgroundDrawable(new ColorDrawable(themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor));
        // Prevent that actionBar show grey color
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayShowTitleEnabled(true);

    }

    public BaseClass.ThemeColor[] getThemeColors(){
        return themeColors;
    }

}
