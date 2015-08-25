package com.adp.example.checkmarkanimation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.Checkable;
import android.widget.FrameLayout;

public class CheckMarkView extends FrameLayout implements Checkable {

    private static final Property<CheckMarkView, Integer> COLOR =
            new Property<CheckMarkView, Integer>(Integer.class, "color") {
                @Override
                public Integer get(CheckMarkView v) {
                    return v.getColor();
                }

                @Override
                public void set(CheckMarkView v, Integer value) {
                    v.setColor(value);
                }
            };

    private static final long CHECK_MARK_ANIMATION_DURATION = 400;

    private final CheckMarkDrawable mDrawable;
    private final Paint mPaint = new Paint();
    private final RectF mBounds = new RectF();
    private final int mExclamationColor;
    private final int mCheckColor;
    private boolean mIsCheck;
    private int mColor;

    public CheckMarkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        mColor = getResources().getColor(R.color.red);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mDrawable = new CheckMarkDrawable(context);
        mDrawable.setCallback(this);

        mExclamationColor = getResources().getColor(R.color.red);
        mCheckColor = getResources().getColor(R.color.green);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mDrawable.setBounds(0, 0, w, h);
        mBounds.set(0, 0, w, h);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new ViewOutlineProvider() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            setClipToOutline(true);
        }
    }

    private void setColor(int color) {
        mColor = color;
        invalidate();
    }

    private int getColor() {
        return mColor;
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mDrawable || super.verifyDrawable(who);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float cornerRadius = mBounds.width() / 2f;
        mPaint.setColor(mColor);
        canvas.drawRoundRect(mBounds, cornerRadius, cornerRadius, mPaint);
        mDrawable.draw(canvas);
    }

    @Override
    public void setChecked(boolean checked) {
        if (mIsCheck != checked) {
            startAnimation();
        }
    }

    @Override
    public boolean isChecked() {
        return mIsCheck;
    }

    @Override
    public void toggle() {
        startAnimation();
    }

    @CheckMarkDrawable.IconType private int currentIconType = CheckMarkDrawable.CHECK;

    private void startAnimation() {
        final AnimatorSet set = new AnimatorSet();
        final ObjectAnimator colorAnim = ObjectAnimator.ofInt(this, COLOR, mIsCheck ? mExclamationColor : mCheckColor);
        colorAnim.setEvaluator(new ArgbEvaluator());
        final int nextIconType = currentIconType == CheckMarkDrawable.CHECK
                ? CheckMarkDrawable.EXCLAMATION : currentIconType == CheckMarkDrawable.EXCLAMATION
                ? CheckMarkDrawable.REFRESH : CheckMarkDrawable.CHECK;
        currentIconType = nextIconType;
        final Animator checkAnim = mDrawable.getCheckMarkAnimator(nextIconType);
        set.setInterpolator(new DecelerateInterpolator());
        set.setDuration(CHECK_MARK_ANIMATION_DURATION);
        set.playTogether(colorAnim, checkAnim);
        set.start();
        mIsCheck = !mIsCheck;
    }
}
