package de.hs_heilbronn.floribot.android.floribot_hmi.data;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import de.hs_heilbronn.floribot.android.floribot_hmi.AboutActivity;
import de.hs_heilbronn.floribot.android.floribot_hmi.HelpActivity;
import de.hs_heilbronn.floribot.android.floribot_hmi.R;
import de.hs_heilbronn.floribot.android.floribot_hmi.SettingsActivity;

/**
 * Created by mr on 12.05.14.
 */
public class BaseClass extends FragmentActivity implements PopupMenu.OnMenuItemClickListener{

    public ActionBar bar;
    private PopupMenu popup;
    private ActivityInfo info;
    private Intent helpIntent, aboutIntent, propIntent;
    private TextView titleView;
    private String theme;
    private SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        /// Disable activity title
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Create instance of PopupMenu and inflate it using xml file
        ImageButton button_property = (ImageButton) findViewById(R.id.properties_button);
        /*popup = new PopupMenu(this, button_property);
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);*/

        // Create intents for info activities (e.g. help, about,, properties)
        helpIntent = new Intent(this, HelpActivity.class);
        aboutIntent = new Intent(this, AboutActivity.class);
        propIntent = new Intent(this, SettingsActivity.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        // Start info activities (e.g. help, about,, properties)
        switch(menuItem.getItemId()){
            case(R.id.popup_properties):
                    //startActivity(propIntent);

                break;
            case(R.id.popup_help):
                propertyDialog(getResources().getString(R.string.title_activity_help), getResources().getString(R.string.helpText));
                break;
            case(R.id.popup_about):
                propertyDialog(getResources().getString(R.string.title_activity_about), getResources().getString(R.string.aboutText));
                break;
        }
        return true;
    }

    public void propertyButtonClicked(View view){
        //showing popup menu
        popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    public void propertyDialog(String title, String content) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.layout_property_dialog);
        dialog.setTitle(title);

        TextView content_description = (TextView) dialog.findViewById(R.id.dialog_content_description);
        content_description.setText(content);
        dialog.show();
    }
}
