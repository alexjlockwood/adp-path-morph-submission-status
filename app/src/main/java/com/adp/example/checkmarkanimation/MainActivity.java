package com.adp.example.checkmarkanimation;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private CheckMarkDrawable mCheckMarkDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCheckMarkDrawable = new CheckMarkDrawable(getResources());
        mCheckMarkDrawable.setIconType(CheckMarkDrawable.REFRESH);
        View checkMarkView = findViewById(R.id.check_mark_view);
        checkMarkView.setBackgroundDrawable(mCheckMarkDrawable);
        final Button check = (Button) findViewById(R.id.check);
        check.setOnClickListener(this);
        final Button exclamation = (Button) findViewById(R.id.exclamation);
        exclamation.setOnClickListener(this);
        final Button refresh = (Button) findViewById(R.id.refresh);
        refresh.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.check:
                mCheckMarkDrawable.animateTo(CheckMarkDrawable.CHECK);
                break;
            case R.id.exclamation:
                mCheckMarkDrawable.animateTo(CheckMarkDrawable.EXCLAMATION);
                break;
            case R.id.refresh:
                mCheckMarkDrawable.animateTo(CheckMarkDrawable.REFRESH);
                break;
        }
    }
}
