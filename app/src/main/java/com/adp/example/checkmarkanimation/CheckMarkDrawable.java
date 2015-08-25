package com.adp.example.checkmarkanimation;


import android.animation.Animator;
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
import android.graphics.drawable.Drawable;

public class CheckMarkDrawable extends Drawable {

    private final Path mPath = new Path();
    private final Paint mPaint = new Paint();
    private final float mStrokeWidth;
    private final float mLongBarSize;
    private final float mShortBarSize;
    private float mWidth;
    private float mHeight;

    private float mProgress;

    public CheckMarkDrawable(Context context) {
        final Resources res = context.getResources();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mStrokeWidth = res.getDimensionPixelSize(R.dimen.stroke_width);
        mPaint.setStrokeWidth(mStrokeWidth);
        mLongBarSize = res.getDimensionPixelSize(R.dimen.long_check_mark_bar);
        mShortBarSize = res.getDimensionPixelSize(R.dimen.short_check_mark_bar);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mWidth = bounds.width();
        mHeight = bounds.height();
    }

    @Override
    public void draw(Canvas canvas) {
        mPath.rewind();

        mPath.lineTo(0, -mStrokeWidth);
        mPath.moveTo(0, -2.2f * mStrokeWidth + lerp(0, 1.2f * mStrokeWidth, mProgress));
        mPath.lineTo(0, -mLongBarSize);

        final float shortBarWidth = (float) Math.cos(Math.toRadians(35f)) * lerp(0, mShortBarSize, mProgress);
        mPath.moveTo(0, -mStrokeWidth / 2f);
        mPath.lineTo(-shortBarWidth, -mStrokeWidth / 2f);

        final float totalSizeDiff = mHeight - (2.2f * mStrokeWidth + mLongBarSize);

        canvas.save();
        canvas.translate(0, totalSizeDiff * 0.225f);
        canvas.translate(mWidth / 2f, mHeight / 2f);
        canvas.rotate(lerp(0, 395, mProgress), 0 , -totalSizeDiff * 0.1f);
        canvas.drawPath(mPath, mPaint);
        canvas.restore();
    }

    public Animator getCheckMarkAnimator(boolean toCheck) {
        final ValueAnimator anim = ValueAnimator.ofFloat(mProgress, toCheck ? 1 : 0);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mProgress = (float) animation.getAnimatedValue();
                invalidateSelf();
            }
        });
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
}