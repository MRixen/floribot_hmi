package de.hs_heilbronn.floribot.android.floribot_hmi;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;

public class SettingsActivity extends BaseClass implements AdapterView.OnItemSelectedListener{

    private String[] array;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_properties);

       array = getResources().getStringArray(R.array.screen_orientation_entries);

        Spinner spinner = (Spinner) findViewById(R.id.properties_spinner);
        spinner.setAdapter(new MySpinnerAdapter(this,array));
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    private class MySpinnerAdapter extends BaseAdapter{
        private LayoutInflater mInflater;

        public MySpinnerAdapter(Context context, String[] objects){
            mInflater = LayoutInflater.from(context);

        }

        @Override
        public int getCount() {
            return array.length;
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
            main_text.setText(array[position]);

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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d("@SettingsActivity#onItemSelected: ", "pos = " + position);
        Log.d("@SettingsActivity#onItemSelected: ", "id = " + id);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.d("@SettingsActivity#onNothingSelected: ", "nothing selected...");
    }

}

