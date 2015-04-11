package net.wujingchao.android.demo;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;


public class MainActivity extends ActionBarActivity {

    private final static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void tranlate(View view) {
        Log.d(TAG,"Y:" + ViewHelper.getY(view));
        ObjectAnimator
                .ofFloat(view, "y", ViewHelper.getY(view), ViewHelper.getY(view) + 100)
                .setDuration(500)
                .start();
    }
}
