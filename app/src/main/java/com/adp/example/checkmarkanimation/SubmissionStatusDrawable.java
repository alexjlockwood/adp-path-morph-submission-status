package com.adp.example.checkmarkanimation;

import android.animation.ArgbEvaluator;
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
import android.support.v4.content.ContextCompat;
import android.view.animation.DecelerateInterpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.adp.example.checkmarkanimation.MathUtils.cos;
import static com.adp.example.checkmarkanimation.MathUtils.lerp;
import static com.adp.example.checkmarkanimation.MathUtils.sin;
import static com.adp.example.checkmarkanimation.MathUtils.sqrt;

/**
 * A custom drawable that animates between a done (check), late (exclamation mark), and returned
 * (refresh) icon states. The morphing animation works by translating the control points and end
 * points of three cubic bezier curves.
 */
public class SubmissionStatusDrawable extends Drawable {

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({DONE, LATE, RETURNED})
  public @interface IconType {}

  public static final int RETURNED = 0;
  public static final int DONE = 1;
  public static final int LATE = 2;

  private static final int ANIMATION_DURATION = 325;
  private static final float DEBUG_SLOW_DURATION_FACTOR = 5f;

  private static final ArgbEvaluator ARGB_EVALUATOR = new ArgbEvaluator();

  // Precomputed trig constants.
  private static final float COS55 = cos(55);
  private static final float COS35 = cos(35);
  private static final float SIN55 = sin(55);
  private static final float SIN35 = sin(35);

  // Multiply this constant by R to approximate the distance between the control
  // points and end points of a circle with radius R.
  private static final float FOUR_SPLINE_MAGIC_NUMBER = (sqrt(2) - 1) * 4 / 3;

  private final Path mPath = new Path();
  private final Path mArrowHeadPath = new Path();
  private final Path mExclamationDotPath = new Path();
  private final RectF mTotalBounds = new RectF();
  private final RectF mDrawBounds = new RectF();
  private final float mStrokeWidth;
  private float mInset;
  private float mProgress;

  private final Paint mIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  private final int mLateColor;
  private final int mDoneColor;
  private final int mReturnedColor;
  private int mBackgroundColor;

  @IconType private int mPrevIconType;
  @IconType private int mCurrIconType;

  private float[][][] mEndPoints;
  private float[][][] mControlPoints1;
  private float[][][] mControlPoints2;
  private float[][][] mArrowHeadPoints;
  private float[][][] mExclamationDotPoints;

  private boolean mAnimatingFromCheck;
  private boolean mAnimatingToCheck;

  private final Paint mDebugControlPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final float mDebugControlPointRadius;
  private final float mDebugEndPointRadius;

  private boolean mDebugShouldEnableRotation = true;
  private boolean mDebugShouldShowControlPoints;
  private boolean mDebugShouldSlowDownAnimation;

