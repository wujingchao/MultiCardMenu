package net.wujingchao.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wujingchao  2015-04-01 email:wujingchao@aliyun.com
 *
 * TODO support listview scrollview
 */
@SuppressWarnings("unused")
public class  MultiCardMenu extends FrameLayout {

    private final static String TAG = "MultiCardMenu";

    private final static int DEFAULT_CARD_MARGIN_TOP = 0;

    private final static int DEFAULT_TITLE_BAR_HEIGHT_NO_DISPLAY = 60;

    private final static int DEFAULT_TITLE_BAR_HEIGHT_DISPLAY = 20;

    private final static int DEFAULT_MOVE_DISTANCE_TO_TRIGGER = 30;

    private final static int DEFAULT_DURATION = 250;

    private final static int MAX_CLICK_TIME = 300;

    private static float MAX_CLICK_DISTANCE = 5;

    private float mDensity;

    private float mTitleBarHeightNoDisplay;

    private float mTitleBarHeightDisplay;

    private VelocityTracker mVelocityTracker;

    private float mMarginTop;

    private float deltaY;

    private int whichCardOnTouch = -1;

    private float downY;

    private float firstDownY;

    private float firstDownX;

    private boolean isTouchOnCard = false;

    private int mChildCount;

    private boolean isDisplaying = false;

    private Interpolator mOpenAnimatorInterpolator = new AccelerateInterpolator();

    private Interpolator mCloseAnimatorInterpolator = new AccelerateInterpolator();

    private float mMoveDistanceToTrigger;

    private int mDisplayingCard  = -1;

    private int mMaxVelocity;

    private int mMinVelocity;

    private boolean isDragging = false;

    private float xVelocity;

    private float yVelocity;

    private OnDisplayOrHideListener mOnDisplayOrHideListener;

    private boolean isExistBackground = false;

    private int mDuration;

    private boolean isAnimating = false;

    private DarkFrameLayout mDarkFrameLayout;

    private boolean isFade;

    private int mTouchSlop;

    private long mPressStartTime;

    private boolean mBoundary;

    private float mTouchingViewOriginY;

    private Context mContext;

    private int mBackgroundRid;

    public MultiCardMenu(Context context) {
        this(context,null);
    }

