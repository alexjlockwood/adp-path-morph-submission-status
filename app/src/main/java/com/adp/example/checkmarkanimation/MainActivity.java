package com.adp.example.checkmarkanimation;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
  private static final String STATE_DEBUG_ROTATION = "state_debug_rotation";
  private static final String STATE_DEBUG_CPS = "state_debug_cps";
  private static final String STATE_DEBUG_BOUNDS = "state_debug_bounds";
  private static final String STATE_DEBUG_SLOW_ANIMATION = "state_debug_slow_animation;";

  private boolean mDebugRotation = true;
  private boolean mDebugCps;
  private boolean mDebugBounds;
  private boolean mDebugAnimation;

  private BezierDrawable mBezierDrawable;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    getSupportActionBar().setSubtitle(R.string.action_bar_subtitle);

    mBezierDrawable = new BezierDrawable(this);
    mBezierDrawable.setIconType(BezierDrawable.REFRESH);

    //noinspection deprecation
    findViewById(R.id.check_mark_view).setBackgroundDrawable(mBezierDrawable);

    final Button check = (Button) findViewById(R.id.check);
    check.setOnClickListener(this);

    final Button exclamation = (Button) findViewById(R.id.exclamation);
    exclamation.setOnClickListener(this);

    final Button refresh = (Button) findViewById(R.id.refresh);
    refresh.setOnClickListener(this);

    if (savedInstanceState != null) {
      mDebugRotation = savedInstanceState.getBoolean(STATE_DEBUG_ROTATION);
      mDebugCps = savedInstanceState.getBoolean(STATE_DEBUG_CPS);
      mDebugBounds = savedInstanceState.getBoolean(STATE_DEBUG_BOUNDS);
      mDebugAnimation = savedInstanceState.getBoolean(STATE_DEBUG_SLOW_ANIMATION);
    }

    mBezierDrawable.setDebugEnableRotation(mDebugRotation);
    mBezierDrawable.setDebugShowControlPoints(mDebugCps);
    mBezierDrawable.setDebugShowBounds(mDebugBounds);
    mBezierDrawable.setDebugSlowDownAnimation(mDebugAnimation);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(STATE_DEBUG_ROTATION, mDebugRotation);
    outState.putBoolean(STATE_DEBUG_CPS, mDebugCps);
    outState.putBoolean(STATE_DEBUG_BOUNDS, mDebugBounds);
    outState.putBoolean(STATE_DEBUG_SLOW_ANIMATION, mDebugAnimation);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.action_enable_rotation).setChecked(mDebugRotation);
    menu.findItem(R.id.action_show_control_points).setChecked(mDebugCps);
    menu.findItem(R.id.action_show_bounds).setChecked(mDebugBounds);
    menu.findItem(R.id.action_show_bounds).setVisible(false);
    menu.findItem(R.id.action_slow_down_animation).setChecked(mDebugAnimation);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_enable_rotation) {
      mDebugRotation = !item.isChecked();
      item.setChecked(mDebugRotation);
      mBezierDrawable.setDebugEnableRotation(mDebugRotation);
      return true;
    }
    if (item.getItemId() == R.id.action_show_control_points) {
      mDebugCps = !item.isChecked();
      item.setChecked(mDebugCps);
      mBezierDrawable.setDebugShowControlPoints(mDebugCps);
      return true;
    }
    if (item.getItemId() == R.id.action_show_bounds) {
      mDebugBounds = !item.isChecked();
      item.setChecked(mDebugBounds);
      mBezierDrawable.setDebugShowBounds(mDebugBounds);
      return true;
    }
    if (item.getItemId() == R.id.action_slow_down_animation) {
      mDebugAnimation = !item.isChecked();
      item.setChecked(mDebugAnimation);
      mBezierDrawable.setDebugSlowDownAnimation(mDebugAnimation);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.check:
        mBezierDrawable.animateTo(BezierDrawable.CHECK);
        break;
      case R.id.exclamation:
        mBezierDrawable.animateTo(BezierDrawable.EXCLAMATION);
        break;
      case R.id.refresh:
        mBezierDrawable.animateTo(BezierDrawable.REFRESH);
        break;
    }
  }
}
