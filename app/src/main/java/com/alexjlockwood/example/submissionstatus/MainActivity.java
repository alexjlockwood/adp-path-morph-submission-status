package com.alexjlockwood.example.submissionstatus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * Main activity for the sample app.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
  private static final String STATE_ENABLE_ROTATION = "state_debug_rotation";
  private static final String STATE_SHOW_CPS = "state_debug_cps";
  private static final String STATE_SLOW_ANIMATION = "state_debug_slow_animation;";
  private static final String STATE_ICON_TYPE = "state_icon_type";

  private SubmissionStatusDrawable mDrawable;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    //noinspection ConstantConditions
    getSupportActionBar().setSubtitle(R.string.action_bar_subtitle);

    mDrawable = new SubmissionStatusDrawable(this);

    if (savedInstanceState != null) {
      //noinspection WrongConstant
      mDrawable.setIconType(savedInstanceState.getInt(STATE_ICON_TYPE));
      mDrawable.setDebugEnableRotation(savedInstanceState.getBoolean(STATE_ENABLE_ROTATION));
      mDrawable.setDebugShowControlPoints(savedInstanceState.getBoolean(STATE_SHOW_CPS));
      mDrawable.setDebugSlowDownAnimation(savedInstanceState.getBoolean(STATE_SLOW_ANIMATION));
    }

    //noinspection deprecation
    findViewById(R.id.submission_status_view).setBackgroundDrawable(mDrawable);
    findViewById(R.id.returned).setOnClickListener(this);
    findViewById(R.id.done).setOnClickListener(this);
    findViewById(R.id.late).setOnClickListener(this);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(STATE_ENABLE_ROTATION, mDrawable.getDebugEnableRotation());
    outState.putBoolean(STATE_SHOW_CPS, mDrawable.getDebugShowControlPoints());
    outState.putBoolean(STATE_SLOW_ANIMATION, mDrawable.getDebugSlowAnimation());
    outState.putInt(STATE_ICON_TYPE, mDrawable.getIconType());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.action_enable_rotation).setChecked(mDrawable.getDebugEnableRotation());
    menu.findItem(R.id.action_show_cps).setChecked(mDrawable.getDebugShowControlPoints());
    menu.findItem(R.id.action_slow_animation).setChecked(mDrawable.getDebugSlowAnimation());
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.isCheckable()) {
      item.setChecked(!item.isChecked());
    }
    if (item.getItemId() == R.id.action_enable_rotation) {
      mDrawable.setDebugEnableRotation(item.isChecked());
      return true;
    }
    if (item.getItemId() == R.id.action_show_cps) {
      mDrawable.setDebugShowControlPoints(item.isChecked());
      return true;
    }
    if (item.getItemId() == R.id.action_slow_animation) {
      mDrawable.setDebugSlowDownAnimation(item.isChecked());
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.done:
        mDrawable.setIconType(SubmissionStatusDrawable.DONE);
        break;
      case R.id.late:
        mDrawable.setIconType(SubmissionStatusDrawable.LATE);
        break;
      case R.id.returned:
        mDrawable.setIconType(SubmissionStatusDrawable.RETURNED);
        break;
    }
  }
}
