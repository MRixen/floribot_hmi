package de.hs_heilbronn.floribot.android.floribot_hmi.data;

import android.util.Log;

/**
 * Created by mr on 30.05.14.
 */
public class JoystickEventExecutor extends Thread {

    private final int rate = 200;

    public void setJoystickEventListener(JoystickEventListener joystickEventListener) {
        DataSet.joystickEventListener = joystickEventListener;
    }

    public void run() {
      int counter = 0;
        while (DataSet.isRunning) {
            if (DataSet.joystickEventListener != null) {
                DataSet.joystickEventListener.joystickEvent(counter);
                counter++;
                Log.d("@JoystickEventExecutor#run: ", " Thread: " +  Thread.currentThread().getName());
            }
            try {
                Thread.sleep(rate);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public interface JoystickEventListener {
        public void joystickEvent(int count);
    }

}
