package de.hs_heilbronn.floribot.android.floribot_hmi.communication;

import android.content.Context;


public class MyCustomEvent{

    private final Context context;
    private MyCustomEventListener myCustomEventListener;

    public MyCustomEvent(Context context) {
        this.context = context;
    }

    public void setMyCustomEventListener(MyCustomEventListener customEventListener) {
        this.myCustomEventListener = customEventListener;
    }

    public interface MyCustomEventListener {
        public void myCustomEvent(int mode);
    }

    public MyCustomEventListener getMyCustomEventListener(){
        return myCustomEventListener;
    }
}
