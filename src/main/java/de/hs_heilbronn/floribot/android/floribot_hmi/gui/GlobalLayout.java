package de.hs_heilbronn.floribot.android.floribot_hmi.gui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import de.hs_heilbronn.floribot.android.floribot_hmi.R;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;

/**
 * Created by mr on 20.05.14.
 *
 */
public class GlobalLayout extends android.view.SurfaceView implements Runnable{

    private SharedPreferences sharedPreferences;
    private Context context;
    private Thread drawThread;
    private Handler handlerForDrawThread;
    private SurfaceHolder holder;
    private Canvas canvas;
    private Paint svPaint, svbPaint;
    private int backgroundColor;
    private float[] svRectArray = {0,0,0,0,0,0,0,0,0,0,0,0};
    private float svHalfSizeTopBeam, svHalSizeLeftBeam;

    public GlobalLayout(Context context) {
        super(context);
        this.context = context;
        drawThread = null;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void init(Bundle bundle) {
        // Load draw array for sensor visualization
        if (bundle.containsKey(context.getResources().getString(R.string.svArray))) svRectArray = bundle.getFloatArray(context.getResources().getString(R.string.svArray));

        svPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        svPaint.setStyle(Paint.Style.FILL);
        svPaint.setColor(context.getResources().getColor(R.color.ModernOrange));
        svbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        svbPaint.setStyle(Paint.Style.FILL);
        svbPaint.setColor(context.getResources().getColor(R.color.GreyLight));
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
        handlerForDrawThread = new Handler();

        try {
            canvas = holder.lockCanvas();
            drawLayout(0,0);
            holder.unlockCanvasAndPost(canvas);
        }
        catch(Exception e){
            Log.e("@GlobalLayout#run: ", "Exception: " + e);
        }

        // Handler to receive and visualize sensor data
        BaseClass.handlerForVisualization = new Handler() {
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();

                float[] axesData;

                if (bundle != null) {
                    if(bundle.containsKey(context.getResources().getString(R.string.axes_state_array))) {
                        axesData = bundle.getFloatArray(context.getResources().getString(R.string.axes_state_array));

                        if (axesData != null) {
                            try {
                                canvas = holder.lockCanvas();
                                drawLayout(axesData[1], axesData[0]);
                                holder.unlockCanvasAndPost(canvas);
                            } catch (Exception e) {
                                Log.e("@GlobalLayout#handleMessage: ", "Exception: " + e);
                            }
                        }
                    }
                }
            }
        };
        Looper.loop();
    }

    public void drawLayout(float translation, float rotation){
        Log.d("@GlobalLayout->drawLayout", Thread.currentThread().getName());
        canvas.drawColor(backgroundColor);
        float factor = svHalfSizeTopBeam/10;

        // Draw sensor visualization for top beam
        canvas.drawRect(svRectArray[0], svRectArray[1], svRectArray[2], svRectArray[3], svbPaint); // For grey beam background
        canvas.drawRect(svRectArray[0] + svHalfSizeTopBeam + rotation*factor, svRectArray[1], svRectArray[2] - svHalfSizeTopBeam, svRectArray[3], svPaint);
        // Draw sensor visualization for left beam
        canvas.drawRect(svRectArray[4], svRectArray[5], svRectArray[6], svRectArray[7], svbPaint); // For grey beam background
        canvas.drawRect(svRectArray[4], svRectArray[5] + svHalSizeLeftBeam - translation*10, svRectArray[6], svRectArray[7] - svHalSizeLeftBeam, svPaint);
    }


    public void pause(){
            try {
                handlerForDrawThread.getLooper().quit();
                handlerForDrawThread = null;
                // Blocks drawThread until all operations are finished
                drawThread.join();
            }catch(Exception e){
                Log.d("@GlobalLayout#pause: ", String.valueOf(e));
            }
        drawThread = null;
    }

    public void setGlobalLayout(Bundle bundle, SurfaceView surface) {
        // Stop old drawThread to provide new theme settings by chang
        pause();

        // Set holder
        surface.setZOrderOnTop(false);
        holder = surface.getHolder();

        // Load color from settings
        BaseClass.ThemeColor[] themeColors = BaseClass.ThemeColor.values();
        this.backgroundColor = themeColors[sharedPreferences.getInt("theme", 0)].backgroundColor;

        init(bundle);

        // Stop last draw drawThread and execute a new one
        if (drawThread == null) {
            drawThread = new Thread(this);
            drawThread.start();
        }
    }
}
