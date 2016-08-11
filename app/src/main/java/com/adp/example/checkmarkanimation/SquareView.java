package com.adp.example.checkmarkanimation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class SquareView extends View {

  public SquareView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    final int size;
    if (widthMode == MeasureSpec.EXACTLY && widthSize > 0) {
      size = widthSize;
    } else if (heightMode == MeasureSpec.EXACTLY && heightSize > 0) {
      size = heightSize;
    } else {
      size = widthSize < heightSize ? widthSize : heightSize;
    }

    final int finalMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
    super.onMeasure(finalMeasureSpec, finalMeasureSpec);
  }
}
