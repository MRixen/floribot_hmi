package de.hs_heilbronn.floribot.android.floribot_hmi.gui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by mr on 22.05.14.
 */
public class LayoutManager extends Fragment {

    private final int layout;
    LayoutManagerInterface layoutManagerInterface;

    // Callback method to signal that layout is valid
    public interface LayoutManagerInterface{
        public void callbackFragment(boolean flag, int layout);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try{
            layoutManagerInterface = (LayoutManagerInterface) activity;
        }catch(ClassCastException e){
            throw new ClassCastException((activity.toString() + " must implement LayoutManagerInterface"));
        }
    }

    public LayoutManager(int layout) {
        this.layout = layout;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(layout, container, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        layoutManagerInterface.callbackFragment(true, this.layout);
    }
}
