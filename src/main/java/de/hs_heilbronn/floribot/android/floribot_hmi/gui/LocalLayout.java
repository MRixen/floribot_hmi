package de.hs_heilbronn.floribot.android.floribot_hmi.gui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by mr on 22.05.14.
 *
 * This class set the fragments for the joystick buttons
 */
public class LocalLayout extends Fragment {

    private final int layout;
    LocalLayoutManager layoutManagerInterface;

    public LocalLayout(int layout) {
        this.layout = layout;
    }



    public interface LocalLayoutManager{
        public void localLayoutCallback();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try{
            layoutManagerInterface = (LocalLayoutManager) activity;
        }catch(ClassCastException e){
            throw new ClassCastException((activity.toString() + " must implement LocalLayoutManager"));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(layout, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        layoutManagerInterface.localLayoutCallback();
    }
}