    public MultiCardMenu(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MultiCardMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        ViewConfiguration vc = ViewConfiguration.get(context);
        mMaxVelocity = vc.getScaledMaximumFlingVelocity();
        mMinVelocity = vc.getScaledMinimumFlingVelocity() * 8;
        mTouchSlop = vc.getScaledTouchSlop();
        mDensity = context.getResources().getDisplayMetrics().density;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiCardMenu, defStyleAttr, 0);
        MAX_CLICK_DISTANCE = mTitleBarHeightNoDisplay = a.getDimension(R.styleable.MultiCardMenu_title_bar_height_no_display,dip2px(DEFAULT_TITLE_BAR_HEIGHT_NO_DISPLAY));
        mTitleBarHeightDisplay = a.getDimension(R.styleable.MultiCardMenu_title_bar_height_display, dip2px(DEFAULT_TITLE_BAR_HEIGHT_DISPLAY));
        mMarginTop = a.getDimension(R.styleable.MultiCardMenu_margin_top, dip2px(DEFAULT_CARD_MARGIN_TOP));
        mMoveDistanceToTrigger = a.getDimension(R.styleable.MultiCardMenu_move_distance_to_trigger,dip2px(DEFAULT_MOVE_DISTANCE_TO_TRIGGER));
        mBackgroundRid = a.getResourceId(R.styleable.MultiCardMenu_background_layout,-1);
        mDuration = a.getInt(R.styleable.MultiCardMenu_animator_duration,DEFAULT_DURATION);
        isFade = a.getBoolean(R.styleable.MultiCardMenu_fade,true);
        mBoundary = a.getBoolean(R.styleable.MultiCardMenu_boundary,false);
        a.recycle();
        initBackgroundView();
    }

    private void initBackgroundView() {
        if(mBackgroundRid != -1) {
            mDarkFrameLayout = new DarkFrameLayout(mContext);
            mDarkFrameLayout.addView(LayoutInflater.from(mContext).inflate(mBackgroundRid, null));
            isExistBackground = true;
            mDarkFrameLayout.setMultiCardMenu(this);
            addView(mDarkFrameLayout);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mChildCount = getChildCount();
        for(int i = 0; i < mChildCount; i ++) {
            View childView = getChildAt(i);
            if(i == 0 && isExistBackground) {
                childView.layout(0,0,childView.getMeasuredWidth(),childView.getMeasuredHeight());
                continue;
            }
//            FrameLayout.LayoutParams params = (LayoutParams) childView.getLayoutParams();
            //l t r b
            int t = (int) (getMeasuredHeight() - (mChildCount - i)* mTitleBarHeightNoDisplay);
            childView.layout(0, t, childView.getMeasuredWidth(), childView.getMeasuredHeight() + t);
        }
    }

    private void initVelocityTracker(MotionEvent event) {
        if(mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    private void releaseVelocityTracker() {
        if(mVelocityTracker != null) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        initVelocityTracker(event);
        boolean isConsume = false;
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                isConsume = handleActionDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                handleActionUp(event);
                releaseVelocityTracker();
                break;
        }
        return isConsume || super.dispatchTouchEvent(event);
    }

    private boolean handleActionDown(MotionEvent event) {
        boolean isConsume = false;
        mPressStartTime = System.currentTimeMillis();
        firstDownY = downY = event.getY();
        firstDownX = event.getX();
        int childCount = isExistBackground ? (mChildCount - 1) : mChildCount;
        //Judge which card on touching
        if(!isDisplaying && downY > (getMeasuredHeight() - mChildCount * mTitleBarHeightNoDisplay)) {
            for(int i = 1; i <= mChildCount; i ++) {
                if(downY < (getMeasuredHeight() - mChildCount * mTitleBarHeightNoDisplay
                        + mTitleBarHeightNoDisplay * i)) {
                    whichCardOnTouch = i-1;
                    isTouchOnCard = true;
                    if(mOnDisplayOrHideListener != null)
                        mOnDisplayOrHideListener.onTouchCard(whichCardOnTouch);
                    isConsume = true;
                    break;
                }
            }
            mTouchingViewOriginY = ViewHelper.getY(getChildAt(whichCardOnTouch));
        }else if(isDisplaying && downY > (getMeasuredHeight() - (childCount - 1) * mTitleBarHeightDisplay)) {
            hideCard(mDisplayingCard);
        }else if(isDisplaying && downY > mMarginTop && mDisplayingCard >= 0 && downY < getChildAt(mDisplayingCard).getMeasuredHeight() + mMarginTop) {
            whichCardOnTouch = mDisplayingCard;
            isTouchOnCard = true;
        }else if(isDisplaying && (downY < mMarginTop
                    || (mDisplayingCard >= 0
                    && (downY > mMarginTop + getChildAt(mDisplayingCard).getMeasuredHeight())))) {
            hideCard(mDisplayingCard);
        }

        if(isExistBackground && whichCardOnTouch == 0){
            isTouchOnCard = false;
        }
        return isConsume;
    }


    private void handleActionMove(MotionEvent event) {
        if(whichCardOnTouch == -1 || !isTouchOnCard)return;
        computeVelocity();
        if(Math.abs(yVelocity) < Math.abs(xVelocity)) return;

        if(!isDragging && Math.abs(event.getY() - firstDownY) > mTouchSlop
                && Math.abs(event.getX() - firstDownX) < mTouchSlop) {
            isDragging = true;
            downY = event.getY();
        }

        if(isDragging) {
            deltaY = event.getY() - downY;
            downY = event.getY();
            View touchingChildView = getChildAt(whichCardOnTouch);
            if(!mBoundary) {
                touchingChildView.offsetTopAndBottom((int) deltaY);
            }else {
                float touchingViewY = ViewHelper.getY(touchingChildView);
                if(touchingViewY + deltaY <= mMarginTop) {
                    touchingChildView.offsetTopAndBottom((int) (mMarginTop - touchingViewY));
                }else if(touchingViewY + deltaY >= mTouchingViewOriginY) {
                    touchingChildView.offsetTopAndBottom((int) (mTouchingViewOriginY - touchingViewY));
                }else {
                    touchingChildView.offsetTopAndBottom((int) deltaY);
                }
            }
        }

    }

    private void handleActionUp(MotionEvent event) {
        if(whichCardOnTouch == -1 || !isTouchOnCard) return;
        long pressDuration = System.currentTimeMillis() - mPressStartTime;
        computeVelocity();
        if(!isDisplaying && ((event.getY() - firstDownY < 0 && (Math.abs(event.getY() - firstDownY) > mMoveDistanceToTrigger))
                || (yVelocity < 0 && Math.abs(yVelocity) > mMinVelocity && Math.abs(yVelocity)> Math.abs(xVelocity))
        )){
            displayCard(whichCardOnTouch);
        }else if(!isDisplaying && pressDuration < MAX_CLICK_TIME &&   //means click
                distance(firstDownX,firstDownY,event.getX(),event.getY()) < MAX_CLICK_DISTANCE) {
            displayCard(whichCardOnTouch);
        }else if(!isDisplaying && isDragging &&  ((event.getY() - firstDownY > 0) || Math.abs(event.getY() - firstDownY) < mMoveDistanceToTrigger)) {
            hideCard(whichCardOnTouch);
        }else if(isDisplaying) {
            float currentY = ViewHelper.getY(getChildAt(mDisplayingCard));
            if(currentY < mMarginTop || currentY < (mMarginTop + mMoveDistanceToTrigger)) {
                ObjectAnimator.ofFloat(getChildAt(mDisplayingCard),"y",
                        currentY,mMarginTop)
                        .setDuration(mDuration)
                        .start();
            }else if(currentY > (mMarginTop + mMoveDistanceToTrigger)) {
                hideCard(mDisplayingCard);
            }
        }
        isTouchOnCard = false;
        deltaY = 0;
        isDragging = false;
    }

    private double distance(float x1, float y1, float x2, float y2) {
        float deltaX = x2 - x1;
        float deltaY = y2 - y1;
        return  Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private void computeVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000,mMaxVelocity);
        yVelocity = mVelocityTracker.getYVelocity();
        xVelocity = mVelocityTracker.getXVelocity();
    }

    private void displayCard(int which) {
        if(isDisplaying || isAnimating)return;
        if(isFade && mDarkFrameLayout != null) mDarkFrameLayout.fade(true);
        List<Animator> animators = new ArrayList<>(mChildCount);
        final float distance = ViewHelper.getY(getChildAt(which)) - mMarginTop;
        ValueAnimator displayAnimator = ValueAnimator.ofFloat(ViewHelper.getY(getChildAt(which)), mMarginTop)
                 .setDuration(mDuration);
        displayAnimator.setTarget(getChildAt(which));
        final View displayingView = getChildAt(which);
        displayAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float value = (float) valueAnimator.getAnimatedValue();
                ViewHelper.setY(displayingView, value);
                if(mDarkFrameLayout != null && isFade) {
                    mDarkFrameLayout.fade((int) ((1-(value - mMarginTop)/distance) * DarkFrameLayout.MAX_ALPHA));
                }
            }
        });
        animators.add(displayAnimator);
        int n = isExistBackground ? (mChildCount - 1) : mChildCount;
        for(int i = 0,j = 1; i < mChildCount; i++) {
            if(i == 0 && isExistBackground) continue;
            if(i != which){
                animators.add(ObjectAnimator
                        .ofFloat(getChildAt(i), "y", ViewHelper.getY(getChildAt(i)),
                                getMeasuredHeight() - mTitleBarHeightDisplay * (n - j))
                        .setDuration(mDuration));
                j ++;
            }
        }
        AnimatorSet set = new AnimatorSet();
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                isAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                isAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                isAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        set.setInterpolator(mOpenAnimatorInterpolator);
        set.playTogether(animators);
        set.start();
        isDisplaying = true;
        mDisplayingCard = which;
        if(mOnDisplayOrHideListener != null)
            mOnDisplayOrHideListener.onDisplay(isExistBackground ? (which - 1) : which);
    }


    private void hideCard(int which) {
        if(isAnimating)return;
        List<Animator> animators = new ArrayList<>(mChildCount);
        final View displayingCard = getChildAt(which);
        int t = (int) (getMeasuredHeight() - (mChildCount - which)* mTitleBarHeightNoDisplay);
        ValueAnimator displayAnimator = ValueAnimator.ofFloat(ViewHelper.getY(displayingCard), t)
                       .setDuration(mDuration);
        displayAnimator.setTarget(displayingCard);
        final int finalT = t;
        displayAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float value = (float) valueAnimator.getAnimatedValue();
                ViewHelper.setY(displayingCard,value);
                if(mDarkFrameLayout != null && isFade && value < finalT) {
                    mDarkFrameLayout.fade((int) ((1 - value/ finalT) * DarkFrameLayout.MAX_ALPHA));
                }
            }
        });
        animators.add(displayAnimator);
        for(int i = 0; i < mChildCount; i ++) {
            if(i == 0 && isExistBackground) continue;
            if(i != which) {
                t = (int) (getMeasuredHeight() - (mChildCount - i)* mTitleBarHeightNoDisplay);
                animators.add(ObjectAnimator.ofFloat(getChildAt(i),"y",
                        ViewHelper.getY(getChildAt(i)),t));
            }
        }
        AnimatorSet set = new AnimatorSet();
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                isAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                isAnimating = false;
                isDisplaying = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                isAnimating = false;
                isDisplaying = false;
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        set.setInterpolator(mCloseAnimatorInterpolator);
        set.playTogether(animators);
        set.start();
        mDisplayingCard = -1;
        if(mOnDisplayOrHideListener != null)
            mOnDisplayOrHideListener.onHide(isExistBackground ? (which - 1) : which);
    }

    public void show(int index) {
        if (isExistBackground)index ++;
        if(index >= mChildCount) throw new IllegalArgumentException("Card Index Not Exist");
        displayCard(index);
    }



    public void hide(int index) {
        if(isExistBackground) index ++;
        if(index != mDisplayingCard || !isDisplaying) return;
        if(index >= mChildCount) throw new IllegalArgumentException("Card Index Not Exist");
        hideCard(index);
    }

    public void setOnDisplayOrHideListener(OnDisplayOrHideListener onDisplayOrHideListener) {
        this.mOnDisplayOrHideListener = onDisplayOrHideListener;
    }

    public void setAnimatorInterpolator(Interpolator interpolator) {
        this.mOpenAnimatorInterpolator = interpolator;
    }


    /**
     *
     * @return less than 0 :No Display Card
     */
    public int getDisplayingCard() {
        return isExistBackground ? (mDisplayingCard - 1) : mDisplayingCard;
    }

    public boolean isDisplaying() {
        return isDisplaying;
    }

    public void setBoundary(boolean boundary) {
        this.mBoundary = boundary;
    }

    public boolean isBoundary() {
        return mBoundary;
    }

    public boolean isFade() {
        return isFade;
    }

    public void setFade(boolean isFade) {
        this.isFade = isFade;
    }

    /**
     *
     * @return marginTop unit:dip
     */
    public int getMarginTop() {
        return px2dip(mMarginTop);
    }

    /**
     *
     * @param marginTop unit:dip
     */
    public void setMarginTop(int marginTop) {
        this.mMarginTop = dip2px(marginTop);
    }


    /**
     *
     * @return unit:dip
     */
    public int getTitleBarHeightNoDisplay() {
        return px2dip(mTitleBarHeightNoDisplay);
    }

    /**
     *
     * @param titleBarHeightNoDisplay unit:dip
     */
    public void setTitleBarHeightNoDisplay(int titleBarHeightNoDisplay) {
        this.mTitleBarHeightNoDisplay = dip2px(titleBarHeightNoDisplay);
        requestLayout();
    }

    /**
     *
     * @return unit:dip
     */
    public int getTitleBarHeightDisplay() {
        return px2dip(mTitleBarHeightDisplay);
    }

    /**
     *
     * @param titleBarHeightDisplay unit:dip
     */
    public void setTitleBarHeightDisplay(int titleBarHeightDisplay) {
        this.mTitleBarHeightDisplay = titleBarHeightDisplay;
        requestLayout();
    }

    /**
     *
     * @return unit:dip
     */
    public int getMoveDistanceToTrigger() {
        return px2dip(mMoveDistanceToTrigger);
    }

    /**
     *
     * @param moveDistanceToTrigger unit:dip
     */
    public void setMoveDistanceToTrigger(int moveDistanceToTrigger) {
        this.mMoveDistanceToTrigger = moveDistanceToTrigger;
    }


    public void setOpenAnimatorInterpolator(Interpolator interpolator) {
        this.mOpenAnimatorInterpolator =  interpolator;
    }

    public void setCloseAnimatorInterpolator(Interpolator interpolator) {
        this.mCloseAnimatorInterpolator = interpolator;
    }

    public Interpolator getOpenAnimatorInterpolator() {
        return this.mOpenAnimatorInterpolator;
    }

    public Interpolator getCloseAnimatorInterpolator() {
        return this.mCloseAnimatorInterpolator;
    }

    public void setAnimatorDuration(int duration) {
        this.mDuration = duration;
    }

    public int getAnimatorDuration() {
        return this.mDuration;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return isDragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    private int px2dip(float pxVal) {
        return (int)(pxVal/mDensity + 0.5f);
    }

    private int dip2px(int dipVal) {
        return (int)(dipVal * mDensity + 0.5f);
    }


    public interface OnDisplayOrHideListener {

        public void onDisplay(int which);

        public void onHide(int which);

        public void onTouchCard(int which);

    }

}
