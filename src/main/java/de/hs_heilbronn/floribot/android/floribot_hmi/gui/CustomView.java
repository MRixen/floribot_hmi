package de.hs_heilbronn.floribot.android.floribot_hmi.gui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import de.hs_heilbronn.floribot.android.floribot_hmi.data.DataSet;

import static java.lang.Math.PI;
import static java.lang.Math.tan;

/**
 * Created by mr on 09.05.14.
 */
public class CustomView extends View {

    private Context context;
    private DataSet mDataSet;
    private float bottomBarHeight, bottomBarLineStopX, bottomBarLineStartY, bottomBarLineStopY, topBarHeight,bottomBarLineStartX;
    private Path bottomBar = new Path();
    private Paint barPaint, bottomBarLinePaint;
    private PointF[] bottomBarPoints = new PointF[4];
    private float bottomBarOffsetHeight, bottomBarOffsetWidth;
    private float topBarHeightInDp = 50; // in dp

    public CustomView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.context = context;

        // Load parameters
        mDataSet = new DataSet(context);

        // Initialize parameters for paint objects, e.g color, etc.
        init();
    }

    public CustomView(Context context) {
        super(context);
        this.context = context;
    }

    // Create paint objects for setting parameters (e.g. color, text size, etc.)
    // NOTE: Not to create in onDraw to get better performance
    private void init() {
        // Calculate display size
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();

        float displayWidth = displayMetrics.widthPixels;
        float displayHeight = displayMetrics.heightPixels ;

        float dpWidth = displayWidth / displayMetrics.density;
        float dpHeight = displayHeight / displayMetrics.density;

        // Calculate top and bottom bar height dp to px
        bottomBarHeight = (displayMetrics.heightPixels / dpHeight) * mDataSet.bottomBarHeightInDp;
        topBarHeight = (displayMetrics.heightPixels / dpHeight) * topBarHeightInDp;

        bottomBarOffsetHeight = displayHeight - bottomBarHeight - topBarHeight;
        bottomBarOffsetWidth = displayWidth / mDataSet.topBarWidthDivider;

        bottomBarPoints[0] = new PointF((float) (bottomBarOffsetWidth + bottomBarHeight/tan(PI/3)), bottomBarOffsetHeight);
        bottomBarPoints[1] = new PointF(displayWidth, displayHeight - bottomBarHeight - topBarHeight);
        bottomBarPoints[2] = new PointF(displayWidth, displayHeight - topBarHeight);
        bottomBarPoints[3] = new PointF(bottomBarOffsetWidth, displayHeight- topBarHeight) ;

        bottomBar.setFillType(Path.FillType.EVEN_ODD);
        bottomBar.moveTo(bottomBarPoints[0].x, bottomBarPoints[0].y);
        for(int i=0;i<=3;i++) {
            bottomBar.lineTo(bottomBarPoints[i].x, bottomBarPoints[i].y);
        }
        bottomBar.close();



        // Calculate coordinates for line in top bar
        float linOffset = (displayMetrics.heightPixels / dpHeight) * mDataSet.bottomBarLineOffset;

        bottomBarLineStartX = displayWidth -  linOffset;
        bottomBarLineStartY = bottomBarOffsetHeight;
        bottomBarLineStopX = (float) (displayWidth -  linOffset - bottomBarHeight/tan(PI/3));
        bottomBarLineStopY = bottomBarOffsetHeight + bottomBarHeight;

        // Paint for top and bottom bar
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(mDataSet.barColor);
        barPaint.setStyle(Paint.Style.FILL);

        // Paint for line in top bar
        bottomBarLinePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        bottomBarLinePaint.setColor(mDataSet.barLineColor);
        bottomBarLinePaint.setStrokeWidth(3);

    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);

        // Canvas: What to draw
        // Paint: How to draw
        //-----------

        // Draw bottom bar
        canvas.drawPath(bottomBar, barPaint);
        // Draw line in bottom bar

        canvas.drawLine(bottomBarLineStartX, bottomBarLineStartY, bottomBarLineStopX, bottomBarLineStopY, bottomBarLinePaint);
        // Draw bottom bar
        //canvas.drawRect(0,canvas.getHeight()-bottomBarHeight,canvas.getWidth(),canvas.getHeight(),barPaint);
    }

}