package de.hs_heilbronn.floribot.android.floribot_hmi.gui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;

/**
 * Created by mr on 09.05.14.
 */
public class SurfaceViewExecuteActivity extends SurfaceView implements Runnable {

    private SurfaceHolder holder;
    private Thread thread;

    private Handler loopHandler1;
    private Context context;
    private float rectRight, rectLeft, rectTop, rectBottom;
    private Paint paintForeground, paintBackground, paintBottom;

    private DataSet mDataSet;
    private Path bottomBar = new Path();
    private Canvas canvas;

    public SurfaceViewExecuteActivity(Context context, SurfaceHolder holder) {
        super(context);

        this.context = context;
        this.holder = holder;
        thread = null;

        init();

        paintBottom = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBottom.setColor(Color.parseColor("#0071ff"));
        paintBottom.setStyle(Paint.Style.FILL);


        holder.setFormat(PixelFormat.TRANSPARENT);
    }

    public void init(){
        paintForeground = new Paint();
        paintBackground = new Paint();
        paintForeground.setColor(Color.GREEN);
        paintBackground.setColor(Color.WHITE);

        // Calculate display size
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        float pxWidth = displayMetrics.widthPixels;
        float pxHeight = displayMetrics.heightPixels ;
        float dpWidth = pxWidth / displayMetrics.density;
        float dpHeight = pxHeight / displayMetrics.density;

        // ----- Create path for bottom bar -----
        // Calculate top and bottom bar height dp to px
        float topBarHeightInDp = 50;
        float topBarHeight = (pxHeight / dpHeight) * topBarHeightInDp;
        float bottomBarHeight = (pxHeight / dpHeight) * DataSet.bottomBarHeightInDp;
        float bottomBarWidthInDp = 400;
        float bottomBarWidth = (pxWidth / dpWidth) *  bottomBarWidthInDp;

        PointF pointTopLeft = new PointF();
        pointTopLeft.x = pxWidth -bottomBarWidth;
        pointTopLeft.y = pxHeight - bottomBarHeight - topBarHeight;

        PointF pointTopRight = new PointF();
        pointTopRight.x = pxWidth;
        pointTopRight.y = pointTopLeft.y;

        PointF pointBottomLeft = new PointF();
        pointBottomLeft.x = (float) (pxWidth - bottomBarWidth - bottomBarHeight*Math.tan(Math.PI / 6));
        pointBottomLeft.y = pxHeight;

        PointF pointBottomRight = new PointF();
        pointBottomRight.x = pxWidth;
        pointBottomRight.y = pxHeight;


        PointF[] bottomBarPoints = new PointF[4];
        bottomBarPoints[0] = pointTopLeft;
        bottomBarPoints[1] = pointTopRight;
        bottomBarPoints[2] = pointBottomRight;
        bottomBarPoints[3] = pointBottomLeft;

        bottomBar.setFillType(Path.FillType.EVEN_ODD);
        bottomBar.moveTo(bottomBarPoints[0].x, bottomBarPoints[0].y);
        for(int i=0;i<=3;i++) {
            bottomBar.lineTo(bottomBarPoints[i].x, bottomBarPoints[i].y);
        }
        bottomBar.close();
        // ---------------


        float marginLeft = 15;
        float rectWidth = 40;
        float marginTop = 25;
        float marginBottom = 90;


        rectRight = (pxWidth / dpWidth) * (rectWidth + marginLeft);
        rectLeft = (pxWidth / dpWidth) * marginLeft;
        //rectTop = (pxHeight / dpHeight) * marginTop;
        rectBottom = (pxHeight / dpHeight) * (dpHeight - DataSet.bottomBarHeightInDp - 50 - marginBottom);
        rectTop = rectBottom;

    }

    @Override
    public void run() {
        while (!holder.getSurface().isValid()) {
            // SET A TIME LIMIT!
            Log.d("@SurfaceViewExecuteActivity#Run: ", "is surface valid?");
        }

        Looper.prepare();

        loopHandler1 = new Handler();

        canvas = holder.lockCanvas();
        drawBottomBar();
        holder.unlockCanvasAndPost(canvas);

        // Handler to receive sensor data and visualize it
        DataSet.handlerForVisualization = new Handler() {
            public void handleMessage(Message msg) {
                Log.d("@SurfaceViewExecuteActivity#Handler: ", "inside Handler");

                Bundle bundle = msg.getData();

                if (bundle != null) {
                    float[] axesData = bundle.getFloatArray("axesData");

                    if (axesData != null) {
                        canvas = holder.lockCanvas();
                        drawBottomBar();
                        drawAccVisualization(axesData[2]);
                        holder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        };


        Looper.loop();
    }

    public void drawBottomBar() {

        Log.d("@drawBottonBar", "inside..");
        canvas.drawColor(Color.parseColor("#fffef2"));
        canvas.drawColor(Color.parseColor("#fffef2"));
        canvas.drawPath(bottomBar, paintBottom);
    }

    public void drawAccVisualization(float height) {

        // Draw white background
        canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, paintBackground);
        // Draw velocity visualization
        canvas.drawRect(rectLeft, rectTop - height*10, rectRight, rectBottom, paintForeground);


    }

    public void pause(){
        loopHandler1.getLooper().quit();
        while(true){
            try {
                // Blocks thread until the operation were finished and dies
                thread.join();
            }catch(Exception e){
                Log.d("@SurfaceViewExecuteActivity#Pause: ", String.valueOf(e));
            }
            break;
        }
        Log.d("@pause: ", "inside...");
        thread = null;
    }

    public void resume() {
        if (thread == null) {
            thread = new Thread(this);
            Log.d("@SurfaceViewExecuteActivity#Resume: ", "inside...");
            thread.start();
        }

    }
}
