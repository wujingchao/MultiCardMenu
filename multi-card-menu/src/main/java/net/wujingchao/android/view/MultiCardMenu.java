package net.wujingchao.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ScrollView;

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
 */
@SuppressWarnings("unused")
public class  MultiCardMenu extends FrameLayout {

    public static final String TAG = "MultiCardMenu";

    private static final boolean DEBUG = true;

    private static final int DEFAULT_CARD_MARGIN_TOP = 0;

    private static final int DEFAULT_TITLE_BAR_HEIGHT_NO_DISPLAY = 60;

    private static final int DEFAULT_TITLE_BAR_HEIGHT_DISPLAY = 20;

    private static final int DEFAULT_MOVE_DISTANCE_TO_TRIGGER = 30;

    private static final int DEFAULT_DURATION = 250;

    private static final int MAX_CLICK_TIME = 300;

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
        this(context, null);
    }

    public MultiCardMenu(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MultiCardMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        ViewConfiguration vc = ViewConfiguration.get(mContext);
        mMaxVelocity = vc.getScaledMaximumFlingVelocity();
        mMinVelocity = vc.getScaledMinimumFlingVelocity() * 8;
        mTouchSlop = vc.getScaledTouchSlop();
        mDensity = mContext.getResources().getDisplayMetrics().density;
        TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.MultiCardMenu, defStyleAttr, 0);
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
        if(mBackgroundRid == -1) {//transparent background
            mBackgroundRid = R.layout.multi_card_view_transparent_background_view;
        }
        mDarkFrameLayout = new DarkFrameLayout(mContext);
        mDarkFrameLayout.addView(LayoutInflater.from(mContext).inflate(mBackgroundRid, null));
        mDarkFrameLayout.setMultiCardMenu(this);
        addView(mDarkFrameLayout);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (DEBUG) {
            Log.i(TAG,"onLayout:" + changed);
        }
        mChildCount = getChildCount();
        View backgroundView = getChildAt(0);//background view
        backgroundView.layout(0, 0, backgroundView.getMeasuredWidth(), backgroundView.getMeasuredHeight());
        for(int i = 1; i < mChildCount; i ++) {
            View childView = getChildAt(i);
            int t = (int) (getMeasuredHeight() - (mChildCount - i)* mTitleBarHeightNoDisplay);
            if (DEBUG) Log.i(TAG, String.format("child index:%s,top:%s", i, t));
            childView.layout(0, t, childView.getMeasuredWidth(), childView.getMeasuredHeight() + t);
        }
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
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
        firstDownX = event.getX();
        firstDownY = downY = event.getY();
        int realChildCount =  mChildCount - 1 ;//do not contain background view
        //Judge which card on touching
        if(!isDisplaying && downY > getMeasuredHeight() - mChildCount * mTitleBarHeightNoDisplay) {
            for(int i = 1; i <= mChildCount; i ++) {
                if(downY < (getMeasuredHeight() - mChildCount * mTitleBarHeightNoDisplay + mTitleBarHeightNoDisplay * i)) {
                    whichCardOnTouch = i-1;
                    isTouchOnCard = true;
                    if(mOnDisplayOrHideListener != null)
                        mOnDisplayOrHideListener.onTouchCard(whichCardOnTouch - 1);
                    isConsume = true;
                    break;
                }
            }
            mTouchingViewOriginY = ViewHelper.getY(getChildAt(whichCardOnTouch));
        }else if(isDisplaying && downY > getMeasuredHeight() - (realChildCount - 1) * mTitleBarHeightDisplay) {
            hideCard(mDisplayingCard);
        }else if(isDisplaying && downY > mMarginTop && mDisplayingCard >= 0 && downY < getChildAt(mDisplayingCard).getMeasuredHeight() + mMarginTop) {
            whichCardOnTouch = mDisplayingCard;
            isTouchOnCard = true;
        }else if(isDisplaying && (downY < mMarginTop || (mDisplayingCard >= 0 && (downY > mMarginTop + getChildAt(mDisplayingCard).getMeasuredHeight())))) {
            hideCard(mDisplayingCard);
        }

        if(whichCardOnTouch == 0){
            isTouchOnCard = false;
        }
        return isConsume;
    }

    private void handleActionMove(MotionEvent event) {
        if(whichCardOnTouch == -1 || !isTouchOnCard)return;
        if(canScrollInView((int) (firstDownY - event.getY()))) return;
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


    /**
     * @param direction Negative to check scrolling up, positive to check
     *                  scrolling down.
     * @return true if need dispatch touch event to child view,otherwise
     */
    private boolean canScrollInView(int direction) {
        View view = getChildAt(whichCardOnTouch);
        if(view instanceof ViewGroup){
            View childView = findTopChildUnder((ViewGroup) view, firstDownX, firstDownY);
            if(childView == null) return false;
            if(childView instanceof AbsListView){
                return absListViewCanScrollList((AbsListView)childView,direction);
            }else if(childView instanceof ScrollView) {
                return scrollViewCanScrollVertically((ScrollView) childView,direction);
            }
        }
        return false;
    }

    /**
     * Copy From AbsListView (API Level >= 19)
     * @param absListView AbsListView
     * @param direction Negative to check scrolling up, positive to check
     *                  scrolling down.
     * @return true if the list can be scrolled in the specified direction,
     *         false otherwise
     */
    private boolean absListViewCanScrollList(AbsListView absListView,int direction) {
        final int childCount = absListView.getChildCount();
        if (childCount == 0) {
            return false;
        }
        final int firstPosition = absListView.getFirstVisiblePosition();
        if (direction > 0) {//can scroll down
            final int lastBottom = absListView.getChildAt(childCount - 1).getBottom();
            final int lastPosition = firstPosition + childCount;
            return lastPosition < absListView.getCount() || lastBottom > absListView.getHeight() - absListView.getPaddingTop();
        } else {//can scroll  up
            final int firstTop = absListView.getChildAt(0).getTop();
            return firstPosition > 0 || firstTop < absListView.getPaddingTop();
        }
    }

    /**
     *  Copy From ScrollView (API Level >= 14)
     * @param direction Negative to check scrolling up, positive to check
     *                  scrolling down.
     *   @return true if the scrollView can be scrolled in the specified direction,
     *         false otherwise
     */
    private  boolean scrollViewCanScrollVertically(ScrollView scrollView,int direction) {
        final int offset = Math.max(0, scrollView.getScrollY());
        final int range = computeVerticalScrollRange(scrollView) - scrollView.getHeight();
        if (range == 0) return false;
        if (direction < 0) { //scroll up
            return offset > 0;
        } else {//scroll down
            return offset < range - 1;
        }
    }

    /**
     * Copy From ScrollView (API Level >= 14)
     * <p>The scroll range of a scroll view is the overall height of all of its
     * children.</p>
     */
    private int computeVerticalScrollRange(ScrollView scrollView) {
        final int count = scrollView.getChildCount();
        final int contentHeight = scrollView.getHeight() - scrollView.getPaddingBottom() - scrollView.getPaddingTop();
        if (count == 0) {
            return contentHeight;
        }

        int scrollRange = scrollView.getChildAt(0).getBottom();
        final int scrollY = scrollView.getScrollY();
        final int overScrollBottom = Math.max(0, scrollRange - contentHeight);
        if (scrollY < 0) {
            scrollRange -= scrollY;
        } else if (scrollY > overScrollBottom) {
            scrollRange += scrollY - overScrollBottom;
        }

        return scrollRange;
    }

    private void computeVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);
        yVelocity = mVelocityTracker.getYVelocity();
        xVelocity = mVelocityTracker.getXVelocity();
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(DEBUG)Log.i(TAG,"isDragging:" + isDragging);
        return isDragging;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return true;
    }

    public void show(int index) {
        if(index >= mChildCount) throw new IllegalArgumentException("Card Index Not Exist");
        displayCard(index);
    }

    public void hide(int index) {
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
        return mDisplayingCard - 1;
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

    private double distance(float x1, float y1, float x2, float y2) {
        float deltaX = x2 - x1;
        float deltaY = y2 - y1;
        return  Math.sqrt(deltaX * deltaX + deltaY * deltaY);
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
        int n = mChildCount - 1;
        for(int i = 1,j = 1; i < mChildCount; i++) {
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
            mOnDisplayOrHideListener.onDisplay(which - 1);
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
        for(int i = 1; i < mChildCount; i ++) {
            if(i != which) {
                t = (int) (getMeasuredHeight() - (mChildCount - i) * mTitleBarHeightNoDisplay);
                animators.add(ObjectAnimator.ofFloat(getChildAt(i), "y", ViewHelper.getY(getChildAt(i)), t).setDuration(mDuration));
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
            mOnDisplayOrHideListener.onHide(which - 1);
    }

    private View findTopChildUnder(ViewGroup parentView,float x, float y) {
        final int childCount = parentView.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            final View child = parentView.getChildAt(i);
            if (x >= child.getLeft() && x < child.getRight() && y >= child.getTop() && y < child.getBottom()) {
                return child;
            }
        }
        return null;
    }

    private int px2dip(float pxVal) {
        return (int)(pxVal/mDensity + 0.5f);
    }

    private int dip2px(int dipVal) {
        return (int)(dipVal * mDensity + 0.5f);
    }

    public interface OnDisplayOrHideListener {

        void onDisplay(int which);

        void onHide(int which);

        void onTouchCard(int which);

    }

}
