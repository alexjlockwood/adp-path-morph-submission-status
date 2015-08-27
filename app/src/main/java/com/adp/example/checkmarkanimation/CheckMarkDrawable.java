package com.adp.example.checkmarkanimation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.Size;
import android.util.Property;
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

    private static final Property<CheckMarkDrawable, Integer> BACKGROUND_COLOR =
            new Property<CheckMarkDrawable, Integer>(Integer.class, "backgroundColor") {
                @Override
                public Integer get(CheckMarkDrawable v) {
                    return v.getBackgroundColor();
                }

                @Override
                public void set(CheckMarkDrawable v, Integer value) {
                    v.setBackgroundColor(value);
                }
            };

    private static final long CHECK_MARK_ANIMATION_DURATION = 325;

    private final Path mPath = new Path();
    private final Path mArrowHeadPath = new Path();
    private final Path mMarkPath = new Path();
    private final RectF mTotalBounds = new RectF();
    private final RectF mDrawBounds = new RectF();
    private final Paint mPaint = new Paint();
    private final Paint mArrowHeadPaint = new Paint();
    private final Paint mColorPaint = new Paint();
    private final float mStrokeWidth;
    private float mInset;
    private float mProgress;

    private final int mExclamationColor;
    private final int mCheckColor;
    private final int mRefreshColor;
    private int mBackgroundColor;

    @IconType private int mPrevIconType;
    @IconType private int mCurrIconType;

    private float[][][] mEndPoints;
    private float[][][] mControlPoints1;
    private float[][][] mControlPoints2;
    private float[][][] mArrowHeadPoints;
    private float[][][] mMarkPoints;

    private boolean animatingFromCheck;
    private boolean animatingToCheck;

    public CheckMarkDrawable(Resources res) {
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mArrowHeadPaint.setAntiAlias(true);
        mArrowHeadPaint.setStyle(Paint.Style.FILL);
        mArrowHeadPaint.setColor(Color.WHITE);
        mColorPaint.setAntiAlias(true);
        mColorPaint.setStyle(Paint.Style.FILL);
        mStrokeWidth = res.getDimensionPixelSize(R.dimen.stroke_width);
        mPaint.setStrokeWidth(mStrokeWidth);
        mCurrIconType = REFRESH;
        mPrevIconType = REFRESH;
        mExclamationColor = res.getColor(R.color.red);
        mCheckColor = res.getColor(R.color.green);
        mRefreshColor = res.getColor(R.color.blue);
        mBackgroundColor = mRefreshColor;
    }

    @IconType
    public int getIconType() {
        return mCurrIconType;
    }

    public void setIconType(@IconType int iconType) {
        mPrevIconType = mCurrIconType;
        mCurrIconType = iconType;
        mProgress = 1;
        setBackgroundColor(iconType == CHECK ? mCheckColor : iconType == EXCLAMATION ? mExclamationColor : mRefreshColor);
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mTotalBounds.set(bounds);

        mInset = 5 * mStrokeWidth;
        mDrawBounds.set(0, 0, bounds.width() - 2 * mInset, bounds.height() - 2 * mInset);

        final float w = mDrawBounds.width();
        final float h = mDrawBounds.height();
        final float r = w / 2;
        final float dist = calcDistanceFromEndpoint(w / 2);
        final float exclamationLongBarLength = h - 2.5f * mStrokeWidth;

        mEndPoints = new float[][][]{
                {{0, h / 2}, {w / 2, 0}, {w, h / 2}, {w / 2, h}}, // refresh end points
                {{w/2-r*cos(35),h/2 - r*sin(35)}, {w/2-r/2*cos(35), h/2-r/2*sin(35)}, {w/2,h/2}, {w/2-r/2*cos(55), h/2+r/2*sin(55)}}, // check end points
                calcExclamationEndPoints(w, exclamationLongBarLength), // exclamation end points
        };

        mControlPoints1 = new float[][][]{
                {{0, h / 2 - dist}, {w / 2 + dist, 0}, {w, h / 2 + dist}}, // refresh cp1
                {calcLongCheckBarCp(r * 5 / 6), calcLongCheckBarCp(r * 2 / 6), calcSmallCheckBarCp(r / 6)}, // check cp1
                calcExclamationCp1(w, exclamationLongBarLength), // exclamation cp1
        };

        mControlPoints2 = new float[][][]{
                {{w / 2 - dist, 0}, {w, h / 2 - dist}, {w / 2 + dist, h}}, // refresh cp2
                {calcLongCheckBarCp(r * 4 / 6), calcLongCheckBarCp(r / 6), calcSmallCheckBarCp(r * 2 / 6)}, // check cp2
                calcExclamationCp2(w, exclamationLongBarLength), // exclamation cp2
        };

        final float arrowHeadSize = 4 * mStrokeWidth;
        final float arrowHeadHeight = arrowHeadSize * cos(30);
        final float refreshEndX = mEndPoints[0][0][0];
        final float refreshEndY = mEndPoints[0][0][1] - 1; // Subtract one pixel to ensure arrow head and refresh arc connect.

        mArrowHeadPoints = new float[][][]{
                {{refreshEndX, refreshEndY + arrowHeadHeight}, {refreshEndX - arrowHeadSize / 2, refreshEndY}, {refreshEndX + arrowHeadSize / 2, refreshEndY}}, // refresh arrow head points
                {mEndPoints[1][0], mEndPoints[1][0], mEndPoints[1][0]}, // check arrow head points
                {mEndPoints[2][0], mEndPoints[2][0], mEndPoints[2][0]}, // exclamation arrow head points
        };

        // TODO: add extra padding above and below the exclamation point mark
        mMarkPoints = new float[][][]{
                {mEndPoints[0][3], mEndPoints[0][3]}, // refresh mark points
                {mEndPoints[1][3], mEndPoints[1][3]}, // check mark points
                {{w/2, h - mStrokeWidth}, {w/2, h}}, // exclamation mark points
        };
    }

    private static float calcDistanceFromEndpoint(float radius) {
        return radius * ((float) (Math.sqrt(2) - 1) * 4f / 3f);
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

        mColorPaint.setColor(mBackgroundColor);
        final float cornerRadius = mTotalBounds.width() / 2f;
        canvas.drawRoundRect(mTotalBounds, cornerRadius, cornerRadius, mColorPaint);

        canvas.save();
        canvas.translate(mInset, mInset);
        if (animatingToCheck || animatingFromCheck) {
            final float progress = animatingToCheck ? mProgress : 1 - mProgress;
            canvas.translate(lerp(0, -(r / 2 * cos(55) - r / 4 * cos(35)), progress), 0); // center the check horizontally
            canvas.translate(0, lerp(0, r / 2 * cos(55), progress)); // center the check vertically
            if (animatingToCheck) {
                canvas.rotate(lerp(0, -270, mProgress), w / 2, h / 2);
            } else {
                canvas.rotate(lerp(90, -360, mProgress), w / 2, h / 2);
            }
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
        mArrowHeadPath.moveTo(arrowPoints(0, 0), arrowPoints(0, 1));
        mArrowHeadPath.lineTo(arrowPoints(1, 0), arrowPoints(1, 1));
        mArrowHeadPath.lineTo(arrowPoints(2, 0), arrowPoints(2, 1));
        mArrowHeadPath.lineTo(arrowPoints(0, 0), arrowPoints(0, 1));
        canvas.drawPath(mArrowHeadPath, mArrowHeadPaint);

        mMarkPath.rewind();
        mMarkPath.moveTo(markPoints(0, 0), markPoints(0, 1));
        mMarkPath.lineTo(markPoints(1, 0), markPoints(1, 1));
        canvas.drawPath(mMarkPath, mPaint);

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

    private float markPoints(int x, int y) {
        return lerp(mMarkPoints[mPrevIconType][x][y], mMarkPoints[mCurrIconType][x][y], mProgress);
    }

    private Animator getCheckMarkAnimator(@IconType final int nextIconType) {
        if (nextIconType == mCurrIconType) {
            throw new RuntimeException();
        }

        final ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                animatingFromCheck = mCurrIconType == CHECK;
                animatingToCheck = nextIconType == CHECK;
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

        final AnimatorSet set = new AnimatorSet();
        final ObjectAnimator colorAnim = ObjectAnimator.ofInt(this, BACKGROUND_COLOR,
                nextIconType == CHECK ? mCheckColor : nextIconType == EXCLAMATION ? mExclamationColor : mRefreshColor);
        colorAnim.setEvaluator(new ArgbEvaluator());
        set.setInterpolator(new DecelerateInterpolator());
        set.setDuration(CHECK_MARK_ANIMATION_DURATION);
        set.playTogether(colorAnim, anim);
        return set;
    }

    private void setBackgroundColor(@ColorInt int color) {
        mBackgroundColor = color;
        invalidateSelf();
    }

    @ColorInt
    private int getBackgroundColor() {
        return mBackgroundColor;
    }

    public void animateTo(@IconType int iconType) {
        if (iconType != mCurrIconType) {
            getCheckMarkAnimator(iconType).start();
        }
    }

    @Override
    public void setAlpha(int alpha) {
        // TODO: set alpha on other stuff too?
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // TODO: set color filter on other stuff too?
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