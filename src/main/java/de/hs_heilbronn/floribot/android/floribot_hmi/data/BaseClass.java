package de.hs_heilbronn.floribot.android.floribot_hmi.data;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import de.hs_heilbronn.floribot.android.floribot_hmi.R;

/**
 * Created by mr on 12.05.14.
 *
 * This class manages global methods like option menu
 */
public class BaseClass extends FragmentActivity implements PopupMenu.OnMenuItemClickListener {

    private SharedPreferences sharedPreferences;
    private DataSet dataSet;
    private DataSet.ThemeColor[] themeColors;
    private Dialog dialog;
    private ThemeManager themeManager;
    private Window dialogWindow;
    private int currentTheme;
    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;
    private EditText editTextNodeGraphName;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Disable activity title
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Initialize some dta stuff (e.g. theme color object, etc.)
        dataSet = new DataSet(this);
        themeColors = DataSet.ThemeColor.values();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Set Interface for theme management
        try {
            themeManager = (ThemeManager) this;
        }catch(ClassCastException e){
            Log.i("@BaseClass->onCreate", "Should include interface for theme management.");
        }

        currentTheme = sharedPreferences.getInt("theme", 0);
        // Set dialog layout and style
        dialog = new Dialog(this, R.style.dialog_style);
        dialogWindow = dialog.getWindow();

        setDialogTitleStyle();
    }

    public DataSet.ThemeColor[] getThemeColors(){
        return themeColors;
    }

    public DataSet getDataSet(){
        return dataSet;
    }

    public int getCurrentTheme(){
        return currentTheme;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch(menuItem.getItemId()){
            case(R.id.popup_properties):
                overflowDialog(getResources().getString(R.string.title_property), R.layout.layout_property);
                break;
            case(R.id.popup_help):
                overflowDialog(getResources().getString(R.string.title_help), R.layout.layout_help);
                break;
            case(R.id.popup_about):
                overflowDialog(getResources().getString(R.string.title_about), R.layout.layout_about);
                break;
        }
        return true;
    }

    public void overflowButtonClicked(View view){
        //Showing popup menu
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.overflow_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);

        // Remove properties menu item to prevent unexpected behaviour
        if(this.getLocalClassName().equals("ExecuteActivity")){
            Menu m = popup.getMenu();
            m.removeItem(R.id.popup_properties);
        }

        popup.show();
    }

    public void overflowDialog(String title, int layout){
        dialog.setContentView(layout);

        if(layout == R.layout.layout_property){
            // Get and set edit text field for node graph name
            editTextNodeGraphName = (EditText) dialog.findViewById(R.id.editText_node_graph_name);
            editTextNodeGraphName.setText(sharedPreferences.getString(getResources().getString(R.string.nodeGraphName), ""));
            editTextNodeGraphName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    // Save node graph name from edit text field
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(getResources().getString(R.string.nodeGraphName), editTextNodeGraphName.getText().toString());
                    editor.commit();
                }
            });

            // Set spinner for theme color
            Spinner spinner_one = (Spinner) dialog.findViewById(R.id.spinner_one);
            spinner_one.setAdapter(new SpinnerAdapter(this, getResources().getStringArray(R.array.theme_items)));
            spinner_one.setSelection(currentTheme);

            spinner_one.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    currentTheme = position;
                    themeManager.themeCallback(currentTheme);
                    setDialogTitleStyle();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
        setDialogTitleStyle();
        dialog.setTitle(title);
        dialog.show();
    }

    private void setDialogTitleStyle() {
        dialogWindow.setBackgroundDrawable(new ColorDrawable(themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor));
        dialogWindow.setTitleColor(themeColors[sharedPreferences.getInt("theme", 0)].textColor);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialogWindow.setAttributes(layoutParams);
    }

    public interface ThemeManager{
        public void themeCallback(int current_theme);
    }

    public class SpinnerAdapter extends BaseAdapter {
        private String[] objects;

        public SpinnerAdapter(Context context, String[] objects) {
            this.objects = objects;
        }

        @Override
        public int getCount() {
            return objects.length;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, parent);
        }

        private View getCustomView(int position, ViewGroup parent) {

            LayoutInflater inflater = getLayoutInflater();
            View convertView = inflater.inflate(R.layout.layout_spinner_item, parent, false);

            //convertView.setBackgroundResource(R.drawable.spinner_view_state);
            TextView main_text = (TextView) convertView.findViewById(R.id.textView_spinner_item);
            main_text.setText(objects[position]);

            return convertView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            return getCustomView(position, parent);
        }
    }

    public SharedPreferences getSharedPreferences(){
        return sharedPreferences;
    }

}
