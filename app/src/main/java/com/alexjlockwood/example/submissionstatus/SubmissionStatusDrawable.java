package com.alexjlockwood.example.submissionstatus;

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
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.v4.content.ContextCompat;
import android.view.animation.DecelerateInterpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.alexjlockwood.example.submissionstatus.MathUtils.lerp;
import static com.alexjlockwood.example.submissionstatus.MathUtils.sqrt;

/**
 * A custom drawable that animates between a done (check), late (exclamation mark), and returned
 * (refresh) icon state. The morphing animation works by translating the control points and end
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

  private static final ArgbEvaluator ARGB_EVALUATOR = new ArgbEvaluator();

  // Precomputed trig constants.
  private static final float COS55 = MathUtils.cos(55);
  private static final float COS35 = MathUtils.cos(35);
  private static final float SIN55 = MathUtils.sin(55);
  private static final float SIN35 = MathUtils.sin(35);

  // Multiply this constant by R to approximate the distance between the control
  // points and end points of a circle with radius R.
  private static final float FOUR_SPLINE_MAGIC_NUMBER = (sqrt(2) - 1) * 4 / 3;

  private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path mArrowHeadPath = new Path();
  private final Path mExclamationDotPath = new Path();
  private final Path mIconPath = new Path();
  private final float mIconStrokeWidth;

  private final RectF mDrawBounds = new RectF();
  private float mInsets;

  @ColorInt private final int mIconColor;
  @ColorInt private final int mLateColor;
  @ColorInt private final int mDoneColor;
  @ColorInt private final int mReturnedColor;
  @ColorInt private int mBackgroundColor;

  @IconType private int mPrevIconType;
  @IconType private int mCurrIconType;
  private boolean mAnimatingFromDone;
  private boolean mAnimatingToDone;

  // The current progress of the animation.
  @FloatRange(from = 0f, to = 1f) private float mProgress;

  private float[][][] mEndPoints;
  private float[][][] mControlPoints1;
  private float[][][] mControlPoints2;
  private float[][][] mArrowHeadPoints;
  private float[][][] mExclamationDotPoints;

  // Debugging stuff.
  private final float mDebugControlPointRadius;
  private final float mDebugEndPointRadius;
  private final float mDebugStrokeWidth;
  @ColorInt private final int mDebugStrokeColor;
  private final int mDebugAnimationDuration;
  private boolean mDebugShouldEnableRotation = true;
  private boolean mDebugShouldShowControlPoints;
  private boolean mDebugShouldSlowDownAnimation;

  public SubmissionStatusDrawable(Context ctx) {
    final Resources res = ctx.getResources();

    mIconStrokeWidth = res.getDimension(R.dimen.stroke_width);
    mIconColor = Color.WHITE;
    mReturnedColor = ContextCompat.getColor(ctx, R.color.quantum_vanillablue500);
    mDoneColor = ContextCompat.getColor(ctx, R.color.quantum_vanillagreen500);
    mLateColor = ContextCompat.getColor(ctx, R.color.quantum_vanillared500);

    // Debugging stuff.
    mDebugControlPointRadius = res.getDimension(R.dimen.debug_control_point_radius);
    mDebugEndPointRadius = res.getDimension(R.dimen.debug_end_point_radius);
    mDebugAnimationDuration = ANIMATION_DURATION * 5;
    mDebugStrokeWidth = res.getDimension(R.dimen.debug_bounds_stroke_width);
    mDebugStrokeColor = Color.BLACK;

    setIconType(RETURNED);
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);

    final float totalWidth = getBounds().width();
    final float totalRadius = totalWidth / 2;
    mInsets = (totalWidth - sqrt(2 * totalRadius * totalRadius)) / 2;
    mDrawBounds.set(0, 0, bounds.width() - 2 * mInsets, bounds.height() - 2 * mInsets);

    final float w = mDrawBounds.width();
    final float h = mDrawBounds.height();
    final float r = w / 2;

    // Please forgive me for these variable names... :D
    final float sw = mIconStrokeWidth; // icon stroke width
    final float ep = h / 6; // exclamation padding
    final float elbl = h - 2.5f * sw - 2 * ep; // exclamation long bar length

    mEndPoints = new float[][][]{
        {
            {0, h / 2},
            {w / 2, 0},
            {w, h / 2},
            {w / 2, h},
        }, // returned end points
        {
            {w / 2 - r * COS35, h / 2 - r * SIN35},
            {w / 2 - r / 2 * COS35, h / 2 - r / 2 * SIN35},
            {w / 2, h / 2}, {w / 2 - r / 2 * COS55, h / 2 + r / 2 * SIN55},
        }, // done end points
        {
            {w / 2, ep},
            {w / 2, ep + elbl / 3},
            {w / 2, ep + 2 * elbl / 3},
            {w / 2, ep + elbl},
        } // late end points
    };

    mControlPoints1 = new float[][][]{
        {
            {0, h / 2 - r * FOUR_SPLINE_MAGIC_NUMBER},
            {w / 2 + r * FOUR_SPLINE_MAGIC_NUMBER, 0},
            {w, h / 2 + r * FOUR_SPLINE_MAGIC_NUMBER},
        }, // returned cp1s
        {
            {w / 2 - (r * 5 / 6) * COS35, h / 2 - (r * 5 / 6) * SIN35},
            {w / 2 - (r * 2 / 6) * COS35, h / 2 - (r * 2 / 6) * SIN35},
            {w / 2 - (r / 6) * COS55, h / 2 + (r / 6) * SIN55},
        }, // done cp1s
        {
            {w / 2, ep + elbl / 9},
            {w / 2, ep + 4 * elbl / 9},
            {w / 2, ep + 7 * elbl / 9},
        }, // late cp1s
    };

    mControlPoints2 = new float[][][]{
        {
            {w / 2 - r * FOUR_SPLINE_MAGIC_NUMBER, 0},
            {w, h / 2 - r * FOUR_SPLINE_MAGIC_NUMBER},
            {w / 2 + r * FOUR_SPLINE_MAGIC_NUMBER, h},
        }, // returned cp2s
        {
            {w / 2 - (r * 4 / 6) * COS35, h / 2 - (r * 4 / 6) * SIN35},
            {w / 2 - (r / 6) * COS35, h / 2 - (r / 6) * SIN35},
            {w / 2 - (r * 2 / 6) * COS55, h / 2 + (r * 2 / 6) * SIN55},
        }, // done cp2s
        {
            {w / 2, ep + 2 * elbl / 9},
            {w / 2, ep + 5 * elbl / 9},
            {w / 2, ep + 8 * elbl / 9},
        }, // late cp2s
    };

    final float arrowHeadSize = 4 * mIconStrokeWidth;
    final float arrowHeadHeight = arrowHeadSize * MathUtils.cos(30);
    final float returnedEndX = mEndPoints[0][0][0];
    final float returnedEndY = mEndPoints[0][0][1] - 1; // Subtract one pixel to ensure arrow head and returned arc connect.

    mArrowHeadPoints = new float[][][]{
        {
            {returnedEndX, returnedEndY + arrowHeadHeight},
            {returnedEndX - arrowHeadSize / 2, returnedEndY},
            {returnedEndX + arrowHeadSize / 2, returnedEndY},
        }, // returned arrow head points
        {mEndPoints[1][0], mEndPoints[1][0], mEndPoints[1][0]}, // done arrow head points
        {mEndPoints[2][0], mEndPoints[2][0], mEndPoints[2][0]}, // late arrow head points
    };

    // TODO: add extra padding above and below the exclamation point mark
    // TODO: figure out nicer way to animate in/out the exclamation point mark
    mExclamationDotPoints = new float[][][]{
        {mEndPoints[0][3], mEndPoints[0][3], mEndPoints[0][3], mEndPoints[0][3]}, // returned exclamation dot points
        {mEndPoints[1][3], mEndPoints[1][3], mEndPoints[1][3], mEndPoints[1][3]}, // done exclamation dot points
        {
            {w / 2 - sw / 2, h - sw - ep},
            {w / 2 + sw / 2, h - sw - ep},
            {w / 2 + sw / 2, h - ep},
            {w / 2 - sw / 2, h - ep},
        }, // late exclamation dot points
    };
  }

  @Override
  public void draw(Canvas canvas) {
    mPaint.setStrokeWidth(mIconStrokeWidth);
    mPaint.setColor(mBackgroundColor);
    mPaint.setStyle(Paint.Style.FILL);
    final float radius = Math.min(getBounds().width(), getBounds().height()) / 2f;
    canvas.drawCircle(getBounds().centerX(), getBounds().centerY(), radius, mPaint);

    // TODO: figure out what to do if the w != h (probably should just take the minimum?)
    final float w = mDrawBounds.width();
    final float h = mDrawBounds.height();
    final float r = w / 2;

    canvas.save();
    canvas.translate(mInsets, mInsets);

    if (mAnimatingToDone || mAnimatingFromDone) {
      final float translateProgress = mAnimatingToDone ? mProgress : 1 - mProgress;
      canvas.translate(
          lerp(0, -(r / 2 * COS55 - r / 4 * COS35), translateProgress),
          lerp(0, r / 2 * COS55, translateProgress)); // center the check icon
      if (mAnimatingToDone) {
        maybeRotate(canvas, lerp(0, -270, mProgress), w / 2, h / 2);
      } else {
        maybeRotate(canvas, lerp(90, -360, mProgress), w / 2, h / 2);
      }
    } else {
      maybeRotate(canvas, lerp(0, -360, mProgress), w / 2, h / 2);
    }

    // Draw the arrow head displayed by the returned icon.
    mArrowHeadPath.rewind();
    mArrowHeadPath.moveTo(arrowx(0), arrowy(0));
    mArrowHeadPath.lineTo(arrowx(1), arrowy(1));
    mArrowHeadPath.lineTo(arrowx(2), arrowy(2));
    mArrowHeadPath.close();

    mPaint.setColor(mIconColor);
    mPaint.setStyle(Paint.Style.FILL);
    canvas.drawPath(mArrowHeadPath, mPaint);

    // Draw the exclamation dot displayed by the late icon.
    mExclamationDotPath.rewind();
    mExclamationDotPath.moveTo(dotx(0), doty(0));
    mExclamationDotPath.lineTo(dotx(1), doty(1));
    mExclamationDotPath.lineTo(dotx(2), doty(2));
    mExclamationDotPath.lineTo(dotx(3), doty(3));
    mExclamationDotPath.close();

    mPaint.setColor(mIconColor);
    mPaint.setStyle(Paint.Style.FILL);
    canvas.drawPath(mExclamationDotPath, mPaint);

    // Draw the three cubic bezier curves to form the main icon.
    mIconPath.rewind();
    mIconPath.moveTo(endx(0), endy(0));
    mIconPath.cubicTo(cp1x(0), cp1y(0), cp2x(0), cp2y(0), endx(1), endy(1));
    mIconPath.cubicTo(cp1x(1), cp1y(1), cp2x(1), cp2y(1), endx(2), endy(2));
    mIconPath.cubicTo(cp1x(2), cp1y(2), cp2x(2), cp2y(2), endx(3), endy(3));

    mPaint.setColor(mIconColor);
    mPaint.setStyle(Paint.Style.STROKE);
    canvas.drawPath(mIconPath, mPaint);

    maybeDrawDebugControlPoints(canvas);

    canvas.restore();
  }

  /* Linear interpolation helper methods. */

  private float cp1x(int pos) {
    return lerpx(mControlPoints1, pos);
  }

  private float cp1y(int pos) {
    return lerpy(mControlPoints1, pos);
  }

  private float cp2x(int pos) {
    return lerpx(mControlPoints2, pos);
  }

  private float cp2y(int pos) {
    return lerpy(mControlPoints2, pos);
  }

  private float endx(int pos) {
    return lerpx(mEndPoints, pos);
  }

  private float endy(int pos) {
    return lerpy(mEndPoints, pos);
  }

  private float arrowx(int pos) {
    return lerpx(mArrowHeadPoints, pos);
  }

  private float arrowy(int pos) {
    return lerpy(mArrowHeadPoints, pos);
  }

  private float dotx(int pos) {
    return lerpx(mExclamationDotPoints, pos);
  }

  private float doty(int pos) {
    return lerpy(mExclamationDotPoints, pos);
  }

  private float lerpx(float[][][] points, int pos) {
    return lerp(points[mPrevIconType][pos][0], points[mCurrIconType][pos][0], mProgress);
  }

  private float lerpy(float[][][] points, int pos) {
    return lerp(points[mPrevIconType][pos][1], points[mCurrIconType][pos][1], mProgress);
  }

  /* Public API (setting the icon type with and without animation). */

  /** Sets a new icon state without playing an animation. */
  public void setIconType(@IconType int iconType) {
    mCurrIconType = iconType;
    mPrevIconType = iconType;
    mProgress = 1f;
    mBackgroundColor = getBackgroundColorForIconType(iconType);
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

    mAnimatingFromDone = mCurrIconType == DONE;
    mAnimatingToDone = nextIconType == DONE;
    mPrevIconType = mCurrIconType;
    mCurrIconType = nextIconType;

    final int startBgColor = mBackgroundColor;
    final int endBgColor = getBackgroundColorForIconType(nextIconType);

    final ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
    anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        final float newProgress = animation.getAnimatedFraction();
        final int newBgColor =
            (Integer) ARGB_EVALUATOR.evaluate(mProgress, startBgColor, endBgColor);
        if (mProgress != newProgress || mBackgroundColor != newBgColor) {
          mProgress = newProgress;
          mBackgroundColor = newBgColor;
          invalidateSelf();
        }
      }
    });
    anim.setDuration(mDebugShouldSlowDownAnimation ? mDebugAnimationDuration : ANIMATION_DURATION);
    anim.setInterpolator(new DecelerateInterpolator());
    anim.start();
  }

  @ColorInt
  private int getBackgroundColorForIconType(@IconType int iconType) {
    return iconType == DONE ? mDoneColor : iconType == LATE ? mLateColor : mReturnedColor;
  }

  /* Overridden Drawable methods. */

  @Override
  public void setAlpha(int alpha) {
    if (mPaint.getAlpha() != alpha) {
      mPaint.setAlpha(alpha);
      invalidateSelf();
    }
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    mPaint.setColorFilter(cf);
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
    return mDebugShouldEnableRotation;
  }

  boolean getDebugShowControlPoints() {
    return mDebugShouldShowControlPoints;
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

    mPaint.setStrokeWidth(mDebugStrokeWidth);
    mPaint.setColor(mDebugStrokeColor);

    final float cpsRadius = mDebugControlPointRadius;
    final float endRadius = mDebugEndPointRadius;
    for (int i = 0; i < 3; i++) {
      mPaint.setStyle(Paint.Style.FILL);
      canvas.drawCircle(endx(i), endy(i), endRadius, mPaint);
      canvas.drawCircle(cp1x(i), cp1y(i), cpsRadius, mPaint);
      canvas.drawCircle(cp2x(i), cp2y(i), cpsRadius, mPaint);
      if (i + 1 == 3) {
        canvas.drawCircle(endx(i + 1), endy(i + 1), endRadius, mPaint);
      }
      mPaint.setStyle(Paint.Style.STROKE);
      canvas.drawLine(endx(i), endy(i), cp1x(i), cp1y(i), mPaint);
      canvas.drawLine(cp1x(i), cp1y(i), cp2x(i), cp2y(i), mPaint);
      canvas.drawLine(cp2x(i), cp2y(i), endx(i + 1), endy(i + 1), mPaint);
    }
  }
}