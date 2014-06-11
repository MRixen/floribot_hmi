package de.hs_heilbronn.floribot.android.floribot_hmi.gui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import de.hs_heilbronn.floribot.android.floribot_hmi.MainActivity;
import de.hs_heilbronn.floribot.android.floribot_hmi.R;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;

/**
 * Created by mr on 20.05.14.
 *
 * With this class the global layout (surface view for bottom bar, top bar, etc.) is set
 */
public class GlobalLayout extends android.view.SurfaceView implements Runnable{

    private View currentView;
    private  MainActivity mainActivity;
    private SharedPreferences sharedPreferences;
    private  Context context;

    private Thread thread;
    private Handler loopHandler1;

    private SurfaceHolder holder;
    private Canvas canvas;
    private Paint glPaint, svPaint, svPaintDebug, svbPaint;
    private Path[] glPathArray;

    private int backgroundColor, foregroundColor;
    private float[] svRectArray = {0,0,0,0,0,0,0,0,0,0,0,0};
    private float svHalfSizeTopBeam, svHalSizeLeftBeam;

/*
    public GlobalLayout(Context context, SurfaceHolder holder) {
        super(context);

        this.context = context;
        this.holder = holder;
        thread = null;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
*/

    public GlobalLayout(Context context) {
        super(context);
        this.context = context;

        thread = null;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void init(Bundle bundle) {
        // Load draw array for sensor visualization
        if (bundle.containsKey(context.getResources().getString(R.string.svArray))) svRectArray = bundle.getFloatArray(context.getResources().getString(R.string.svArray));

        // Create paths for global layout
        float[] glPointArray = bundle.getFloatArray(context.getResources().getString(R.string.glPointArray));
        int count = bundle.getInt(context.getResources().getString(R.string.glPathQuantity));

        glPathArray = new Path[count];
        int i = 0;

        for (int j = 0; j <= count - 1; j++) {
            glPathArray[j] = new Path();
            glPathArray[j].setFillType(Path.FillType.EVEN_ODD);
            glPathArray[j].moveTo(glPointArray[i], glPointArray[i + 1]);

            while (glPointArray[i] != -1) {
                glPathArray[j].lineTo(glPointArray[i], glPointArray[i + 1]);
                i += 2;
            }
            i++;
            glPathArray[j].close();
        }

        // Name convention: gl = GlobalLayout, sv = SensorVisualization, svb = SensorVisualizationBackground
        glPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glPaint.setStyle(Paint.Style.FILL);
        glPaint.setColor(foregroundColor);
        svPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        svPaint.setStyle(Paint.Style.FILL);
        svPaint.setColor(context.getResources().getColor(R.color.ModernOrange));
        svbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        svbPaint.setStyle(Paint.Style.FILL);
        svbPaint.setColor(context.getResources().getColor(R.color.GreyLight));

        // -------------------------------------
        // ONLY FOR DEBUG !!!
        // -> For camera preview
        // -------------------------------------
        svPaintDebug = new Paint(Paint.ANTI_ALIAS_FLAG);
        svPaintDebug.setStyle(Paint.Style.STROKE);
        svPaintDebug.setStrokeWidth(5);
        svPaintDebug.setColor(context.getResources().getColor(R.color.GreyLight));
        // -------------------------------------

        holder.setFormat(PixelFormat.TRANSPARENT);

        svHalfSizeTopBeam = ((svRectArray[2] - svRectArray[0]) / 2);
        svHalSizeLeftBeam = ((svRectArray[7] - svRectArray[5]) / 2);
    }

    @Override
    public void run() {
        long timeElapsed;
        long startTime = System.nanoTime();

        while (!holder.getSurface().isValid()) {
            timeElapsed = (System.nanoTime() - startTime) / context.getResources().getInteger(R.integer.DIVIDER);
            if (timeElapsed >= context.getResources().getInteger(R.integer.deadTimeSurfaceValidation)) {
                break;
            }
        }

        Looper.prepare();
        loopHandler1 = new Handler();

        try {
            canvas = holder.lockCanvas();
            drawLayout(0,0);
            holder.unlockCanvasAndPost(canvas);
        }
        catch(Exception e){
            Log.e("@GlobalLayout#run: ", "Exception: " + e);
        }

        // Handler to receive and visualize sensor data
        DataSet.handlerForVisualization = new Handler() {
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();

                if (bundle != null) {
                    float[] axesData = bundle.getFloatArray(context.getResources().getString(R.string.axes_state_array));

                    if (axesData != null) {
                        try {
                            canvas = holder.lockCanvas();
                            drawLayout(axesData[1], axesData[2]);
                            holder.unlockCanvasAndPost(canvas);
                        }
                        catch(Exception e){
                            Log.e("@GlobalLayout#handleMessage: ", "Exception: " + e);
                        }
                    }
                }
            }
        };
        Looper.loop();
    }

    public void drawLayout(float rotation, float translation){
        canvas.drawColor(backgroundColor);
        // draw all paths
        for(int i=0;i<= glPathArray.length-1;i++){
            canvas.drawPath(glPathArray[i], glPaint);
        }
        float factor = svHalfSizeTopBeam/10;

        // Draw sensor visualization for top beam
        canvas.drawRect(svRectArray[0], svRectArray[1], svRectArray[2], svRectArray[3], svbPaint); // For grey beam background
        canvas.drawRect(svRectArray[0] + svHalfSizeTopBeam - rotation*factor, svRectArray[1], svRectArray[2] - svHalfSizeTopBeam, svRectArray[3], svPaint);
        // Draw sensor visualization for left beam
        canvas.drawRect(svRectArray[4], svRectArray[5], svRectArray[6], svRectArray[7], svbPaint); // For grey beam background
        canvas.drawRect(svRectArray[4], svRectArray[5] + svHalSizeLeftBeam - translation*10, svRectArray[6], svRectArray[7] - svHalSizeLeftBeam, svPaint);
        // -------------------------------------
        // ONLY FOR DEBUG !!!
        // -> For pseudo camera preview
        // -------------------------------------
        canvas.drawRect(svRectArray[8], svRectArray[9], svRectArray[10], svRectArray[11], svPaintDebug);
        // -------------------------------------
    }


    public void pause(){

            try {
                loopHandler1.getLooper().quit();
                // Blocks thread until all operations are finished
                thread.join();
            }catch(Exception e){
                Log.d("@GlobalLayout#pause: ", String.valueOf(e));
            }
        thread = null;
    }

    public void setGlobalLayout(Bundle bundle, SurfaceView surface) {
        // Stop old thread
        pause();

        // Set holder
        surface.setZOrderOnTop(false);
        holder = surface.getHolder();

        // Load color from settings
        DataSet.ThemeColor[] themeColors = DataSet.ThemeColor.values();
        this.backgroundColor = themeColors[sharedPreferences.getInt("theme", 0)].backgroundColor;
        this.foregroundColor = themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor;

        init(bundle);

        // Stop last draw thread and execute a new one
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }
}
