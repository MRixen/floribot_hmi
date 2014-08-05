package de.hs_heilbronn.floribot.android.floribot_hmi.data;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

import de.hs_heilbronn.floribot.android.floribot_hmi.About;
import de.hs_heilbronn.floribot.android.floribot_hmi.Help;
import de.hs_heilbronn.floribot.android.floribot_hmi.R;
import de.hs_heilbronn.floribot.android.floribot_hmi.Settings;
import de.hs_heilbronn.floribot.android.floribot_hmi.communication.Node;
import sensor_msgs.JoyFeedback;

/**
 * Created by mr on 10.05.14.
 *
 * This class provides global data for all classes
 */
public class BaseClass extends ActionBarActivity{

    private String titleName;
    public static Node node;
    public static Handler sendToNode = null, sendToDataAcquisition = null, sendToSensorVisualization = null;
    public static SubscriberMessageListener subscriberMessageListener;
    public static SensorCalibrationListener sensorCalibrationListener;
    private SharedPreferences sharedPreferences;
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
        drawableDataGreen = new Drawable[3];
        drawableDataBlue = new Drawable[3];
        bgColor[0] = getResources().getColor(R.color.ModernWhite);
        bgColor[1] = getResources().getColor(R.color.ModernWhite);
        fgColor[0] = getResources().getColor(R.color.ModernBlue);
        fgColor[1] = getResources().getColor(R.color.ModernGreen);
        tColor[0] = getResources().getColor(R.color.White);
        tColor[1] = getResources().getColor(R.color.White);
        drawableDataGreen[0] = getResources().getDrawable(R.drawable.button_background_not_pressed_modern_green);
        drawableDataGreen[1] = getResources().getDrawable(R.drawable.bottom_bar_middle_modern_green);
        drawableDataGreen[2] = getResources().getDrawable(R.drawable.bottom_bar_left_modern_green);
        drawableDataBlue[0] = getResources().getDrawable(R.drawable.button_background_not_pressed_modern_blue);
        drawableDataBlue[1] = getResources().getDrawable(R.drawable.bottom_bar_middle_modern_blue);
        drawableDataBlue[2] = getResources().getDrawable(R.drawable.bottom_bar_left_modern_blue);

        themeColors = ThemeColor.values();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().getDecorView().setBackgroundColor(themeColors[sharedPreferences.getInt("theme", 0)].backgroundColor);
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
        // Background color, foreground color, text color, color for 9PatchDraw images
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

    // Interface for communication between subscriber and main thread
    public interface SubscriberMessageListener {
        public void onNewMessage(List<JoyFeedback> message);
    }

    public interface SensorCalibrationListener {
        public void onCalibrationSuccess();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.overflow_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Remove item that starts the settings menu to prevent changes during robot control
        if(titleName.equals(getResources().getString(R.string.title_activity_execute))) menu.removeItem(R.id.popup_properties);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case(R.id.popup_properties):
                Intent settingsIntent = new Intent(this, Settings.class);
                startActivity(settingsIntent);
                break;
            case(R.id.popup_help):
                Intent HelpIntent = new Intent(this, Help.class);
                startActivity(HelpIntent);
                break;
            case(R.id.popup_about):
                Intent AboutIntent = new Intent(this, About.class);
                startActivity(AboutIntent);
                break;
        }
        return true;
    }

    public SharedPreferences getSharedPreferences(){
        return sharedPreferences;
    }

    public void initActionBar() {
        bar = getSupportActionBar();
        // Set action bar parameters
        bar.setDisplayUseLogoEnabled(false);
        bar.setDisplayHomeAsUpEnabled(false);
        bar.setBackgroundDrawable(new ColorDrawable(themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor));
        // Prevent that actionBar show grey color
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayShowTitleEnabled(true);
        bar.setTitle(titleName);
    }

    public void setActionBarTitle(String titleRes){
        this.titleName = titleRes;
    }

    public BaseClass.ThemeColor[] getThemeColors(){
        return themeColors;
    }

}