  public SubmissionStatusDrawable(Context ctx) {
    final Resources res = ctx.getResources();

    mReturnedColor = ContextCompat.getColor(ctx, R.color.quantum_vanillablue500);
    mDoneColor = ContextCompat.getColor(ctx, R.color.quantum_vanillagreen500);
    mLateColor = ContextCompat.getColor(ctx, R.color.quantum_vanillared500);

    mStrokeWidth = res.getDimension(R.dimen.stroke_width);
    mIconPaint.setColor(Color.WHITE);
    mIconPaint.setStrokeWidth(mStrokeWidth);
    mIconPaint.setStyle(Paint.Style.STROKE);
    mBackgroundPaint.setStyle(Paint.Style.FILL);

    // Debugging stuff.
    mDebugControlPointRadius = res.getDimension(R.dimen.debug_control_point_radius);
    mDebugEndPointRadius = res.getDimension(R.dimen.debug_end_point_radius);
    mDebugControlPointPaint.setStrokeWidth(res.getDimension(R.dimen.debug_bounds_stroke_width));
    mDebugControlPointPaint.setColor(Color.BLACK);

    setIconType(RETURNED);
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    mTotalBounds.set(bounds);

    final float totalWidth = mTotalBounds.width();
    final float totalRadius = totalWidth / 2;
    mInset = (totalWidth - sqrt(2 * totalRadius * totalRadius)) / 2;
    mDrawBounds.set(0, 0, bounds.width() - 2 * mInset, bounds.height() - 2 * mInset);

    final float w = mDrawBounds.width();
    final float h = mDrawBounds.height();
    final float r = w / 2;
    final float exclamationPadding = h / 6;
    final float exclamationLongBarLength = h - 2.5f * mStrokeWidth - 2 * exclamationPadding;

    mEndPoints = new float[][][]{
        {
            {0, h / 2},
            {w / 2, 0},
            {w, h / 2},
            {w / 2, h}
        }, // refresh end points
        {
            {w / 2 - r * COS35, h / 2 - r * SIN35},
            {w / 2 - r / 2 * COS35, h / 2 - r / 2 * SIN35},
            {w / 2, h / 2}, {w / 2 - r / 2 * COS55, h / 2 + r / 2 * SIN55}
        }, // check end points
        calcExclamationEndPoints(w / 2, exclamationLongBarLength, exclamationPadding), // exclamation end points
    };

    final float dist = r * FOUR_SPLINE_MAGIC_NUMBER;
    mControlPoints1 = new float[][][]{
        {
            {0, h / 2 - dist},
            {w / 2 + dist, 0},
            {w, h / 2 + dist}
        }, // refresh cp1
        {calcLongCheckBarCp(r * 5 / 6), calcLongCheckBarCp(r * 2 / 6), calcSmallCheckBarCp(r / 6)}, // check cp1
        calcExclamationCp1(w / 2, exclamationLongBarLength, exclamationPadding), // exclamation cp1
    };

    mControlPoints2 = new float[][][]{
        {
            {w / 2 - dist, 0},
            {w, h / 2 - dist},
            {w / 2 + dist, h}
        }, // refresh cp2
        {calcLongCheckBarCp(r * 4 / 6), calcLongCheckBarCp(r / 6), calcSmallCheckBarCp(r * 2 / 6)}, // check cp2
        calcExclamationCp2(w / 2, exclamationLongBarLength, exclamationPadding), // exclamation cp2
    };

    final float arrowHeadSize = 4 * mStrokeWidth;
    final float arrowHeadHeight = arrowHeadSize * cos(30);
    final float refreshEndX = mEndPoints[0][0][0];
    final float refreshEndY = mEndPoints[0][0][1] - 1; // Subtract one pixel to ensure arrow head and refresh arc connect.

    mArrowHeadPoints = new float[][][]{
        {
            {refreshEndX, refreshEndY + arrowHeadHeight},
            {refreshEndX - arrowHeadSize / 2, refreshEndY},
            {refreshEndX + arrowHeadSize / 2, refreshEndY}
        }, // refresh arrow head points
        {mEndPoints[1][0], mEndPoints[1][0], mEndPoints[1][0]}, // check arrow head points
        {mEndPoints[2][0], mEndPoints[2][0], mEndPoints[2][0]}, // exclamation arrow head points
    };

    // TODO: add extra padding above and below the exclamation point mark
    // TODO: figure out nicer way to animate in/out the exclamation point mark
    final float sw = mStrokeWidth;
    final float o = exclamationPadding;
    mExclamationDotPoints = new float[][][]{
        {mEndPoints[0][3], mEndPoints[0][3], mEndPoints[0][3], mEndPoints[0][3]}, // refresh points
        {mEndPoints[1][3], mEndPoints[1][3], mEndPoints[1][3], mEndPoints[1][3]}, // check mark points
        {
            {w / 2 - sw / 2, h - sw - o},
            {w / 2 + sw / 2, h - sw - o},
            {w / 2 + sw / 2, h - o},
            {w / 2 - sw / 2, h - o}
        }, // exclamation mark points
    };
  }

  @Size(4)
  private static float[][] calcExclamationEndPoints(float x, float h, float offset) {
    final float o = offset;
    return new float[][]{
        {x, o}, {x, o + h / 3}, {x, o + 2 * h / 3}, {x, o + h},
    };
  }

  @Size(3)
  private static float[][] calcExclamationCp1(float x, float h, float offset) {
    final float o = offset;
    return new float[][]{
        {x, o + h / 9}, {x, o + 4 * h / 9}, {x, o + 7 * h / 9},
    };
  }

  @Size(3)
  private static float[][] calcExclamationCp2(float x, float h, float offset) {
    final float o = offset;
    return new float[][]{
        {x, o + 2 * h / 9}, {x, o + 5 * h / 9}, {x, o + 8 * h / 9},
    };
  }

  @Size(2)
  private float[] calcLongCheckBarCp(float r) {
    return new float[]{
        mDrawBounds.width() / 2 - r * COS35,
        mDrawBounds.height() / 2 - r * SIN35,
    };
  }

  @Size(2)
  private float[] calcSmallCheckBarCp(float r) {
    return new float[]{
        mDrawBounds.width() / 2 - r * COS55,
        mDrawBounds.height() / 2 + r * SIN55,
    };
  }

