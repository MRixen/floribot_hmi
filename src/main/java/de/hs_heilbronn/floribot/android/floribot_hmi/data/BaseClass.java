package de.hs_heilbronn.floribot.android.floribot_hmi.data;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import de.hs_heilbronn.floribot.android.floribot_hmi.R;
import de.hs_heilbronn.floribot.android.floribot_hmi.SettingsActivity;
import de.hs_heilbronn.floribot.android.floribot_hmi.AboutActivity;
import de.hs_heilbronn.floribot.android.floribot_hmi.HelpActivity;

/**
 * Created by mr on 12.05.14.
 */
public class BaseClass extends ActionBarActivity implements PopupMenu.OnMenuItemClickListener{

    public ActionBar bar;
    private PopupMenu popup;
    private ActivityInfo info;
    private Intent helpIntent, aboutIntent, propIntent;
    private TextView titleView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Disable status bar menu for sdk < 4.1
        if(Build.VERSION.SDK_INT < 16){
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // Get action par object and set parameters
        bar = getSupportActionBar();
        bar.setDisplayUseLogoEnabled(false);
        bar.setDisplayHomeAsUpEnabled(false);
        bar.setDisplayShowCustomEnabled(true);
        bar.setDisplayShowHomeEnabled(false);
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#0071ff")));

        // Check this, it not set the color correctly
        //bar.setBackgroundDrawable(new ColorDrawable(R.color.ModernBlue));


        // Inflate action bar with custom views and show it
        ActionBar.LayoutParams lp1 = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        View customNav = LayoutInflater.from(this).inflate(R.layout.actionbar, null);
        bar.setCustomView(customNav, lp1);

        // Set action bar title
        PackageManager packageManager = this.getPackageManager();
        try {
            info = packageManager.getActivityInfo(this.getComponentName(), 0);
            titleView = (TextView) findViewById(R.id.my_action_bar_title);
            titleView.setText(getString(info.labelRes));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //Create instance of PopupMenu and inflate it using xml file
        ImageButton button1 = (ImageButton) findViewById(R.id.button1);
        popup = new PopupMenu(this, button1);
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);

        // Create intents for info activities (e.g. help, about,, properties)
        helpIntent = new Intent(this, HelpActivity.class);
        aboutIntent = new Intent(this, AboutActivity.class);
        propIntent = new Intent(this, SettingsActivity.class);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        // Start info activities (e.g. help, about,, properties)
        switch(menuItem.getItemId()){
            case(R.id.popup_properties):
                startActivity(propIntent);
                break;
            case(R.id.popup_help):
                    startActivity(helpIntent);
                break;
            case(R.id.popup_about):
                    startActivity(aboutIntent);
                break;
        }
        return true;
    }

    public void myButtonOnClick(View view){
        //showing popup menu
        popup.show();
    }
}
