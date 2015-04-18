package net.wujingchao.android.demo;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import net.wujingchao.android.view.MultiCardMenu;


public class MainActivity extends ActionBarActivity {

    private final static String TAG = "MainActivity";

    MultiCardMenu mc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mc = (MultiCardMenu) findViewById(R.id.multi_card_menu);
        mc.setOnDisplayOrHideListener(new MultiCardMenu.OnDisplayOrHideListener() {
            @Override
            public void onDisplay(int which) {
                Log.d(TAG,"onDisplay:" + which);
            }

            @Override
            public void onHide(int which) {
                Log.d(TAG, "onHide:" + which);
            }
        });
    }


    public void go(View view) {
        Toast.makeText(this,"getDisplayingCard:" + mc.getDisplayingCard(),Toast.LENGTH_SHORT).show();
    }

    int i = -1;

    public void show(View view) {
        i ++;
        if(mc.getChildCount() <= i) i = 0;
        Log.d(TAG,"index:" + i);
        mc.show(i);
    }

    public void hide(View view) {
        mc.hide(mc.getDisplayingCard());
    }

    @Override
    public void onBackPressed() {
        if(mc.isDisplaying()) {
            mc.hide(mc.getDisplayingCard());
        }else {
            super.onBackPressed();
        }
    }
}
