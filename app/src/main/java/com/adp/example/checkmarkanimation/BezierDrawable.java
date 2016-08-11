package com.adp.example.checkmarkanimation;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
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

public class BezierDrawable extends Drawable {

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({CHECK, EXCLAMATION, REFRESH})
  public @interface IconType {}

  public static final int REFRESH = 0;
  public static final int CHECK = 1;
  public static final int EXCLAMATION = 2;

  private static final long ANIMATION_DURATION = 325;

  private final Path mPath = new Path();
  private final Path mArrowHeadPath = new Path();
  private final Path mExclamationDotPath = new Path();
  private final RectF mTotalBounds = new RectF();
  private final RectF mDrawBounds = new RectF();
  private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
  private float[][][] mExclamationDotPoints;

  private boolean mAnimatingFromCheck;
  private boolean mAnimatingToCheck;

  private final Paint mDebugBoundsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint mDebugControlPointsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final float mDebugControlPointRadius;
  private final float mDebugEndPointRadius;
  private boolean mDebugShouldEnableRotation = true;
  private boolean mDebugShouldShowControlPoints;
  private boolean mDebugShouldShowBounds;
  private boolean mDebugShouldSlowDownAnimation;

  public BezierDrawable(Context ctx) {
    mExclamationColor = ContextCompat.getColor(ctx, R.color.red);
    mCheckColor = ContextCompat.getColor(ctx, R.color.green);
    mRefreshColor = ContextCompat.getColor(ctx, R.color.blue);
    mStrokeWidth = ctx.getResources().getDimension(R.dimen.stroke_width);
    mPaint.setStrokeWidth(mStrokeWidth);
    setIconType(REFRESH);

    mDebugBoundsPaint.setStyle(Paint.Style.STROKE);
    mDebugBoundsPaint.setStrokeWidth(
        ctx.getResources().getDimension(R.dimen.debug_bounds_stroke_width));
    mDebugBoundsPaint.setColor(Color.BLACK);

    mDebugControlPointsPaint.setStyle(Paint.Style.FILL);
    mDebugControlPointsPaint.setColor(Color.BLACK);

    mDebugControlPointRadius = ctx.getResources().getDimension(R.dimen.debug_control_point_radius);
    mDebugEndPointRadius = ctx.getResources().getDimension(R.dimen.debug_end_point_radius);
  }

  @IconType
  public int getIconType() {
    return mCurrIconType;
  }

  public void setIconType(@IconType int iconType) {
    mCurrIconType = iconType;
    mPrevIconType = iconType;
    mProgress = 1f;
    mBackgroundColor = iconType == CHECK ? mCheckColor : iconType == EXCLAMATION ? mExclamationColor : mRefreshColor;
    invalidateSelf();
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
    final float dist = calcDistanceFromEndpoint(w / 2);
    final float exclamationPadding = h / 6;
    final float exclamationLongBarLength = h - 2.5f * mStrokeWidth - 2 * exclamationPadding;

    mEndPoints = new float[][][]{
        {{0, h / 2}, {w / 2, 0}, {w, h / 2}, {w / 2, h}}, // refresh end points
        {{w / 2 - r * cos(35), h / 2 - r * sin(35)}, {w / 2 - r / 2 * cos(35), h / 2 - r / 2 * sin(35)}, {w / 2, h / 2}, {w / 2 - r / 2 * cos(55), h / 2 + r / 2 * sin(55)}}, // check end points
        calcExclamationEndPoints(w / 2, exclamationLongBarLength, exclamationPadding), // exclamation end points
    };

    mControlPoints1 = new float[][][]{
        {{0, h / 2 - dist}, {w / 2 + dist, 0}, {w, h / 2 + dist}}, // refresh cp1
        {calcLongCheckBarCp(r * 5 / 6), calcLongCheckBarCp(r * 2 / 6), calcSmallCheckBarCp(r / 6)}, // check cp1
        calcExclamationCp1(w / 2, exclamationLongBarLength, exclamationPadding), // exclamation cp1
    };

    mControlPoints2 = new float[][][]{
        {{w / 2 - dist, 0}, {w, h / 2 - dist}, {w / 2 + dist, h}}, // refresh cp2
        {calcLongCheckBarCp(r * 4 / 6), calcLongCheckBarCp(r / 6), calcSmallCheckBarCp(r * 2 / 6)}, // check cp2
        calcExclamationCp2(w / 2, exclamationLongBarLength, exclamationPadding), // exclamation cp2
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
    // TODO: figure out nicer way to animate in/out the exclamation point mark?
    final float sw = mStrokeWidth;
    final float o = exclamationPadding;
    mExclamationDotPoints = new float[][][]{
        {mEndPoints[0][3], mEndPoints[0][3], mEndPoints[0][3], mEndPoints[0][3]}, // refresh mark points
        {mEndPoints[1][3], mEndPoints[1][3], mEndPoints[1][3], mEndPoints[1][3]}, // check mark points
        {{w / 2 - sw / 2, h - sw - o}, {w / 2 + sw / 2, h - sw - o}, {w / 2 + sw / 2, h - o}, {w / 2 - sw / 2, h - o}}, // exclamation mark points
    };
  }

  private static float calcDistanceFromEndpoint(float radius) {
    return radius * (sqrt(2) - 1) * 4 / 3;
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
        mDrawBounds.width() / 2 - r * cos(35),
        mDrawBounds.height() / 2 - r * sin(35),
    };
  }

