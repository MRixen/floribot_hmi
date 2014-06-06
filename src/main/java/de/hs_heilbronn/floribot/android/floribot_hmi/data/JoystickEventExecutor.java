package de.hs_heilbronn.floribot.android.floribot_hmi.data;

import android.content.Context;

import de.hs_heilbronn.floribot.android.floribot_hmi.R;

/**
 * Created by mr on 30.05.14.
 *
 * This class provides acceleration data for control with joystick buttons
 */
public class JoystickEventExecutor extends Thread {

    private final Context context;

    public JoystickEventExecutor(Context context) {
        this.context = context;
    }

    public void setJoystickEventListener(JoystickEventListener joystickEventListener) {
        DataSet.joystickEventListener = joystickEventListener;
    }

    public void run() {
        while (DataSet.isRunning) {
            if (DataSet.joystickEventListener != null) {
                DataSet.joystickEventListener.joystickEvent();
            }
            try {
                Thread.sleep(context.getResources().getInteger(R.integer.joystick_delay));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public interface JoystickEventListener {
        public void joystickEvent();
    }
}
