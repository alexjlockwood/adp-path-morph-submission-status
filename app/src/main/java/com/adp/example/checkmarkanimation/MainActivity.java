package com.adp.example.checkmarkanimation;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private CheckMarkView checkMarkView;
    private Button mRefresh;
    private Button mCheck;
    private Button mExclamation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkMarkView = (CheckMarkView) findViewById(R.id.check_mark_view);
        checkMarkView.setIconType(CheckMarkDrawable.REFRESH);
        mCheck = (Button) findViewById(R.id.check);
        mCheck.setOnClickListener(this);
        mExclamation = (Button) findViewById(R.id.exclamation);
        mExclamation.setOnClickListener(this);
        mRefresh = (Button) findViewById(R.id.refresh);
        mRefresh.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.check:
                checkMarkView.animateTo(CheckMarkDrawable.CHECK);
                break;
            case R.id.exclamation:
                checkMarkView.animateTo(CheckMarkDrawable.EXCLAMATION);
                break;
            case R.id.refresh:
                checkMarkView.animateTo(CheckMarkDrawable.REFRESH);
                break;
        }
    }
}
