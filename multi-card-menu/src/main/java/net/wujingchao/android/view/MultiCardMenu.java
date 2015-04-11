package net.wujingchao.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

/**
 * @author wujingchao  2015-04-01 email:wujingchao@aliyun.com
 */
public class MultiCardMenu extends FrameLayout {

    private final static String TAG = "MultiCardMenu";

    private final static int DEFAULT_CARD_MARGIN_TOP = 20;

    private final static int DEFAULT_TITLE_BAR_HEIGHT_ON_SPREAD = 60;

    private final static int DEFAULT_TITLE_BAR_HEIGHT_ON_FOLD = 20;

    private final static int DEFAULT_MOVE_DISTANCE_TO_TRIGGER = 50;

    private final static int DEFAULT_DURATION = 800;

    private float mDensity;

    private float mTitleBarHeightOnSpread;

    private float mTitleBarHeightOnFold;

    public MultiCardMenu(Context context) {
        this(context,null);
    }

    public MultiCardMenu(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    private float mMarginTop;

    private float deltaY;

    private int whichCardOnTouch = -1;

    private float downY;

    private float firstDownY;

    private float firstDownX;

    private boolean isTouchOnCard = false;

    private float mTouchViewOriginalY;

    private int mChildCount;

    private boolean isDisplaying = false;

    private Interpolator interpolator = new DecelerateInterpolator();

    private float mMoveDistanceToTrigger;

    private int mDisplayingCard  = -1;

    public MultiCardMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDensity = context.getResources().getDisplayMetrics().density;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiCardMenu, defStyleAttr, 0);
        mTitleBarHeightOnSpread = a.getDimension(R.styleable.MultiCardMenu_title_bar_height_on_spread,dip2px(DEFAULT_TITLE_BAR_HEIGHT_ON_SPREAD));
        mTitleBarHeightOnFold = a.getDimension(R.styleable.MultiCardMenu_title_bar_height_on_fold, dip2px(DEFAULT_TITLE_BAR_HEIGHT_ON_FOLD));
        mMarginTop = a.getDimension(R.styleable.MultiCardMenu_margin_top, dip2px(DEFAULT_CARD_MARGIN_TOP));
        mMoveDistanceToTrigger = a.getDimension(R.styleable.MultiCardMenu_move_distance_to_trigger,dip2px(DEFAULT_MOVE_DISTANCE_TO_TRIGGER));
        a.recycle();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mChildCount = getChildCount();
        for(int i = 0; i < mChildCount; i ++) {
            View childView = getChildAt(i);
            //l t r b
            int t = (int) (getMeasuredHeight() - (mChildCount - i)* mTitleBarHeightOnSpread);
            childView.layout(0, t, childView.getMeasuredWidth(), childView.getMeasuredHeight() + t);
        }
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                handleActionDown(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                handleActionUp(event);
                break;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(event);
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    private void handleActionDown(MotionEvent event) {
        firstDownY = downY = event.getY();
        firstDownX = event.getX();
        //Judge which card on touching
        if(!isDisplaying && downY > (getMeasuredHeight() - mChildCount * mTitleBarHeightOnSpread)) {
            for(int i = 1; i <= mChildCount; i ++) {
                if(downY < (getMeasuredHeight() - mChildCount * mTitleBarHeightOnSpread
                        + mTitleBarHeightOnSpread * i)) {
                    whichCardOnTouch = i-1;
                    isTouchOnCard = true;
                    break;
                }
            }
        }else if(isDisplaying && downY > (getMeasuredHeight() - (mChildCount - 1) * mTitleBarHeightOnFold)) {
            //TODO click bottom card
            Log.d(TAG,"click bottom card");
        }else if(isDisplaying && downY > mMarginTop && downY < getChildAt(mDisplayingCard).getMeasuredHeight() + mMarginTop) {
            whichCardOnTouch = mDisplayingCard;
            isTouchOnCard = true;
        }
        mTouchViewOriginalY = ViewHelper.getY(getChildAt(whichCardOnTouch));
    }

    private void handleActionMove(MotionEvent event) {
        deltaY += (event.getY() - downY);
        downY = event.getY();
        View touchingChildView = getChildAt(whichCardOnTouch);
        if(!isDisplaying && whichCardOnTouch != -1 && isTouchOnCard) {
            if(ViewHelper.getY(touchingChildView) <= mMarginTop && deltaY < 0){
                ViewHelper.setY(touchingChildView,mMarginTop);
                return;
            }else if(ViewHelper.getY(touchingChildView) >= mTouchViewOriginalY && deltaY > 0) {
                ViewHelper.setY(touchingChildView, mTouchViewOriginalY);
                return;
            }
            Log.d(TAG,"Y:" + ViewHelper.getY(touchingChildView));
            ViewHelper.setTranslationY(touchingChildView, deltaY);
        }else if(isDisplaying && whichCardOnTouch != -1 && isTouchOnCard) {
//            ViewHelper.setTranslationY(touchingChildView, deltaY);
            ViewHelper.setY(touchingChildView,downY);
        }
    }


    private void handleActionUp(MotionEvent event) {
        if(!isDisplaying && whichCardOnTouch != -1 && isTouchOnCard && (event.getY() == firstDownY && event.getX() == firstDownX) //means click...
                || (Math.abs(event.getY() - firstDownY) > mMoveDistanceToTrigger)) {
               displayCard(whichCardOnTouch);
        }else if(!isDisplaying && (Math.abs(event.getY() - firstDownY) < mMoveDistanceToTrigger)) {
            hideCard(whichCardOnTouch);
        }
        isTouchOnCard = false;
        deltaY = 0;
    }

    private void displayCard(int which) {
        if(isDisplaying)return;
        ObjectAnimator [] animators = new ObjectAnimator[mChildCount];
        animators[0] = ObjectAnimator
                .ofFloat(getChildAt(which), "y", ViewHelper.getY(getChildAt(which)), mMarginTop)
                .setDuration(DEFAULT_DURATION);
        for(int i = 0,j=1; i < mChildCount; i++) {
            if(i != which){
                animators[j] = ObjectAnimator
                        .ofFloat(getChildAt(i),"y",ViewHelper.getY(getChildAt(i)),
                                getMeasuredHeight() - mTitleBarHeightOnFold * (mChildCount-j))
                        .setDuration(DEFAULT_DURATION);
                j ++;
            }
        }
        for(ObjectAnimator o:animators) {
            o.setInterpolator(interpolator);
            o.start();
        }
        isDisplaying = true;
        mDisplayingCard = which;
    }

    private void hideCard(int which) {
        ObjectAnimator animators [] = new ObjectAnimator[mChildCount];
        View displayingCard = getChildAt(which);
        int t = (int) (getMeasuredHeight() - (mChildCount - which)* mTitleBarHeightOnSpread);
        animators[0] = ObjectAnimator.ofFloat(displayingCard,"y",
                ViewHelper.getY(displayingCard),t).setDuration(DEFAULT_DURATION);
        for(int i = 0,j = 1; i < mChildCount; i ++) {
            if(i != which) {
                t = (int) (getMeasuredHeight() - (mChildCount - i)* mTitleBarHeightOnSpread);
                animators[j] = ObjectAnimator.ofFloat(getChildAt(i),"y",
                        ViewHelper.getY(getChildAt(i)),t);
                j ++;
            }
        }
        for(ObjectAnimator o :animators) {
            o.setInterpolator(interpolator);
            o.start();
        }
        isDisplaying = false;
        mDisplayingCard = -1;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return true;
    }

    @SuppressWarnings("unused")
    private int px2dip(float pxVal) {
        return (int)(pxVal/mDensity + 0.5f);
    }

    private int dip2px(int dipVal) {
        return (int)(dipVal * mDensity + 0.5f);
    }
}
