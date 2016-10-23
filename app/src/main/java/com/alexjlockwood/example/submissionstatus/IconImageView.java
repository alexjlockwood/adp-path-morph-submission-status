package com.alexjlockwood.example.submissionstatus;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

class IconImageView extends AppCompatImageView {
  private static final int[] CHECK_STATE_SET =
      {R.attr.state_check, -R.attr.state_refresh, -R.attr.state_exclamation};
  private static final int[] EXCLAMATION_STATE_SET =
      {R.attr.state_exclamation, -R.attr.state_refresh, -R.attr.state_check};
  private static final int[] REFRESH_STATE_SET =
      {R.attr.state_refresh, -R.attr.state_exclamation, -R.attr.state_check};

  @IconType
  private int iconType;

  public IconImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setIconType(@IconType int iconType) {
    this.iconType = iconType;
    final int[] stateSet =
        iconType == IconType.REFRESH
            ? REFRESH_STATE_SET : iconType == IconType.EXCLAMATION
            ? EXCLAMATION_STATE_SET : CHECK_STATE_SET;
    setImageState(stateSet, true);
  }

  @IconType
  public int getIconType() {
    return iconType;
  }

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({IconType.REFRESH, IconType.CHECK, IconType.EXCLAMATION})
  public @interface IconType {
    int REFRESH = 0;
    int CHECK = 1;
    int EXCLAMATION = 2;
  }
}
