package de.hs_heilbronn.floribot.android.floribot_hmi.data;

import android.content.Context;
import android.util.Log;

import de.hs_heilbronn.floribot.android.floribot_hmi.R;

/**
 * Created by mr on 30.05.14.
 *
 * This class provides acceleration data for control with joystick buttons
 */
public class CustomEventExecutor extends Thread {

    private final Context context;
    private boolean isRunning;
    private CustomEventListener customEventListener;

    public CustomEventExecutor(Context context) {
        this.context = context;
    }

    public void setCustomEventListener(CustomEventListener customEventListener) {
        this.customEventListener = customEventListener;
    }

    public void run() {
        while (true) {
            if (this.customEventListener != null) {
                this.customEventListener.customEvent();
            }
            try {
                Thread.sleep(context.getResources().getInteger(R.integer.custom_event_delay));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(!isRunning) break;
        }
    }

    public interface CustomEventListener {
        public void customEvent();
    }

    public boolean getFlag(){
        return isRunning;
    }

    public void setFlag(boolean flag){
        this.isRunning = flag;
        Log.d("@CustomEventExecutor->setFlag", String.valueOf(flag));
    }

}
