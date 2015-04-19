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

    public void blue(View view) {
        Toast.makeText(this,"Blue",Toast.LENGTH_SHORT).show();
    }

    public void fail(View view) {
        Toast.makeText(this,"Fail",Toast.LENGTH_SHORT).show();
    }

    public void one(View view) {
        mc.show(0);
    }

    public void two(View view) {
        mc.show(1);
    }

    public void three(View view) {
        mc.show(2);
    }

    public void four(View view) {
        mc.show(3);
    }

}
