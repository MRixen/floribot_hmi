package de.hs_heilbronn.floribot.android.floribot_hmi.gui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;

/**
 * Created by mr on 20.05.14.
 */
public class SurfaceViewMainActivity extends SurfaceView implements Runnable{

    private Paint paint;
    private Context context;
    private PointF[] middleBarPoints = new PointF[4];
    private Path middleBar = new Path();
    private Path bottomBar = new Path();

    private DataSet mDataSet;
    private SurfaceHolder holder;
    private Thread thread;

    public SurfaceViewMainActivity(Context context, SurfaceHolder holder) {
        super(context);

        this.context = context;
        this.holder = holder;
        thread = null;

        init();

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#0071ff"));
        paint.setStyle(Paint.Style.FILL);

        //SurfaceHolder holder;

        holder.setFormat(PixelFormat.TRANSPARENT);

    }

    public void init(){
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
        float bottomBarWidthInDp = 100;
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

        // ----- Create path for middle bar -----
        // Define dimension of middle bar
        float marginLeft = 0;
        float rectWidth = 390;
        float rectHeight = 105;
        float marginTop = 80;
        float x = (float) (rectHeight*Math.tan(Math.PI/6) );

        // Convert to px unit
        rectWidth = (pxWidth / dpWidth) * rectWidth;
        marginLeft = (pxWidth / dpWidth) * marginLeft;
        marginTop = (pxHeight / dpHeight) * marginTop;
        rectHeight = (pxHeight / dpHeight) * rectHeight;

        middleBarPoints[0] = new PointF(marginLeft, marginTop);
        middleBarPoints[1] = new PointF(marginLeft, marginTop + rectHeight);
        middleBarPoints[2] = new PointF(rectWidth-x, marginTop + rectHeight);
        middleBarPoints[3] = new PointF(rectWidth, marginTop) ;

        middleBar.setFillType(Path.FillType.EVEN_ODD);
        middleBar.moveTo(middleBarPoints[0].x, middleBarPoints[0].y);
        for(int i=0;i<=3;i++) {
            middleBar.lineTo(middleBarPoints[i].x, middleBarPoints[i].y);
        }
        middleBar.close();
        // ---------------
    }

    @Override
    public void run() {
        Canvas canvas;

        while (!holder.getSurface().isValid()) {
            // SET A TIME LIMIT!
            Log.d("@while: ", "inside...");
        }
        canvas = holder.lockCanvas();
        draw(canvas);
        holder.unlockCanvasAndPost(canvas);
        Log.d("@run: ", "inside...");

    }

    public void draw(Canvas canvas){
        canvas.drawColor(Color.parseColor("#fffef2"));
        canvas.drawPath(middleBar, paint);
        canvas.drawPath(bottomBar, paint);
        Log.d("@draw: ", "inside...");
    }

    public void pause(){
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

    public void resume() {
            if (thread == null) {
                thread = new Thread(this);
                Log.d("@Resume: ", "inside...");
                thread.start();
            }

    }


/*    public SurfaceViewMainActivity(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        mDataSet = new DataSet(context);

*//*        init();

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#0071ff"));
        paint.setStyle(Paint.Style.FILL);

        //SurfaceHolder holder;

        holder.setFormat(PixelFormat.TRANSPARENT);*//*
        *//*DrawThread t = new DrawThread(holder);
        t.start();*//*
    }*/

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("SurfaceViewExecute: ", "inside surfaceCreated");


    }


    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("SurfaceViewExecute: ", "inside surfaceChanged");
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("SurfaceViewExecute: ", "inside surfaceDestroyed");
    }

    public void setHolder(SurfaceHolder holder) {
        this.holder = holder;
    }



/*    class DrawThread extends Thread{

        private final SurfaceHolder holder;

        public DrawThread(SurfaceHolder holder){
            this.holder = holder;
        }

        public void run(){
            Canvas canvas;

            while (!holder.getSurface().isValid()) {
                // SET A TIME LIMIT!
            }
                canvas = holder.lockCanvas();
                draw(canvas);
                holder.unlockCanvasAndPost(canvas);

        }


    }*/
}
