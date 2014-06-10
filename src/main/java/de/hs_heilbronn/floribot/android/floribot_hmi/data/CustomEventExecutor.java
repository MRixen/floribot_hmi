package de.hs_heilbronn.floribot.android.floribot_hmi.data;

import android.content.Context;

import de.hs_heilbronn.floribot.android.floribot_hmi.R;

/**
 * Created by mr on 30.05.14.
 *
 * This class provides acceleration data for control with joystick buttons
 */
public class CustomEventExecutor extends Thread {

    private final Context context;

    public CustomEventExecutor(Context context) {
        this.context = context;
    }

    public void setCustomEventListener(CustomEventListener customEventListener) {
        DataSet.customEventListener = customEventListener;
    }

    public void run() {
        while (DataSet.isRunning) {
            if (DataSet.customEventListener != null) {
                DataSet.customEventListener.customEvent();
            }
            try {
                Thread.sleep(context.getResources().getInteger(R.integer.custom_event_delay));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public interface CustomEventListener {
        public void customEvent();
    }
}
