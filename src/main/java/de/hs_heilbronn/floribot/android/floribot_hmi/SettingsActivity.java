package de.hs_heilbronn.floribot.android.floribot_hmi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;

public class SettingsActivity extends BaseClass {

    private String[] screenOrientationArray;
    private String[] themeArray;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_properties);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        screenOrientationArray = getResources().getStringArray(R.array.screen_orientation_entries);
        Spinner screenOrientationSpinner = (Spinner) findViewById(R.id.properties_screen_orientation_spinner);
        screenOrientationSpinner.setAdapter(new MySpinnerAdapter(this,screenOrientationArray));
        screenOrientationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d("@onItemClick", "position: " + position);
                Log.d("@onItemClick", "id: " + id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        // Manage the actual theme
        themeArray = getResources().getStringArray(R.array.theme_entries);
        Spinner themeSpinner = (Spinner) findViewById(R.id.properties_theme_spinner);
        themeSpinner.setAdapter(new MySpinnerAdapter(this,themeArray));
        themeSpinner.setSelection(sharedPreferences.getInt("theme", 0));
        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {


            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch(position){
                    case 0:
                        // Save new theme in shared preferences
                        editor = sharedPreferences.edit();
                        editor.putInt("theme", position);
                        //Log.d("settings1", "position: " + position);
                        //Log.d("settings1", "context: " + getApplicationContext());
                        editor.commit();
                        //Log.d("settings2", "position: " + sharedPreferences.getInt("theme", 0));
                        break;
                    case 1:
                        // Save new theme in shared preferences
                        editor = sharedPreferences.edit();
                        editor.putInt("theme", position);
                        // Log.d("settings1", "position: " + position);
                        //Log.d("settings1", "context: " + getApplicationContext());
                        editor.commit();
                        //Log.d("settings2", "position: " + sharedPreferences.getInt("theme", 0));
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    private class MySpinnerAdapter extends BaseAdapter{
        private LayoutInflater mInflater;
        private String[] objects;

        public MySpinnerAdapter(Context context, String[] objects){
            this.objects = objects;
            mInflater = LayoutInflater.from(context);

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
            return getCustomView(position, convertView, parent);
        }

        private View getCustomView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = getLayoutInflater();
            convertView = inflater.inflate(R.layout.custom_spinner, parent, false);

            TextView main_text = (TextView) convertView.findViewById(R.id.custom_spinner_detail);
            main_text.setText(objects[position]);

            convertView.setBackgroundResource(R.drawable.my_list_selector);

            return convertView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}