  @Size(2)
  private float[] calcSmallCheckBarCp(float r) {
    return new float[]{
        mDrawBounds.width() / 2 - r * cos(55),
        mDrawBounds.height() / 2 + r * sin(55),
    };
  }

  @Override
  public void draw(Canvas canvas) {
    final float w = mDrawBounds.width();
    final float h = mDrawBounds.height();
    final float r = w / 2;

    mPaint.setStyle(Paint.Style.FILL);
    mPaint.setColor(mBackgroundColor);
    final float cornerRadius = mTotalBounds.width() / 2f;
    canvas.drawRoundRect(mTotalBounds, cornerRadius, cornerRadius, mPaint);

    canvas.save();
    canvas.translate(mInset, mInset);
    if (mAnimatingToCheck || mAnimatingFromCheck) {
      final float progress = mAnimatingToCheck ? mProgress : 1 - mProgress;
      canvas.translate(lerp(0, -(r / 2 * cos(55) - r / 4 * cos(35)), progress), 0); // center the check horizontally
      canvas.translate(0, lerp(0, r / 2 * cos(55), progress)); // center the check vertically
      if (mAnimatingToCheck) {
        rotate(canvas, lerp(0, -270, mProgress), w / 2, h / 2);
      } else {
        rotate(canvas, lerp(90, -360, mProgress), w / 2, h / 2);
      }
    } else {
      rotate(canvas, lerp(0, -360, mProgress), w / 2, h / 2);
    }

    mPath.rewind();
    mPath.moveTo(end(0, 0), end(0, 1));
    mPath.cubicTo(cp1(0, 0), cp1(0, 1), cp2(0, 0), cp2(0, 1), end(1, 0), end(1, 1));
    mPath.cubicTo(cp1(1, 0), cp1(1, 1), cp2(1, 0), cp2(1, 1), end(2, 0), end(2, 1));
    mPath.cubicTo(cp1(2, 0), cp1(2, 1), cp2(2, 0), cp2(2, 1), end(3, 0), end(3, 1));

    mArrowHeadPath.rewind();
    mArrowHeadPath.moveTo(arrowHeadPoint(0, 0), arrowHeadPoint(0, 1));
    mArrowHeadPath.lineTo(arrowHeadPoint(1, 0), arrowHeadPoint(1, 1));
    mArrowHeadPath.lineTo(arrowHeadPoint(2, 0), arrowHeadPoint(2, 1));
    mArrowHeadPath.lineTo(arrowHeadPoint(0, 0), arrowHeadPoint(0, 1));

    mExclamationDotPath.rewind();
    mExclamationDotPath.moveTo(exclamationDotPoint(0, 0), exclamationDotPoint(0, 1));
    mExclamationDotPath.lineTo(exclamationDotPoint(1, 0), exclamationDotPoint(1, 1));
    mExclamationDotPath.lineTo(exclamationDotPoint(2, 0), exclamationDotPoint(2, 1));
    mExclamationDotPath.lineTo(exclamationDotPoint(3, 0), exclamationDotPoint(3, 1));
    mExclamationDotPath.lineTo(exclamationDotPoint(0, 0), exclamationDotPoint(0, 1));

    mPaint.setColor(Color.WHITE);
    canvas.drawPath(mArrowHeadPath, mPaint);
    canvas.drawPath(mExclamationDotPath, mPaint);
    mPaint.setStyle(Paint.Style.STROKE);
    canvas.drawPath(mPath, mPaint);

    if (mDebugShouldShowBounds) {
      drawDebugBounds(canvas);
    }

    if (mDebugShouldShowControlPoints) {
      drawDebugControlPoints(canvas);
    }

    canvas.restore();
  }

