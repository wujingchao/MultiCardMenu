package net.wujingchao.android.demo;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import net.wujingchao.android.view.MultiCardMenu;
import java.util.LinkedList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    MultiCardMenu mc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
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

            @Override
            public void onTouchCard(int which) {
                Log.d(TAG,"onTouchCard:" + which);
            }
        });
        CheckBox fade = (CheckBox) findViewById(R.id.fade);
        fade.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mc.setFade(isChecked);
            }
        });
        CheckBox boundary = (CheckBox) findViewById(R.id.boundary);
        boundary.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mc.setBoundary(isChecked);
            }
        });
//        int [] imgRes = {R.drawable.ent,R.drawable.qa};
        int [] imgRes = {R.drawable.nuan1,R.drawable.nuan2,R.drawable.ic_launcher,
                R.drawable.nuan1,R.drawable.nuan2,R.drawable.ic_launcher};
        final List<ImageView> imageViews = new LinkedList<>();
        ImageView imageView;
        for(Integer i :imgRes) {
            imageView = (ImageView) LayoutInflater.from(this).inflate(R.layout.image_item,null);
            imageView.setImageResource(i);
            imageViews.add(imageView);
        }
        ViewPager mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return imageViews.size();
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView(imageViews.get(position));
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                container.addView(imageViews.get(position));
                return imageViews.get(position);
            }
        });
        ListView listView = (ListView) findViewById(R.id.lv);
        listView.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,data));

    }

    String data [] = {"mother"		,
            "passion ",
            "smile 	"			,
            "love 		"	,
            "eternity 	"		,
            "fantastic "		,
            "destiny "			,
            "freedom "			,
            "liberty 	"		,
            "tranquility "		,
            "peace 	"		,
            "blossom "			,
            "sunshine "			,
            "sweetheart "		,
            "gorgeous "			,
            "cherish 	"		,
            "enthusiasm" 		,
            "hope "				,
            "grace "			,
            "rainbow "			,
            "blue 	"			,
            "sunflower "		,
            "twinkle "			,
            "serendipity" 		,
            "bliss 		"		,
            "lullaby 		"	,
            "sophisticated 	"	,
            "renaissance 	"	,
            "cute 			"	,
            "cosy 			",
            "butterfly ",
            "galaxy ",
            "hilarious ",
            "moment",
            "extravaganza ",
            "aqua ",
            "sentiment ",
            "cosmopolitan ",
            "bubble ",
            "pumpkin",
            "banana",
            "lollipop ",
            "if 　",
            "bumblebee ",
            "giggle ",
            "paradox ",
            "peek-a-boo ",
            "umbrella ",
            "kangaroo ",
            "flabbergasted" ,
            "hippopotamus ",
            "gothic ",
            "coconut ",
            "smashing ",
            "whoops ",
            "tickle ",
            "loquacious ",
            "flip-flop ",
            "smithereens",
            "hi" ,
            "gazebo",
            "hiccup",
            "hodgepodge" ,
            "shipshape",
            "explosion" ,
            "fuselage",
            "zing",
            "gum",
            "hen-night "};

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
