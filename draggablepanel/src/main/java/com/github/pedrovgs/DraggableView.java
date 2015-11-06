/*
 * Copyright (C) 2014 Pedro Vicente Gómez Sánchez.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pedrovgs;

import com.github.pedrovgs.transformer.Transformer;
import com.github.pedrovgs.transformer.TransformerFactory;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class created to extends a ViewGroup and simulate the YoutubeLayoutComponent
 *
 * @author Pedro Vicente Gómez Sánchez
 */
public class DraggableView extends RelativeLayout {

    public static final int STATE_UNKNOWN = -1;
    public static final int STATE_MINIMIZED = 0;
    public static final int STATE_DRAG = 1;
    public static final int STATE_MAXIMIZED = 2;
    public static final int STATE_CLOSED = 3;

    @IntDef({STATE_UNKNOWN, STATE_MINIMIZED, STATE_DRAG, STATE_MAXIMIZED, STATE_CLOSED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {

    }

    private static final int DEFAULT_SCALE_FACTOR = 2;
    private static final int DEFAULT_TOP_VIEW_MARGIN = 30;
    private static final float SLIDE_TOP = 0f;
    private static final float SLIDE_BOTTOM = 1f;
    private static final float MIN_SLIDE_OFFSET = 0.1f;
    private static final boolean DEFAULT_ENABLE_HORIZONTAL_ALPHA_EFFECT = true;
    private static final boolean DEFAULT_ENABLE_CLICK_TO_MAXIMIZE = false;
    private static final boolean DEFAULT_ENABLE_CLICK_TO_MINIMIZE = false;
    private static final int MIN_SLIDING_DISTANCE_ON_CLICK = 10;
    private static final int ONE_HUNDRED = 100;
    private static final float SENSITIVITY = 1f;
    private static final boolean DEFAULT_TOP_VIEW_RESIZE = false;
    private static final int INVALID_POINTER = -1;

    private int activePointerId = INVALID_POINTER;
    private float lastTouchActionDownXPosition;

    private View dragView;
    private View secondView;
    private View resizableView;
    private TypedArray attributes;

    private FragmentManager fragmentManager;
    private ViewDragHelper viewDragHelper;
    private Transformer transformer;

    private boolean enableHorizontalAlphaEffect;
    private boolean enableClickToMaximize;
    private boolean enableClickToMinimize;

    private boolean touchEnabled;
    private boolean configurationChanged;
    private int lastPosition;
    private float lastDragRange;
    @State
    private int lastNotifiedState;
    private boolean stateChanged;
    private int lastSlideTargetY;
    private float lastSlideOffset;

    private OnStateChangedListener listener;

    public DraggableView(Context context) {
        super(context);
    }

    public DraggableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeAttributes(attrs);
    }

    public DraggableView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeAttributes(attrs);
        setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if (viewDragHelper.getViewDragState() == ViewDragHelper.STATE_SETTLING
                        && lastSlideTargetY != calculateSlideTargetY(lastSlideOffset)) {
                    smoothSlideTo(lastSlideOffset);
                }
            }
        });
    }

    /**
     * Return if user can maximize minimized view on click.
     */
    public boolean isClickToMaximizeEnabled() {
        return enableClickToMaximize;
    }

    /**
     * Enable or disable click to maximize view when dragged view is minimized
     * If your content have a touch/click listener (like YoutubePlayer), you
     * need disable it to active this feature.
     *
     * @param enableClickToMaximize to enable or disable the click.
     */
    public void setClickToMaximizeEnabled(boolean enableClickToMaximize) {
        this.enableClickToMaximize = enableClickToMaximize;
    }

    /**
     * Return if user can minimize maximized view on click.
     */
    public boolean isClickToMinimizeEnabled() {
        return enableClickToMinimize;
    }

    /**
     * Enable or disable click to minimize view when dragged view is maximized
     * If your content have a touch/click listener (like YoutubePlayer), you
     * need disable it to active this feature.
     *
     * @param enableClickToMinimize to enable or disable the click.
     */
    public void setClickToMinimizeEnabled(boolean enableClickToMinimize) {
        this.enableClickToMinimize = enableClickToMinimize;
    }

    /**
     * Return if touch listener is enable or disable
     */
    private boolean isTouchEnabled() {
        return this.touchEnabled;
    }

    /**
     * Enable or disable the touch listener
     *
     * @param touchEnabled to enable or disable the touch event.
     */
    public void setTouchEnabled(boolean touchEnabled) {
        this.touchEnabled = touchEnabled;
    }

    /**
     * Slide the view based on scroll of the nav drawer.
     * "setEnableTouch" user prevents click to expand while the drawer is moving, it will be
     * set to false when the @slideOffset is bigger than MIN_SLIDE_OFFSET.
     * When the slideOffset is bigger than 0.1 and dragView isn't close, set the dragView
     * to minimized.
     * It's only possible to maximize the view when @slideOffset is equals to 0.0,
     * in other words, closed.
     *
     * @param slideOffset    Value between 0 and 1, represent the value of slide:
     *                       0.0 is equal to close drawer and 1.0 equals open drawer.
     * @param drawerPosition Represent the position of nav drawer on X axis.
     * @param width          Width of nav drawer
     */
    public void slideHorizontally(float slideOffset, float drawerPosition, int width) {
        if (slideOffset > MIN_SLIDE_OFFSET && !isClosed() && isMaximized()) {
            minimize();
        }
        setTouchEnabled(slideOffset <= MIN_SLIDE_OFFSET);
        setX(width - Math.abs(drawerPosition));
    }

    /**
     * Configure the horizontal scale factor applied when the view is dragged to the bottom of the
     * custom view.
     */
    public void setXTopViewScaleFactor(float xScaleFactor) {
        transformer.setXScaleFactor(xScaleFactor);
    }

    /**
     * Configure the vertical scale factor applied when the view is dragged to the bottom of the
     * custom view.
     */
    public void setYTopViewScaleFactor(float yScaleFactor) {
        transformer.setYScaleFactor(yScaleFactor);
    }

    /**
     * Configure the dragged view margin right applied when the dragged view is minimized.
     *
     * @param topFragmentMarginRight in pixels.
     */
    public void setTopViewMarginRight(int topFragmentMarginRight) {
        transformer.setMarginRight(topFragmentMarginRight);
    }

    /**
     * Configure the dragView margin bottom applied when the dragView is minimized.
     */
    public void setTopViewMarginBottom(int topFragmentMarginBottom) {
        transformer.setMarginBottom(topFragmentMarginBottom);
    }

    /**
     * Configure the disabling of the alpha effect applied when the dragView is dragged
     * horizontally.
     */
    public void setHorizontalAlphaEffectEnabled(boolean enableHorizontalAlphaEffect) {
        this.enableHorizontalAlphaEffect = enableHorizontalAlphaEffect;
    }

    /**
     * Configure the DraggableListener notified when the view is minimized, maximized, closed to
     * the
     * right or closed to the left.
     */
    public void setStateListener(OnStateChangedListener listener) {
        if (this.listener != listener) {
            this.listener = listener;
            lastNotifiedState = STATE_UNKNOWN;
            onStateChanged();
        }
    }

    /**
     * To ensure the animation is going to work this method has been override to call
     * postInvalidateOnAnimation if the view is not settled yet.
     */
    @Override
    public void computeScroll() {
        if (!isInEditMode() && viewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        configurationChanged = true;
    }

    /**
     * Maximize the custom view applying an animation to return the view to the initial position.
     */
    public void maximize() {
        smoothSlideTo(SLIDE_TOP);
    }

    /**
     * Minimize the custom view applying an animation to put the top fragment on the bottom right
     * corner of the screen.
     */
    public void minimize() {
        smoothSlideTo(SLIDE_BOTTOM);
    }

    /**
     * Close the custom view applying an animation to close the view to the right side of the
     * screen.
     */
    public void closeToRight() {
        if (viewDragHelper.smoothSlideViewTo(dragView, transformer.getOriginalWidth(),
                getHeight() - transformer.getMinHeightPlusMargin())) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * Close the custom view applying an animation to close the view to the left side of the
     * screen.
     */
    public void closeToLeft() {
        if (viewDragHelper.smoothSlideViewTo(dragView, -transformer.getOriginalWidth(),
                getHeight() - transformer.getMinHeightPlusMargin())) {
            ViewCompat.postInvalidateOnAnimation(this);
            onStateChanged();
        }
    }

    /**
     * Checks if the top view is minimized.
     *
     * @return true if the view is minimized.
     */
    public boolean isMinimized() {
        return lastPosition == lastDragRange && lastDragRange != 0;
    }

    /**
     * Checks if the top view is maximized.
     *
     * @return true if the view is maximized.
     */
    public boolean isMaximized() {
        return lastPosition == 0;
    }

    /**
     * Checks if the top view closed at the right place.
     *
     * @return true if the view is closed at right.
     */
    public boolean isClosedAtRight() {
        return dragView.getLeft() >= getWidth();
    }

    /**
     * Checks if the top view is closed at the left place.
     *
     * @return true if the view is closed at left.
     */
    public boolean isClosedAtLeft() {
        return dragView.getRight() <= 0;
    }

    /**
     * Checks if the top view is closed at the right or left place.
     *
     * @return true if the view is closed.
     */
    public boolean isClosed() {
        return isClosedAtLeft() || isClosedAtRight();
    }

    /**
     * Override method to intercept only touch events over the drag view and to cancel the drag
     * when
     * the action associated to the MotionEvent is equals to ACTION_CANCEL or ACTION_UP.
     *
     * @param ev captured.
     * @return true if the view is going to process the touch event or false if not.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isEnabled() || !isTouchEnabled()) {
            return false;
        }

        try {
            switch (MotionEventCompat.getActionMasked(ev) & MotionEventCompat.ACTION_MASK) {
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    viewDragHelper.cancel();
                    return false;
                case MotionEvent.ACTION_DOWN:
                    int index = MotionEventCompat.getActionIndex(ev);
                    activePointerId = MotionEventCompat.getPointerId(ev, index);
                    if (activePointerId == INVALID_POINTER) {
                        return false;
                    }
                    break;
                default:
                    break;
            }

            boolean interceptTap = viewDragHelper.isViewUnder(dragView, (int) ev.getX(),
                    (int) ev.getY());
            return viewDragHelper.shouldInterceptTouchEvent(ev) || interceptTap;
        } catch (Exception ignored) {

        }

        return false;
    }

    /**
     * Override method to dispatch touch event to the dragged view.
     *
     * @param ev captured.
     * @return true if the touch event is realized over the drag or second view.
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled() || !isTouchEnabled()) {
            return false;
        }

        try {
            int actionMasked = MotionEventCompat.getActionMasked(ev);
            if ((actionMasked & MotionEventCompat.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                activePointerId = MotionEventCompat.getPointerId(ev, actionMasked);
            }

            if (activePointerId == INVALID_POINTER) {
                return false;
            }

            boolean isResizeViewHit = isViewHit(resizableView, (int) ev.getX(), (int) ev.getY());
            if (isResizeViewHit || ev.getAction() != MotionEvent.ACTION_DOWN) {
                viewDragHelper.processTouchEvent(ev);
            }

            if (isClosed()) {
                return false;
            }

            boolean isSecondViewHit = isViewHit(secondView, (int) ev.getX(), (int) ev.getY());
            analyzeTouchToMaximizeIfNeeded(ev, isResizeViewHit);
            if (isMaximized()) {
                dragView.dispatchTouchEvent(ev);
            } else {
                dragView.dispatchTouchEvent(
                        cloneMotionEventWithAction(ev, MotionEvent.ACTION_CANCEL));
            }

            return isResizeViewHit || isSecondViewHit;
        } catch (Exception ignored) {

        }
        return false;
    }

    private void analyzeTouchToMaximizeIfNeeded(MotionEvent ev, boolean isDragViewHit) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchActionDownXPosition = ev.getX();
                break;
            case MotionEvent.ACTION_UP:
                float clickOffset = ev.getX() - lastTouchActionDownXPosition;
                if (shouldMaximizeOnClick(ev, clickOffset, isDragViewHit)) {
                    if (isMinimized() && isClickToMaximizeEnabled()) {
                        maximize();
                    } else if (isMaximized() && isClickToMinimizeEnabled()) {
                        minimize();
                    }
                }
                break;
            default:
                break;
        }
    }

    public boolean shouldMaximizeOnClick(MotionEvent ev, float deltaX, boolean isDragViewHit) {
        return (Math.abs(deltaX) < MIN_SLIDING_DISTANCE_ON_CLICK)
                && ev.getAction() != MotionEvent.ACTION_MOVE
                && isDragViewHit;
    }

    /**
     * Clone given motion event and set specified action. This method is useful, when we want to
     * cancel event propagation in child views by sending event with {@link
     * android.view.MotionEvent#ACTION_CANCEL}
     * action.
     *
     * @param event  event to clone
     * @param action new action
     * @return cloned motion event
     */
    private MotionEvent cloneMotionEventWithAction(MotionEvent event, int action) {
        return MotionEvent.obtain(event.getDownTime(), event.getEventTime(), action, event.getX(),
                event.getY(), event.getMetaState());
    }

    /**
     * Override method to configure the dragged view and secondView layout properly.
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (isInEditMode()) {
            super.onLayout(changed, left, top, right, bottom);
        } else if (isMaximized()) {
            secondView.setY(transformer.getOriginalHeight());
            super.onLayout(changed, left, top, right, bottom);
        } else if ((configurationChanged || stateChanged) && lastNotifiedState != STATE_CLOSED) {
            float verticalDragRange = getVerticalDragRange();
            int newPosition = configurationChanged ? (int) verticalDragRange : dragView.getTop();

            lastPosition = newPosition;
            lastDragRange = verticalDragRange;
            dragView.layout(left, newPosition, right,
                    newPosition + transformer.getOriginalHeight());
            if (configurationChanged) {
                secondView.layout(left, transformer.getOriginalHeight(), right, bottom);
                viewDragHelper.abort();
            }
            changeDragViewScale();
        }

        configurationChanged = false;
        stateChanged = false;
    }

    public void onViewPositionChanged() {
        if (isMinimized() && !configurationChanged) {
            changeDragViewAlpha();
        } else {
            restoreAlpha();
            changeDragViewScale();
            changeSecondViewAlpha();
            changeSecondViewPosition();
            changeBackgroundAlpha();
        }

        int newPosition = dragView.getTop();
        if (lastPosition != 0 && newPosition == 0) {
            requestLayout();
        }

        lastPosition = newPosition;
        lastDragRange = getVerticalDragRange();

        onStateChanged();
    }

    /**
     * Override method to map dragged view, secondView to view objects, to configure dragged
     * view height and to initialize DragViewHelper.
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (!isInEditMode()) {
            mapGUI(attributes);
            initializeTransformer(attributes);
            attributes.recycle();
            initializeViewDragHelper();
        }
    }

    private void mapGUI(TypedArray attributes) {
        int dragViewId = attributes.getResourceId(R.styleable.draggable_view_top_view_id,
                R.id.drag_view);
        int secondViewId = attributes.getResourceId(R.styleable.draggable_view_bottom_view_id,
                R.id.second_view);
        int resizableViewId = attributes.getResourceId(
                R.styleable.draggable_view_resizable_top_view_id, dragViewId);
        dragView = findViewById(dragViewId);
        secondView = findViewById(secondViewId);
        resizableView = findViewById(resizableViewId);
    }

    /**
     * Configure the FragmentManager used to attach top and bottom Fragments to the view. The
     * FragmentManager is going to be provided only by DraggablePanel view.
     */
    void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    /**
     * Attach one fragment to the dragged view.
     *
     * @param topFragment to be attached.
     */
    void attachTopFragment(Fragment topFragment) {
        addFragmentToView(R.id.drag_view, topFragment);
    }

    /**
     * Attach one fragment to the secondView.
     *
     * @param bottomFragment to be attached.
     */
    void attachBottomFragment(Fragment bottomFragment) {
        addFragmentToView(R.id.second_view, bottomFragment);
    }

    /**
     * Modify secondView position to be always below dragged view.
     */
    void changeSecondViewPosition() {
        secondView.setY(dragView.getBottom());
    }

    /**
     * Modify dragged view scale based on the dragged view vertical position and the scale factor.
     */
    void changeDragViewScale() {
        transformer.updateScaleAndPosition(getVerticalDragOffset());
    }

    /**
     * Modify the background alpha if has been configured to applying an alpha effect when the view
     * is dragged.
     */
    void changeBackgroundAlpha() {
        Drawable background = getBackground();
        if (background != null) {
            int newAlpha = (int) (ONE_HUNDRED * (1 - getVerticalDragOffset()));
            background.setAlpha(newAlpha);
        }
    }

    /**
     * Modify the second view alpha based on dragged view vertical position.
     */
    void changeSecondViewAlpha() {
        secondView.setAlpha(1 - getVerticalDragOffset());
    }

    /**
     * Modify dragged view alpha based on the horizontal position while the view is being
     * horizontally dragged.
     */
    void changeDragViewAlpha() {
        if (enableHorizontalAlphaEffect) {
            float alpha = 1 - getHorizontalDragOffset();
            if (alpha == 0) {
                alpha = 1;
            }
            dragView.setAlpha(alpha);
        }
    }

    /**
     * Restore view alpha to 1
     */
    void restoreAlpha() {
        if (enableHorizontalAlphaEffect && dragView.getAlpha() < 1) {
            dragView.setAlpha(1);
        }
    }

    /**
     * Check if dragged view is above the middle of the custom view.
     *
     * @return true if dragged view is above the middle of the custom view or false if is below.
     */
    boolean isDragViewAboveTheMiddle() {
        return transformer.isAboveTheMiddle();
    }

    /**
     * Check if dragged view is next to the left bound.
     *
     * @return true if dragged view right position is behind the right half of the custom view.
     */
    boolean isNextToLeftBound() {
        return transformer.isNextToLeftBound();
    }

    /**
     * Check if dragged view is next to the right bound.
     *
     * @return true if dragged view left position is behind the left quarter of the custom view.
     */
    boolean isNextToRightBound() {
        return transformer.isNextToRightBound();
    }

    /**
     * Check if dragged view is at the right of the custom view.
     *
     * @return true if dragged view right position is equals to custom view width.
     */
    boolean isDragViewAtRight() {
        return transformer.isViewAtRight();
    }

    /**
     * Calculate if one position is above any view.
     *
     * @param view to analyze.
     * @param x    position.
     * @param y    position.
     * @return true if x and y positions are below the view.
     */
    private boolean isViewHit(View view, int x, int y) {
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0]
                && screenX < viewLocation[0] + view.getWidth()
                && screenY >= viewLocation[1]
                && screenY < viewLocation[1] + view.getHeight();
    }

    /**
     * Use FragmentManager to attach one fragment to one view using the viewId.
     *
     * @param viewId   used to obtain the view.
     * @param fragment to be attached.
     */
    private void addFragmentToView(final int viewId, final Fragment fragment) {
        fragmentManager.beginTransaction().replace(viewId, fragment).commit();
    }

    /**
     * Initialize the viewDragHelper.
     */
    private void initializeViewDragHelper() {
        viewDragHelper = ViewDragHelper.create(this, SENSITIVITY,
                new DraggableViewCallback(this, dragView));
    }

    /**
     * Initialize Transformer with a scalable or change width/height implementation.
     */
    private void initializeTransformer(TypedArray attributes) {
        boolean topViewResize = attributes.getBoolean(R.styleable.draggable_view_top_view_resize,
                DEFAULT_TOP_VIEW_RESIZE);
        TransformerFactory transformerFactory = new TransformerFactory();
        transformer = transformerFactory.getTransformer(topViewResize, dragView, resizableView,
                this);

        transformer.setXScaleFactor(
                attributes.getFloat(R.styleable.draggable_view_top_view_x_scale_factor,
                        DEFAULT_SCALE_FACTOR));
        transformer.setYScaleFactor(
                attributes.getFloat(R.styleable.draggable_view_top_view_y_scale_factor,
                        DEFAULT_SCALE_FACTOR));
        transformer.setMarginRight(
                attributes.getDimensionPixelSize(R.styleable.draggable_view_top_view_margin_right,
                        DEFAULT_TOP_VIEW_MARGIN));
        transformer.setMarginBottom(
                attributes.getDimensionPixelSize(R.styleable.draggable_view_top_view_margin_bottom,
                        DEFAULT_TOP_VIEW_MARGIN));

        transformer.setMinHeight(
                attributes.getDimensionPixelSize(R.styleable.draggable_view_minimized_height, 0));
        transformer.setMinWidth(
                attributes.getDimensionPixelSize(R.styleable.draggable_view_minimized_width, 0));
    }

    /**
     * Initialize XML attributes.
     *
     * @param attrs to be analyzed.
     */
    private void initializeAttributes(AttributeSet attrs) {
        TypedArray attributes = getContext().obtainStyledAttributes(attrs,
                R.styleable.draggable_view);
        this.enableHorizontalAlphaEffect = attributes.getBoolean(
                R.styleable.draggable_view_enable_minimized_horizontal_alpha_effect,
                DEFAULT_ENABLE_HORIZONTAL_ALPHA_EFFECT);
        this.enableClickToMaximize = attributes.getBoolean(
                R.styleable.draggable_view_enable_click_to_maximize_view,
                DEFAULT_ENABLE_CLICK_TO_MAXIMIZE);
        this.enableClickToMinimize = attributes.getBoolean(
                R.styleable.draggable_view_enable_click_to_minimize_view,
                DEFAULT_ENABLE_CLICK_TO_MINIMIZE);
        this.attributes = attributes;
    }

    /**
     * Realize an smooth slide to an slide offset passed as argument. This method is the base of
     * maximize, minimize and close methods.
     *
     * @param slideOffset to apply
     * @return true if the view is slided.
     */
    private boolean smoothSlideTo(float slideOffset) {
        int y = calculateSlideTargetY(slideOffset);

        lastSlideTargetY = y;
        lastSlideOffset = slideOffset;

        if (viewDragHelper.smoothSlideViewTo(dragView, 0, y)) {
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }

        return false;
    }

    private int calculateSlideTargetY(float slideOffset) {
        return (int) (getPaddingTop() + slideOffset * getVerticalDragRange());
    }

    /**
     * Calculate the dragged view left position normalized between 1 and 0.
     *
     * @return absolute value between the dragged view  left position divided by custon view width
     */
    private float getHorizontalDragOffset() {
        return (float) Math.abs(dragView.getLeft()) / (float) getWidth();
    }

    /**
     * Calculate the dragged view  top position normalized between 1 and 0.
     *
     * @return dragged view top divided by vertical drag range.
     */
    private float getVerticalDragOffset() {
        float verticalDragRange = getVerticalDragRange();
        float offset = verticalDragRange != 0 ? dragView.getTop() / verticalDragRange : 0;

        if (offset > 1) {
            return 1;
        } else if (offset < 0) {
            return 0;
        } else {
            return offset;
        }
    }

    /**
     * Calculate the vertical drag range between the custom view and dragged view.
     *
     * @return the difference between the custom view height and the dragged view height.
     */
    private float getVerticalDragRange() {
        return getHeight() - transformer.getMinHeightPlusMargin();
    }

    private void onStateChanged() {
        @State int newState;
        if (isMaximized()) {
            newState = STATE_MAXIMIZED;
        } else if (isClosed()) {
            newState = STATE_CLOSED;
        } else if (isMinimized()) {
            newState = STATE_MINIMIZED;
        } else {
            newState = STATE_DRAG;
        }

        if (listener != null && lastNotifiedState != newState) {
            lastNotifiedState = newState;
            stateChanged = true;
            listener.onStateChanged(newState);
        }
    }

    public int getDraggedViewHeightPlusMarginTop() {
        return transformer.getMinHeightPlusMargin();
    }

    public interface OnStateChangedListener {

        void onStateChanged(@State int state);
    }
}