  private void rotate(Canvas canvas, float degrees, float px, float py) {
    if (mDebugShouldEnableRotation) {
      canvas.rotate(degrees, px, py);
    }
  }

  private void drawDebugBounds(Canvas canvas) {
    canvas.drawRect(mDrawBounds, mDebugBoundsPaint);
  }

  private void drawDebugControlPoints(Canvas canvas) {
    // Draw cp1s and cp2s.
    for (int i = 0; i < 3; i++) {
      canvas.drawCircle(cp1(i, 0), cp1(i, 1), mDebugControlPointRadius, mDebugControlPointsPaint);
      canvas.drawCircle(cp2(i, 0), cp2(i, 1), mDebugControlPointRadius, mDebugControlPointsPaint);
    }

    // Draw starting and ending points.
    for (int i = 0; i < 4; i++) {
      canvas.drawCircle(end(i, 0), end(i, 1), mDebugEndPointRadius, mDebugControlPointsPaint);
    }
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

  public void animateTo(@IconType int iconType) {
    if (iconType != mCurrIconType) {
      startAnimation(iconType);
    }
  }

  private void startAnimation(@IconType final int nextIconType) {
    if (nextIconType == mCurrIconType) {
      return;
    }

    mAnimatingFromCheck = mCurrIconType == CHECK;
    mAnimatingToCheck = nextIconType == CHECK;
    mPrevIconType = mCurrIconType;
    mCurrIconType = nextIconType;

    long duration = ANIMATION_DURATION;
    if (mDebugShouldSlowDownAnimation) {
      duration *= 10;
    }

    final int startBgColor = mBackgroundColor;
    final int endBgColor =
        nextIconType == CHECK ? mCheckColor : nextIconType == EXCLAMATION ? mExclamationColor : mRefreshColor;

    final ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
    anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        final float newProgress = animation.getAnimatedFraction();
        final int newBgColor = (Integer) argbEvaluator.evaluate(mProgress, startBgColor, endBgColor);
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

  /**
   * Linear interpolate between a and b with parameter t.
   */
  private static float lerp(float a, float b, float t) {
    return a + (b - a) * t;
  }

  private static float cos(float degrees) {
    return (float) Math.cos(Math.toRadians(degrees));
  }

  private static float sin(float degrees) {
    return (float) Math.sin(Math.toRadians(degrees));
  }

  private static float sqrt(float value) {
    return (float) Math.sqrt(value);
  }

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

  void setDebugShowBounds(boolean shouldShowBounds) {
    if (mDebugShouldShowBounds != shouldShowBounds) {
      mDebugShouldShowBounds = shouldShowBounds;
      invalidateSelf();
    }
  }

  void setDebugSlowDownAnimation(boolean shouldSlowDownAnimation) {
    if (mDebugShouldSlowDownAnimation != shouldSlowDownAnimation) {
      mDebugShouldSlowDownAnimation = shouldSlowDownAnimation;
      invalidateSelf();
    }
  }
}