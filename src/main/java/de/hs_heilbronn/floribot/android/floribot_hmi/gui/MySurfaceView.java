package de.hs_heilbronn.floribot.android.floribot_hmi.gui;

import android.content.Context;
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;

import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;

/**
 * Created by mr on 20.05.14.
 */
public class MySurfaceView extends android.view.SurfaceView implements Runnable{

    private SharedPreferences sharedPreferences;
    private String backgroundColor, foregroundColor;
    private Paint paint, svPaint, svPaintDebug;
    private Context context;
    private PointF[] middleBarPoints = new PointF[4];
    private Path middleBar = new Path();
    private Path bottomBar = new Path();

    private DataSet mDataSet;
    private SurfaceHolder holder;
    private Thread thread;
    private Object myDrawData;
    private Path[] paths;
    private Canvas canvas;
    private Handler loopHandler1;
    private float[] svRectArray = {0,0,0,0,0,0,0,0,0,0,0,0};

    public MySurfaceView(Context context, SurfaceHolder holder) {
        super(context);

        this.context = context;
        this.holder = holder;
        thread = null;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        /*init();*/

    }

    public void init(Bundle bundle) {
        // Load draw array for sensor visualization
        if (bundle.containsKey("svRectArray")) svRectArray = bundle.getFloatArray("svRectArray");

        // Create paths for main layout
        float[] pointsArray = bundle.getFloatArray("pointsArray");
        int count = bundle.getInt("pathCount");

        paths = new Path[count];
        int i = 0;

        for (int j = 0; j <= count - 1; j++) {
            paths[j] = new Path();
            paths[j].setFillType(Path.FillType.EVEN_ODD);
            paths[j].moveTo(pointsArray[i], pointsArray[i + 1]);

            while (pointsArray[i] != -1) {
                paths[j].lineTo(pointsArray[i], pointsArray[i + 1]);
                Log.d("FloatArray: ", "x(" + i + "): " + pointsArray[i] + " and y(" + i + "): " + pointsArray[i + 1]);
                i += 2;
            }
            i++;
            paths[j].close();
        }

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor(foregroundColor));
        svPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        svPaint.setStyle(Paint.Style.FILL);
        svPaint.setColor(Color.GREEN);

        // -------------------------------------
        // ONLY FOR DEBUG !!!
        // -------------------------------------
        svPaintDebug = new Paint(Paint.ANTI_ALIAS_FLAG);
        svPaintDebug.setStyle(Paint.Style.FILL);
        svPaintDebug.setColor(Color.GRAY);
        // -------------------------------------

        holder.setFormat(PixelFormat.TRANSPARENT);


    }

    @Override
    public void run() {

        while (!holder.getSurface().isValid()) {
            // SET A TIME LIMIT!
        }

        Looper.prepare();

        loopHandler1 = new Handler();

        canvas = holder.lockCanvas();
        drawLayout(0);
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
                        drawLayout(axesData[2]);
                        holder.unlockCanvasAndPost(canvas);
                    }
                }
            }

        };
        Looper.loop();
    }

    public void drawLayout(float height){
        canvas.drawColor(Color.parseColor(backgroundColor));
        // draw all paths
        for(int i=0;i<=paths.length-1;i++){
            canvas.drawPath(paths[i], paint);
        }
        // Draw sensor visualization for top beam
        canvas.drawRect(svRectArray[0] - height*20, svRectArray[1], svRectArray[2], svRectArray[3], svPaint);
        // Draw sensor visualization for left beam
        canvas.drawRect(svRectArray[4], svRectArray[5] - height*10, svRectArray[6], svRectArray[7], svPaint);
        // -------------------------------------
        // ONLY FOR DEBUG !!!
        // -------------------------------------
        // Draw sensor visualization for pseudo camera preview
        canvas.drawRect(svRectArray[8], svRectArray[9], svRectArray[10], svRectArray[11], svPaintDebug);
        // -------------------------------------
    }


    public void pause(){
        loopHandler1.getLooper().quit();
        while(true){
            try {
                // Blocks thread until the operation were finished and dies
                thread.join();
            }catch(Exception e){
                Log.d("@SurfaceViewMainActivity#pause: ", String.valueOf(e));
            }
            break;
        }
        Log.d("@pause: ", "inside...");
        thread = null;
    }

    public void resume(Bundle bundle) {
        // Load color from settings
        DataSet.ThemeColor[] themeColors = DataSet.ThemeColor.values();
        this.backgroundColor = themeColors[sharedPreferences.getInt("theme", 0)].backgroundColor;
        this.foregroundColor = themeColors[sharedPreferences.getInt("theme", 0)].foregroundColor;
        Log.d("@surfaceViewMain: ", "color: " + sharedPreferences.getInt("theme", 0));

        init(bundle);

        // Stop last draw thread and execute a new one
        //pause();
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }
}