  @Override
  public void draw(Canvas canvas) {
    mBackgroundPaint.setColor(mBackgroundColor);
    final float radius = Math.min(mTotalBounds.width(), mTotalBounds.height()) / 2f;
    canvas.drawCircle(mTotalBounds.centerX(), mTotalBounds.centerY(), radius, mBackgroundPaint);

    final float w = mDrawBounds.width();
    final float h = mDrawBounds.height();
    final float r = w / 2;

    canvas.save();
    canvas.translate(mInset, mInset);
    if (mAnimatingToCheck || mAnimatingFromCheck) {
      final float progress = mAnimatingToCheck ? mProgress : 1 - mProgress;
      canvas.translate(lerp(0, -(r / 2 * COS55 - r / 4 * COS35), progress), 0); // center the check horizontally
      canvas.translate(0, lerp(0, r / 2 * COS55, progress)); // center the check vertically
      if (mAnimatingToCheck) {
        maybeRotate(canvas, lerp(0, -270, mProgress), w / 2, h / 2);
      } else {
        maybeRotate(canvas, lerp(90, -360, mProgress), w / 2, h / 2);
      }
    } else {
      maybeRotate(canvas, lerp(0, -360, mProgress), w / 2, h / 2);
    }

    // Draw the three cubic bezier curves.
    mPath.rewind();
    mPath.moveTo(end(0, 0), end(0, 1));
    mPath.cubicTo(cp1(0, 0), cp1(0, 1), cp2(0, 0), cp2(0, 1), end(1, 0), end(1, 1));
    mPath.cubicTo(cp1(1, 0), cp1(1, 1), cp2(1, 0), cp2(1, 1), end(2, 0), end(2, 1));
    mPath.cubicTo(cp1(2, 0), cp1(2, 1), cp2(2, 0), cp2(2, 1), end(3, 0), end(3, 1));

    // Draw the arrow head displayed by the RETURNED icon.
    mArrowHeadPath.rewind();
    mArrowHeadPath.moveTo(arrowHeadPoint(0, 0), arrowHeadPoint(0, 1));
    mArrowHeadPath.lineTo(arrowHeadPoint(1, 0), arrowHeadPoint(1, 1));
    mArrowHeadPath.lineTo(arrowHeadPoint(2, 0), arrowHeadPoint(2, 1));
    mArrowHeadPath.lineTo(arrowHeadPoint(0, 0), arrowHeadPoint(0, 1));

    // Draw the exclamation dot displayed by the LATE icon.
    mExclamationDotPath.rewind();
    mExclamationDotPath.moveTo(exclamationDotPoint(0, 0), exclamationDotPoint(0, 1));
    mExclamationDotPath.lineTo(exclamationDotPoint(1, 0), exclamationDotPoint(1, 1));
    mExclamationDotPath.lineTo(exclamationDotPoint(2, 0), exclamationDotPoint(2, 1));
    mExclamationDotPath.lineTo(exclamationDotPoint(3, 0), exclamationDotPoint(3, 1));
    mExclamationDotPath.lineTo(exclamationDotPoint(0, 0), exclamationDotPoint(0, 1));

    mIconPaint.setStyle(Paint.Style.FILL);
    canvas.drawPath(mArrowHeadPath, mIconPaint);
    canvas.drawPath(mExclamationDotPath, mIconPaint);
    mIconPaint.setStyle(Paint.Style.STROKE);
    canvas.drawPath(mPath, mIconPaint);

    maybeDrawDebugControlPoints(canvas);

    canvas.restore();
  }

  private float cp1(int pos, int xy) {
    return lerpPoint(mControlPoints1, pos, xy);
  }

  private float cp2(int pos, int xy) {
    return lerpPoint(mControlPoints2, pos, xy);
  }

  private float end(int pos, int xy) {
    return lerpPoint(mEndPoints, pos, xy);
  }

  private float arrowHeadPoint(int pos, int xy) {
    return lerpPoint(mArrowHeadPoints, pos, xy);
  }

  private float exclamationDotPoint(int pos, int xy) {
    return lerpPoint(mExclamationDotPoints, pos, xy);
  }

  private float lerpPoint(float[][][] points, int pos, int xy) {
    return lerp(points[mPrevIconType][pos][xy], points[mCurrIconType][pos][xy], mProgress);
  }

  /** Sets a new icon state without playing an animation. */
  public void setIconType(@IconType int iconType) {
    mCurrIconType = iconType;
    mPrevIconType = iconType;
    mProgress = 1f;
    mBackgroundColor = iconType == DONE ? mDoneColor : iconType == LATE ? mLateColor : mReturnedColor;
    invalidateSelf();
  }

  /** Returns the current icon state. */
  @IconType
  public int getIconType() {
    return mCurrIconType;
  }

