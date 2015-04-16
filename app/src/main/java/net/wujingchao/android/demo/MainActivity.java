package net.wujingchao.android.demo;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import net.wujingchao.android.view.MultiCardMenu;


public class MainActivity extends ActionBarActivity {

    private final static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MultiCardMenu mc = (MultiCardMenu) findViewById(R.id.mc);
        mc.setOnDisplayOrHideListener(new MultiCardMenu.OnDisplayOrHideListener() {
            @Override
            public void onDisplay(int which) {
                Log.d(TAG,"onDisplay:" + which);
            }

            @Override
            public void onHide(int which) {
                Log.d(TAG,"onHide:" + which);
            }
        });
    }

    public void tranlate(View view) {
        Log.d(TAG,"Y:" + ViewHelper.getY(view));
        ObjectAnimator
                .ofFloat(view, "y", ViewHelper.getY(view), ViewHelper.getY(view) + 100)
                .setDuration(500)
                .start();
    }

    public void go(View view) {
        Toast.makeText(this,"gogo",Toast.LENGTH_SHORT).show();
    }
}
