package de.hs_heilbronn.floribot.android.floribot_hmi.gui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.hs_heilbronn.floribot.android.floribot_hmi.ExecuteActivity;

/**
 * Created by mr on 22.05.14.
 *
 * This class set the fragments for the joystick buttons
 */
public class LocalLayout extends Fragment {

    private ExecuteActivity context;
    LocalLayoutManager layoutManagerInterface;
    private int localLayoutResource;
    private int drawable;


    public LocalLayout(ExecuteActivity context) {
        this.context = context;
    }

    public void setLocalLayout(int frameLayout, int localLayoutResource, int drawable) {
        this.localLayoutResource = localLayoutResource;
        this.drawable = drawable;
        if (this.localLayoutResource != 0) {
            context.getSupportFragmentManager().beginTransaction().replace(frameLayout, this).commit();
            if(layoutManagerInterface != null) layoutManagerInterface.localLayoutCallback(drawable);
        }
        else{
            context.getSupportFragmentManager().beginTransaction().remove(this).commit();
            layoutManagerInterface = null;
        }

    }

    public interface LocalLayoutManager{
        public void localLayoutCallback(int drawable);
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
        return inflater.inflate(localLayoutResource, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        layoutManagerInterface.localLayoutCallback(drawable);
    }
}
