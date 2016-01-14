[![Build Status](https://travis-ci.org/wujingchao/MultiCardMenu.svg?branch=master)](https://travis-ci.org/wujingchao/MultiCardMenu)
[![Android Gems](http://www.android-gems.com/badge/wujingchao/MultiCardMenu.svg?branch=master)](http://www.android-gems.com/lib/wujingchao/MultiCardMenu)

# MultiCardMenu
 A multicard menu that can open and close with animation on android,require API level >= 11

## <a href="https://raw.githubusercontent.com/wujingchao/MultiCardMenu/master/demo.apk">Demo</a>

   <img src="https://raw.githubusercontent.com/wujingchao/MultiCardMenu/master/multi_card_menu_demo_static.png"  width="401" heigit="638" alt="Screenshot"/>


<br/>
<p>
   <img src="https://raw.githubusercontent.com/wujingchao/MultiCardMenu/master/multi_card_menu_demo.gif"  width="401" heigit="638" alt="Screenshot"/>
</p>

<br/>

##Usage
	
	   <net.wujingchao.android.view.MultiCardMenu
	        xmlns:simple="http://schemas.android.com/apk/res-auto"
	        android:id="@+id/multi_card_menu"
	        android:layout_width="match_parent"
	        android:layout_height="match_parent"
	        simple:background_layout="@layout/background_view"
	        simple:margin_top="50dp"
	        simple:fade="true"
	        simple:title_bar_height_display="20dp"
	        simple:title_bar_height_no_display="60dp"
	        simple:boundary="true"
			simple:move_distance_to_trigger="30dip"
			simple:animator_duration="300">

				....(Your ChildViews)

	   </net.wujingchao.android.view.MultiCardMenu>

License
-------
    MIT
