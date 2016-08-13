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
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
    setMeasuredDimension(size, size);
  }
}
