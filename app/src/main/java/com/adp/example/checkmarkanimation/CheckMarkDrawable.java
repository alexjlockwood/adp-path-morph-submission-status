package com.adp.example.checkmarkanimation;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.Size;
import android.view.animation.DecelerateInterpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class CheckMarkDrawable extends Drawable {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CHECK, EXCLAMATION, REFRESH})
    public @interface IconType {}
    public static final int REFRESH = 0;
    public static final int CHECK = 1;
    public static final int EXCLAMATION = 2;

    private final Path mPath = new Path();
    private final Path mArrowHeadPath = new Path();
    private final RectF mDrawBounds = new RectF();
    private final Paint mPaint = new Paint();
    private final Paint mArrowHeadPaint = new Paint();
    private final float mStrokeWidth;
    private float mInset;
    private float mProgress;

    @IconType private int mPrevIconType = CHECK;
    @IconType private int mCurrIconType = CHECK;

    private float[][][] mEndPoints;
    private float[][][] mControlPoints1;
    private float[][][] mControlPoints2;

    private float[][][] mArrowHeadPoints;

    public CheckMarkDrawable(Context context) {
        final Resources res = context.getResources();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mArrowHeadPaint.setAntiAlias(true);
        mArrowHeadPaint.setStyle(Paint.Style.FILL);
        mArrowHeadPaint.setColor(Color.WHITE);
        mStrokeWidth = res.getDimensionPixelSize(R.dimen.stroke_width);
        mPaint.setStrokeWidth(mStrokeWidth);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        mInset = 5 * mStrokeWidth;
        mDrawBounds.set(0, 0, bounds.width() - 2 * mInset, bounds.height() - 2 * mInset);

        final float w = mDrawBounds.width();
        final float h = mDrawBounds.height();
        final float r = w / 2;
        final float dist = calcDistanceFromEndpoint(w / 2);

        mEndPoints = new float[][][]{
                {{0, h / 2}, {w / 2, 0}, {w, h / 2}, {w / 2, h}}, // refresh end points
                {{w/2-r*cos(35),h/2 - r*sin(35)}, {w/2-r/2*cos(35), h/2-r/2*sin(35)}, {w/2,h/2}, {w/2-r/2*cos(55), h/2+r/2*sin(55)}}, // check end points
                calcExclamationEndPoints(w, h), // exclamation end points
        };

        mControlPoints1 = new float[][][]{
                {{0, h / 2 - dist}, {w / 2 + dist, 0}, {w, h / 2 + dist}}, // refresh cp1
                {calcLongCheckBarCp(r * 5 / 6), calcLongCheckBarCp(r * 2 / 6), calcSmallCheckBarCp(r / 6)}, // check cp1
                calcExclamationCp1(w, h), // exclamation cp1
        };

        mControlPoints2 = new float[][][]{
                {{w / 2 - dist, 0}, {w, h / 2 - dist}, {w / 2 + dist, h}}, // refresh cp2
                {calcLongCheckBarCp(r * 4 / 6), calcLongCheckBarCp(r / 6), calcSmallCheckBarCp(r * 2 / 6)}, // check cp2
                calcExclamationCp2(w, h), // exclamation cp2
        };

        final float arrowHeadSize = 4 * mStrokeWidth;
        final float arrowHeadHeight = arrowHeadSize * cos(30);
        final float refreshEndX = mEndPoints[0][0][0];
        final float refreshEndY = mEndPoints[0][0][1];

        mArrowHeadPoints = new float[][][]{
                {{refreshEndX, refreshEndY + arrowHeadHeight}, {refreshEndX - arrowHeadSize / 2, refreshEndY}, {refreshEndX + arrowHeadSize / 2, refreshEndY}}, // refresh arrow head points
                {mEndPoints[1][0], mEndPoints[1][0], mEndPoints[1][0]}, // check arrow head points
                {mEndPoints[2][0], mEndPoints[2][0], mEndPoints[2][0]}, // exclamation arrow head points
        };
    }

    private float[][] calcExclamationEndPoints(float w, float h) {
        return new float[][] {
                {w/2,0}, {w/2,h/3}, {w/2,2*h/3}, {w/2,h},
        };
    }

    private float[][] calcExclamationCp1(float w, float h) {
        return new float[][] {
                {w/2,h/9}, {w/2,4*h/9}, {w/2,7*h/9},
        };
    }

    private float[][] calcExclamationCp2(float w, float h) {
        return new float[][] {
                {w/2,2*h/9}, {w/2,5*h/9}, {w/2,8*h/9},
        };
    }

    @Size(2)
    private float[] calcLongCheckBarCp(float r) {
        final float w = mDrawBounds.width();
        final float h = mDrawBounds.height();
        float[] out = new float[2];
        out[0] = w / 2 - r * cos(35);
        out[1] = h / 2 - r * sin(35);
        return out;
    }

    @Size(2)
    private float[] calcSmallCheckBarCp(float r) {
        final float w = mDrawBounds.width();
        final float h = mDrawBounds.height();
        float[] out = new float[2];
        out[0] = w / 2 - r * cos(55);
        out[1] = h / 2 + r * sin(55);
        return out;
    }

    @Override
    public void draw(Canvas canvas) {
        final float w = mDrawBounds.width();
        final float h = mDrawBounds.height();
        final float r = w / 2;

        canvas.save();
        canvas.translate(mInset, mInset);
        if (mPrevIconType == CHECK || mCurrIconType == CHECK) {
            final float progress = mCurrIconType == CHECK ? mProgress : 1 - mProgress;
            canvas.translate(lerp(0, -(r / 2 * cos(55) - r / 4 * cos(35)), progress), 0); // center the check horizontally
            canvas.translate(0, lerp(r / 2 * cos(55), 0, progress)); // center the check vertically
            canvas.rotate(90 + lerp(0, -450, progress), w / 2, h / 2);
        } else {
            canvas.rotate(lerp(0, -360, mProgress), w / 2, h / 2);
        }

        mPath.rewind();
        mPath.moveTo(end(0, 0), end(0, 1));
        mPath.cubicTo(cp1(0, 0), cp1(0, 1), cp2(0, 0), cp2(0, 1), end(1, 0), end(1, 1));
        mPath.cubicTo(cp1(1, 0), cp1(1, 1), cp2(1, 0), cp2(1, 1), end(2, 0), end(2, 1));
        mPath.cubicTo(cp1(2, 0), cp1(2, 1), cp2(2, 0), cp2(2, 1), end(3, 0), end(3, 1));
        canvas.drawPath(mPath, mPaint);

        mArrowHeadPath.rewind();
        mArrowHeadPath.reset();
        mArrowHeadPath.moveTo(arrowPoints(0, 0), arrowPoints(0, 1));
        mArrowHeadPath.lineTo(arrowPoints(1, 0), arrowPoints(1, 1));
        mArrowHeadPath.lineTo(arrowPoints(2, 0), arrowPoints(2, 1));
        mArrowHeadPath.close();
        canvas.drawPath(mArrowHeadPath, mArrowHeadPaint);

        canvas.restore();
    }

    private float end(int x, int y) {
        return lerp(mEndPoints[mPrevIconType][x][y], mEndPoints[mCurrIconType][x][y], mProgress);
    }

    private float cp1(int x, int y) {
        return lerp(mControlPoints1[mPrevIconType][x][y], mControlPoints1[mCurrIconType][x][y], mProgress);
    }

    private float cp2(int x, int y) {
        return lerp(mControlPoints2[mPrevIconType][x][y], mControlPoints2[mCurrIconType][x][y], mProgress);
    }

    private float arrowPoints(int x, int y) {
        return lerp(mArrowHeadPoints[mPrevIconType][x][y], mArrowHeadPoints[mCurrIconType][x][y], mProgress);
    }

    private static float calcDistanceFromEndpoint(float radius) {
        return radius * ((float) (Math.sqrt(2) - 1) * 4f / 3f);
    }

    public Animator getCheckMarkAnimator(@IconType final int nextIconType) {
        final ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mPrevIconType = mCurrIconType;
                mCurrIconType = nextIconType;
            }
        });
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mProgress = (float) animation.getAnimatedValue();
                invalidateSelf();
            }
        });
        anim.setInterpolator(new DecelerateInterpolator());
        return anim;
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    /** Linear interpolate between a and b with parameter t. */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float cos(float degrees) {
        return (float) Math.cos(Math.toRadians(degrees));
    }

    private static float sin(float degrees) {
        return (float) Math.sin(Math.toRadians(degrees));
    }
}