  /** Animates to a new icon state. */
  public void animateTo(@IconType int iconType) {
    if (iconType != mCurrIconType) {
      startAnimation(iconType);
    }
  }

  private void startAnimation(@IconType final int nextIconType) {
    if (nextIconType == mCurrIconType) {
      return;
    }

    mAnimatingFromCheck = mCurrIconType == DONE;
    mAnimatingToCheck = nextIconType == DONE;
    mPrevIconType = mCurrIconType;
    mCurrIconType = nextIconType;

    int duration = ANIMATION_DURATION;
    if (mDebugShouldSlowDownAnimation) {
      duration *= DEBUG_SLOW_DURATION_FACTOR;
    }

    final int startBgColor = mBackgroundColor;
    final int endBgColor = getBackgroundColorForIconType(nextIconType);

    final ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
    anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        final float newProgress = animation.getAnimatedFraction();
        final int newBgColor = (Integer) ARGB_EVALUATOR.evaluate(mProgress, startBgColor, endBgColor);
        if (mProgress != newProgress || mBackgroundColor != newBgColor) {
          mProgress = newProgress;
          mBackgroundColor = newBgColor;
          invalidateSelf();
        }
      }
    });
    anim.setDuration(duration);
    anim.setInterpolator(new DecelerateInterpolator());
    anim.start();
  }

  private int getBackgroundColorForIconType(@IconType int iconType) {
    return iconType == DONE ? mDoneColor : iconType == LATE ? mLateColor : mReturnedColor;
  }

  /* Overridden drawable methods. */

  @Override
  public void setAlpha(int alpha) {
    if (mIconPaint.getAlpha() != alpha || mBackgroundPaint.getAlpha() != alpha) {
      mIconPaint.setAlpha(alpha);
      mBackgroundPaint.setAlpha(alpha);
      invalidateSelf();
    }
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    mIconPaint.setColorFilter(cf);
    mBackgroundPaint.setColorFilter(cf);
    invalidateSelf();
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }


  /* Debugging methods. */

  void setDebugEnableRotation(boolean shouldEnableRotation) {
    if (mDebugShouldEnableRotation != shouldEnableRotation) {
      mDebugShouldEnableRotation = shouldEnableRotation;
      invalidateSelf();
    }
  }

  void setDebugShowControlPoints(boolean shouldShowControlPoints) {
    if (mDebugShouldShowControlPoints != shouldShowControlPoints) {
      mDebugShouldShowControlPoints = shouldShowControlPoints;
      invalidateSelf();
    }
  }

  void setDebugSlowDownAnimation(boolean shouldSlowDownAnimation) {
    if (mDebugShouldSlowDownAnimation != shouldSlowDownAnimation) {
      mDebugShouldSlowDownAnimation = shouldSlowDownAnimation;
      invalidateSelf();
    }
  }

  boolean getDebugEnableRotation() {
    return mDebugShouldSlowDownAnimation;
  }

  boolean getDebugShowControlPoints() {
    return mDebugShouldSlowDownAnimation;
  }

  boolean getDebugSlowAnimation() {
    return mDebugShouldSlowDownAnimation;
  }

  private void maybeRotate(Canvas canvas, float degrees, float px, float py) {
    if (!mDebugShouldEnableRotation) {
      return;
    }
    canvas.rotate(degrees, px, py);
  }

  private void maybeDrawDebugControlPoints(Canvas canvas) {
    if (!mDebugShouldShowControlPoints) {
      return;
    }

    final float cpsRadius = mDebugControlPointRadius;
    final float endRadius = mDebugEndPointRadius;
    for (int i = 0; i < 3; i++) {
      mDebugControlPointPaint.setStyle(Paint.Style.FILL);
      canvas.drawCircle(end(i, 0), end(i, 1), endRadius, mDebugControlPointPaint);
      canvas.drawCircle(cp1(i, 0), cp1(i, 1), cpsRadius, mDebugControlPointPaint);
      canvas.drawCircle(cp2(i, 0), cp2(i, 1), cpsRadius, mDebugControlPointPaint);
      if (i + 1 == 3) {
        canvas.drawCircle(end(i + 1, 0), end(i + 1, 1), endRadius, mDebugControlPointPaint);
      }
      mDebugControlPointPaint.setStyle(Paint.Style.STROKE);
      canvas.drawLine(end(i, 0), end(i, 1), cp1(i, 0), cp1(i, 1), mDebugControlPointPaint);
      canvas.drawLine(cp1(i, 0), cp1(i, 1), cp2(i, 0), cp2(i, 1), mDebugControlPointPaint);
      canvas.drawLine(cp2(i, 0), cp2(i, 1), end(i + 1, 0), end(i + 1, 1), mDebugControlPointPaint);
    }
  }
}