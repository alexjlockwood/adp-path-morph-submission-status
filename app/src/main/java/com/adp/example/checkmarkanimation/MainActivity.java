package com.adp.example.checkmarkanimation;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  private BezierDrawable mBezierDrawable;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

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
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_enable_rotation) {
      item.setChecked(!item.isChecked());
      mBezierDrawable.setDebugEnableRotation(item.isChecked());
      return true;
    }
    if (item.getItemId() == R.id.action_show_control_points) {
      item.setChecked(!item.isChecked());
      mBezierDrawable.setDebugShowControlPoints(item.isChecked());
      return true;
    }
    if (item.getItemId() == R.id.action_show_bounds) {
      item.setChecked(!item.isChecked());
      mBezierDrawable.setDebugShowBounds(item.isChecked());
      return true;
    }
    if (item.getItemId() == R.id.action_slow_down_animation) {
      item.setChecked(!item.isChecked());
      mBezierDrawable.setDebugSlowDownAnimation(item.isChecked());
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
