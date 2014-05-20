package de.hs_heilbronn.floribot.android.floribot_hmi.gui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;

import de.hs_heilbronn.floribot.android.floribot_hmi.MainActivity;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;

import static java.lang.Math.abs;

/**
 * Created by mr on 09.05.14.
 */
public class MySurfaceView {

    private SurfaceHolder holder;
    private Canvas canvas;
    public Thread tt;
    private Handler loopHandler1;
    private Context context;
    private float rectRight, rectLeft, rectTop, rectBottom;
    private Paint paintForeground, paintBackground;

    public void startDrawThread(SurfaceHolder holder, Context context) {

        this.holder = holder;
        this.context = context;
        init();
        // Start thread, if no thread is alive
        if (tt == null) {
            Log.d("@MySurfaceView#startDrawThread: ","Start draw thread...");
            tt = new DrawThread();
            tt.start();
        }
    }

    public void stopDrawThread(){
        // Stop loop
        try {
            loopHandler1.getLooper().quit();
        }catch(Exception e){
            Log.d("@MySurfaceView#stopDrawThread#StopHandlerException: ", String.valueOf(e));
        }
        // Stop thread
        try {
            while (tt.isAlive()) {
            }
            tt = null;
        }catch(Exception e){
            Log.d("@MySurfaceView#stopDrawThread#StopThreadException: ", String.valueOf(e));
        }
    }

    public void init(){
        paintForeground = new Paint();
        paintBackground = new Paint();
        paintForeground.setColor(Color.GREEN);
        paintBackground.setColor(Color.WHITE);

        MainActivity.threadInterruption = true;
        holder.setFormat(PixelFormat.TRANSPARENT);

        // Calculate display size
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        float pxWidth = displayMetrics.widthPixels;
        float pxHeight = displayMetrics.heightPixels ;
        float dpWidth = pxWidth / displayMetrics.density;
        float dpHeight = pxHeight / displayMetrics.density;


        float marginLeft = 15;
        float rectWidth = 40;
        float marginTop = 25;
        float marginBottom = 25;

        /*rectRight = (pxWidth / dpWidth) * (dpWidth - marginRight);
        rectLeft = (pxWidth / dpWidth) * (dpWidth - marginRight - rectWidth);
        rectTop = (pxHeight / dpHeight) * marginTop;
        rectBottom = (pxHeight / dpHeight) * (dpHeight - DataSet.bottomBarHeightInDp - 50 - marginBottom);*/
        rectRight = (pxWidth / dpWidth) * (rectWidth + marginLeft);
        rectLeft = (pxWidth / dpWidth) * marginLeft;
        rectTop = (pxHeight / dpHeight) * marginTop;
        rectBottom = (pxHeight / dpHeight) * (dpHeight - DataSet.bottomBarHeightInDp - 50 - marginBottom);
    }


    public class DrawThread extends Thread {
        @Override
        public void run() {

            Looper.prepare();
            loopHandler1 = new Handler();

            // Wait until surface is initialized
            while (!holder.getSurface().isValid()) {
                // SET A TIME LIMIT!
            }

            // Handler to receive sensor data and visualize it


            DataSet.handlerForVisualization = new Handler() {
                public void handleMessage(Message msg) {

                    Bundle bundle = msg.getData();

                    if (bundle != null) {
                        float[] axesData = bundle.getFloatArray("axesData");

                        if (axesData != null) {
                            canvas = holder.lockCanvas();
                            draw(axesData[2]);
                            holder.unlockCanvasAndPost(canvas);
                            Log.d("@MySurfaceView#Run", "axesData[0] = " + axesData[2]);
                        }
                    }
                }
            };
            Looper.loop();
        }
    }

    public void draw(float height) {

        //Log.d("Thread in draw: ", Thread.currentThread().getName());
        // Draw white background
        canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, paintBackground);
        // Draw velocity visualization
        canvas.drawRect(rectLeft, rectTop + abs(height)*10, rectRight, rectBottom, paintForeground);


    }
}
