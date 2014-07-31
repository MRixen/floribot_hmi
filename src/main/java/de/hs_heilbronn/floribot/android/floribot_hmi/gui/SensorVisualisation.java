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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.hs_heilbronn.floribot.android.floribot_hmi.R;
import de.hs_heilbronn.floribot.android.floribot_hmi.data.BaseClass;

/**
 * Created by mr on 20.05.14.
 *
 */
public class SensorVisualisation extends android.view.SurfaceView implements Runnable{

    private final Bundle surfaceDataBundle;
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
    private int pxWidth, pxHeight;
    private float factorHeight, factorWidth;

    public SensorVisualisation(Context context) {
        super(context);
        this.context = context;
        drawThread = null;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        surfaceDataBundle = getSensorVisualisationData();
    }

    public void calculateDimensions(){
        // Calculate display size
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        Log.d("@calculateDimensions", Thread.currentThread().getName());

        pxWidth = displayMetrics.widthPixels;
        pxHeight = displayMetrics.heightPixels;
        float dpWidth = pxWidth / displayMetrics.density;
        float dpHeight = pxHeight / displayMetrics.density;

        // Factor dp to px
        factorHeight = (pxHeight / dpHeight);
        factorWidth = (pxWidth / dpWidth);
    }

    private int getRes(int res){
        return context.getResources().getInteger(res);
    }

    public Bundle getSensorVisualisationData() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<Bundle> result = executorService.submit(new Callable<Bundle>() {
            @Override
            public Bundle call() throws Exception {
                Log.d("@DataSet->SurfaceDataExecute", Thread.currentThread().getName());
                // Initialize surface data
                calculateDimensions();

                float[] svRectArray = new float[12];
                // Note: Camera width need to be the full length of sensor visualization beam
                float cameraViewWidthInPx = getRes(R.integer.cameraViewWidthInPx);
                float cameraViewHeightInPx = getRes(R.integer.cameraViewHeightInPx);

                // Create rectangle for top sensor visualization (visualization for steer amount)
                // -------------------------------------
                // Distance from left display border to left side of rectangle
                svRectArray[0] = factorWidth * (getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp) + getRes(R.integer.svOffsetInDp));
                // Distance from top display border to top side of rectangle
                svRectArray[1] = factorHeight * (getRes(R.integer.svMarginTopInDp) + getRes(R.integer.svBorderMarginInDp));
                // Distance from left display border to right side of rectangle
                svRectArray[2] = svRectArray[0] + cameraViewWidthInPx;
                // Distance from top display border to bottom side of rectangle
                svRectArray[3] = factorHeight * (getRes(R.integer.svMarginTopInDp) + getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp));
                // -------------------------------------

                // Create rectangle for left sensor visualization (visualization for drive amount)
                // -------------------------------------
                // Distance from left display border to left side of rectangle
                svRectArray[4] = factorWidth * getRes(R.integer.svBorderMarginInDp);
                // Distance from top display border to top side of rectangle
                svRectArray[5] = factorHeight * (getRes(R.integer.svMarginTopInDp)  + getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp) + getRes(R.integer.svOffsetInDp));
                // Distance from left display border to right side of rectangle
                svRectArray[6] = factorWidth * (getRes(R.integer.svBorderMarginInDp) + getRes(R.integer.svBeamWidthInDp));
                // Distance from top display border to bottom side of rectangle
                svRectArray[7] = svRectArray[5] + cameraViewHeightInPx;
                // -------------------------------------

                // Return surface data
                Bundle surfaceData = new Bundle();
                surfaceData.putFloatArray(context.getResources().getString(R.string.svArray), svRectArray);
                return surfaceData;
            }
        });

        try {
            return result.get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void surfaceInit() {
        // Load draw array for sensor visualization
        if(surfaceDataBundle != null){
            if (surfaceDataBundle.containsKey(context.getResources().getString(R.string.svArray))){
                svRectArray = surfaceDataBundle.getFloatArray(context.getResources().getString(R.string.svArray));
            }
        }

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
        BaseClass.sendToSensorVisualization = new Handler() {
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

    public void pauseSensorVisualisation(){
            try {
                handlerForDrawThread.getLooper().quit();
                handlerForDrawThread = null;
                // Blocks drawThread until all operations are finished
                drawThread.join();
            }catch(Exception e){
                Log.d("@GlobalLayout#pauseSensorVisualisation: ", String.valueOf(e));
            }
        drawThread = null;
    }

    public void startSensorVisualisation(SurfaceView surface) {
        // Stop old drawThread to provide new theme settings
        pauseSensorVisualisation();

        // Set holder
        surface.setZOrderOnTop(false);
        holder = surface.getHolder();

        // Load color from settings
        BaseClass.ThemeColor[] themeColors = BaseClass.ThemeColor.values();
        this.backgroundColor = themeColors[sharedPreferences.getInt("theme", 0)].backgroundColor;

        surfaceInit();

        // Stop last draw drawThread and execute a new one
        if (drawThread == null) {
            drawThread = new Thread(this);
            drawThread.start();
        }
    }
}
