package de.hs_heilbronn.floribot.android.floribot_hmi.gui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.hs_heilbronn.floribot.android.floribot_hmi.ControlMenu;

/**
 * Created by mr on 22.05.14.
 *
 * This class set the fragments for the joystick buttons
 */
public class ControlPanel extends Fragment {

    private ControlMenu controlMenu;
    private OnControlPanelListener onControlPanelListener;
    private int layoutResource;
    private int drawable;


    public ControlPanel(ControlMenu controlMenu) {
        this.controlMenu = controlMenu;
    }

    public void setControlPanel(int frameLayout, int layoutResource, int drawable) {
        this.layoutResource = layoutResource;
        this.drawable = drawable;
        if (this.layoutResource != 0) {
            controlMenu.getSupportFragmentManager().beginTransaction().replace(frameLayout, this).commit();
            if(onControlPanelListener != null) onControlPanelListener.onControlPanelLoaded(drawable);
        }
        else{
            controlMenu.getSupportFragmentManager().beginTransaction().remove(this).commit();
            onControlPanelListener = null;
        }

    }

    public interface OnControlPanelListener {
        public void onControlPanelLoaded(int drawable);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try{
            onControlPanelListener = (OnControlPanelListener) activity;
        }catch(ClassCastException e){
            throw new ClassCastException((activity.toString() + " must implement OnControlPanelListener"));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(layoutResource, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        onControlPanelListener.onControlPanelLoaded(drawable);
    }
}
