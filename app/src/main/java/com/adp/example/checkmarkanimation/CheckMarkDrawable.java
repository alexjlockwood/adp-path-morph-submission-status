package com.adp.example.checkmarkanimation;


import android.animation.Animator;
import android.animation.ObjectAnimator;
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
import android.util.Property;

public class CheckMarkDrawable extends Drawable {

    private static final Property<CheckMarkDrawable, Float> PROGRESS =
            new Property<CheckMarkDrawable, Float>(Float.class, "progress") {
                @Override
                public Float get(CheckMarkDrawable d) {
                    return d.getProgress();
                }

                @Override
                public void set(CheckMarkDrawable d, Float value) {
                    d.setProgress(value);
                }
            };

    private final Path mPath = new Path();
    private final Paint mPaint = new Paint();
    private final RectF mBounds = new RectF();
    private final float mStrokeWidth;
    private final float mLongBarSize;
    private final float mShortBarSize;

    private float mProgress;

    public CheckMarkDrawable(Context context) {
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mStrokeWidth = context.getResources().getDimensionPixelSize(R.dimen.stroke_width);
        mPaint.setStrokeWidth(mStrokeWidth);
        final Resources res = context.getResources();
        mLongBarSize = res.getDimensionPixelSize(R.dimen.long_check_mark_bar);
        mShortBarSize = res.getDimensionPixelSize(R.dimen.short_check_mark_bar);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mBounds.set(bounds);
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

        final float totalSizeDiff = mBounds.height() - (2.2f * mStrokeWidth + mLongBarSize);

        canvas.save();
        canvas.translate(0, totalSizeDiff * 0.22f);
        canvas.translate(mBounds.width() / 2f, mBounds.height() / 2f);
        canvas.rotate(lerp(0, 395, mProgress), 0 , -totalSizeDiff * 0.1f);
        canvas.drawPath(mPath, mPaint);
        canvas.restore();
    }

    public Animator getCheckMarkAnimator(boolean toCheck) {
        return ObjectAnimator.ofFloat(this, CheckMarkDrawable.PROGRESS, toCheck ? 1 : 0);
    }

    public void setProgress(float progress) {
        mProgress = progress;
        invalidateSelf();
    }

    public float getProgress() {
        return mProgress;
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

    /**
     * Linear interpolate between a and b with parameter t.
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}