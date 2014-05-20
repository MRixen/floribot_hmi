package de.hs_heilbronn.floribot.android.floribot_hmi;

import android.os.Bundle;
import android.widget.TextView;

import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;

/**
 * Created by mr on 16.05.14.
 */
public class AboutActivity extends BaseClass{

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        textView = (TextView) findViewById(R.id.infoTextField);
        // Set text
        textView.setText(R.string.aboutText);
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}