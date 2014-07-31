package de.hs_heilbronn.floribot.android.floribot_hmi.data;

import android.content.Context;
import android.util.Log;

import de.hs_heilbronn.floribot.android.floribot_hmi.R;

/**
 * Created by mr on 30.05.14.
 *
 * This class provides acceleration data for control with joystick buttons
 */
public class AccelerationEvent extends Thread{

    private final Context context;
    private boolean isRunning;
    private AccEventListener accEventListener;

    public AccelerationEvent(Context context) {
        this.context = context;
        isRunning = true;
    }

    public void registerAccEventListener(AccEventListener accEventListener) {
        this.accEventListener = accEventListener;
    }

    public void run() {
        Log.d("@AccelerationEvent->run", "AccelerationEvent thread started");
        while (true) {
            if (this.accEventListener != null) {
                this.accEventListener.accEvent();
            }
            try {
                Thread.sleep(context.getResources().getInteger(R.integer.custom_event_delay));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(!isRunning) break;
        }
    }

    public void unregisterAccEventListener(AccEventListener accEventListener) {
        this.accEventListener = accEventListener;
        this.isRunning = false;
    }


    public interface AccEventListener {
        public void accEvent();
    }

    public AccEventListener getEventListener(){
        return accEventListener;
    }
}
