package com.slaviboy.switchcontroller;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Copyright (c) 2019 Stanislav Georgiev. (MIT License)
 * https://github.com/slaviboy
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * - The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * - The Software is provided "as is", without warranty of any kind, express or
 * implied, including but not limited to the warranties of merchantability,
 * fitness for a particular purpose and noninfringement. In no event shall the
 * authors or copyright holders be liable for any claim, damages or other
 * liability, whether in an action of contract, tort or otherwise, arising from,
 * out of or in connection with the Software or the use or other dealings in the
 * Software.
 * <p>
 * SwitchController Class (Java)
 * Class that creates switch controllers, that are used to move 2D, 3D
 * objects and are fully customizable.
 */
public class SwitchController extends ConstraintLayout {

    // controller constants -directions
    public static final int DIRECTION_CENTER = 0;
    public static final int DIRECTION_LEFT = 1;
    public static final int DIRECTION_RIGHT = 2;
    public static final int DIRECTION_UP = 3;
    public static final int DIRECTION_DOWN = 4;


    private float activeOpacity;                    // opacity when the user is using the switch controller
    private float inactiveOpacity;                  // opacity when the user is not using the controller
    private int currentDirection;                   // current recorded direction
    private int previousDirection;                  // previous recorded direction
    private boolean isSticky;                       // if the foreground in is stick to the main four directions, if finger is outside the background
    private double angle;                           // rotary angle between parent view and current finger position
    private double distance;                        // finger distance from the center
    private boolean isActive;                       // whether or not the controller is active if finger is pressed down and is in virtual circle
    private boolean keepInside;                     // whether or not to keep the foreground inside the background
    private int[] actions;                          // attached actions to the controller
    private boolean detectTransparency;             // detect events when finger is on the transparent area outside the background
    private Const c;                                // object with constant that are changes when the background or foreground sizes are changed
    private ImageView bgImageView;                  // background image view
    private ImageView fgImageView;                  // foreground image view
    private ControllerListener controllerListener;   // attached listener that implement methods, that will be called

    private int controllerId;                       // current controller id
    private static int controllerCounter = 0;       // static id counter


    public SwitchController(Context context) {
        this(context, null);
        init(context, null);
    }

    public SwitchController(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SwitchController(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public SwitchController(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public SwitchController(Context context, View parentView, int width, int height, Drawable background, Drawable foreground,
                            float activeOpacity, float inactiveOpacity, boolean isSticky, boolean detectTransparency,
                            boolean keepInside) {
        this(context);

        // set properties
        if (background != null) {
            bgImageView.setImageDrawable(background);
        }
        if (foreground != null) {
            fgImageView.setImageDrawable(foreground);
        }
        this.activeOpacity = activeOpacity;
        this.inactiveOpacity = inactiveOpacity;
        this.isSticky = isSticky;
        this.detectTransparency = detectTransparency;
        this.keepInside = keepInside;
        setAlpha(inactiveOpacity);
        if (background != null || foreground != null) {
            updateImageViews();
        }

        // create layout params holding -width and -height
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(width, height);
        this.setLayoutParams(params);

        // attach to parent if available
        if (parentView != null) {
            ((ViewGroup)parentView).addView(this);
        }
    }

    private void init(Context context, AttributeSet attrs) {

        // check if view already inflated by the number of child
        if (this.getChildCount() == 0) {

            // set default background color
            if (getBackground() == null) {
                setBackgroundColor(Color.TRANSPARENT);
            }

            // inflate the merge xml into the layout
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View parent = inflater.inflate(R.layout.controller, this, true);

            // get imageViews
            bgImageView = parent.findViewById(R.id.background);
            fgImageView = parent.findViewById(R.id.foreground);

            // set id only on the first inflation
            controllerId = controllerCounter;
            controllerCounter++;
        }


        postOnPreDraw(new Runnable() {
            @Override
            public void run() {
                updateImageViews();
            }
        }, this);

        // custom xml attributes
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.switch_controller);
            activeOpacity = typedArray.getFloat(R.styleable.switch_controller_active_opacity, 1);
            inactiveOpacity = typedArray.getFloat(R.styleable.switch_controller_inactive_opacity, 1);
            isSticky = typedArray.getBoolean(R.styleable.switch_controller_is_sticky, false);
            detectTransparency = typedArray.getBoolean(R.styleable.switch_controller_detect_transparency, false);
            keepInside = typedArray.getBoolean(R.styleable.switch_controller_keep_inside, false);
            typedArray.recycle();

            setAlpha(inactiveOpacity);
        }
    }

    /**
     * Set the size for both imageViews -background and -foreground
     * Creates padding half the foreground size, so the foreground
     * can move half its size outside the background.
     */
    public void updateImageViews() {

        // check if drawables are available
        if (bgImageView.getDrawable() == null || fgImageView.getDrawable() == null) {
            return;
        }

        // background image size
        int bgImageWidth = bgImageView.getDrawable().getIntrinsicWidth();
        int bgImageHeight = bgImageView.getDrawable().getIntrinsicHeight();

        // foreground image size
        int fgImageWidth = fgImageView.getDrawable().getIntrinsicWidth();
        int fgImageHeight = fgImageView.getDrawable().getIntrinsicHeight();

        // constraint layout size
        int parentViewWidth = getWidth();
        int parentViewHeight = getHeight();

        double ratioWidth = (double) fgImageWidth / bgImageWidth;
        double ratioHeight = (double) fgImageHeight / bgImageHeight;

        // set background image view size to match (parent size - fgImageView size)
        LayoutParams bgParams = (LayoutParams) bgImageView.getLayoutParams();
        bgParams.width = (int) (parentViewWidth / (1 + ratioWidth));
        bgParams.height = (int) (parentViewHeight / (1 + ratioHeight));
        bgImageView.setLayoutParams(bgParams);

        // set foreground image view size
        LayoutParams fgParams = (LayoutParams) fgImageView.getLayoutParams();
        fgParams.width = (int) (bgParams.width * ratioWidth);
        fgParams.height = (int) (bgParams.height * ratioHeight);
        fgImageView.setLayoutParams(fgParams);


        // set temp constants, that way they are not recalculated each time new onTouch event is called
        c = new Const(
                bgParams.width, bgParams.height,
                fgParams.width, fgParams.height,
                getWidth(), getHeight());
    }

    /**
     * Static method used to set runnable, used to observe and call method run()
     * after final measurement is made, and view is about to be drawn.
     *
     * @param runnable - runnable that will be called
     * @param view - observed view
     */
    public static void postOnPreDraw(final Runnable runnable, final View view) {

        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                try {
                    runnable.run();
                    return true;
                } finally {
                    view.getViewTreeObserver().removeOnPreDrawListener(this);
                }
            }
        });
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int maskedAction = event.getActionMasked();
        switch (maskedAction) {

            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {

                // finger position
                float x = event.getX();
                float y = event.getY();

                if (detectTransparency) {
                    // detect event on transparent area

                    isActive = true;

                    // move foreground
                    move(x, y);

                    // change parent opacity to active
                    setAlpha(activeOpacity);
                } else {

                    // detect event on circle area around the background

                    // view center point
                    double centerX = c.centerX;
                    double centerY = c.centerY;

                    // maximum allowed finger distance from the center
                    float maxDistance = keepInside ? c.minBgHalf : c.minBgHalf + c.minFgHalf;

                    // check if finger is inside the circle
                    if (Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2) < Math.pow(maxDistance, 2)) {
                        isActive = true;

                        // move foreground
                        move(x, y);

                        // change parent opacity to active
                        setAlpha(activeOpacity);
                    }
                }

                break;
            }
            case MotionEvent.ACTION_MOVE: {

                // move foreground
                if (isActive) {
                    move(event.getX(), event.getY());
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {

                // only if controller is active
                if (!isActive) {
                    break;
                }

                // set previous and current directions
                previousDirection = currentDirection;
                currentDirection = DIRECTION_CENTER;

                // call listener if available
                if (controllerListener != null) {
                    int currentAction = (actions != null) ? actions[currentDirection] : -1;
                    controllerListener.onDirectionChange(controllerId, currentDirection, currentAction);
                    controllerListener.onMove(controllerId, angle, distance);
                }

                // restore foreground position
                fgImageView.setX(c.middleX);
                fgImageView.setY(c.middleY);

                // change parent opacity to inactive
                setAlpha(inactiveOpacity);

                isActive = false;
                break;
            }
        }
        invalidate();

        return true;
    }

    /**
     * Find the point lying on a line between two points -center and -finger
     * and located from the center point to a -distance.
     *
     * @param centerX  x coordinates
     * @param centerY  y coordinates
     * @param fingerX  x coordinates
     * @param fingerY  y coordinates
     * @param distance desired distance from the center
     * @return
     */
    private PointF pointOnLine(double centerX, double centerY,
                               double fingerX, double fingerY, double distance) {

        double ratio = distance / this.distance;

        // point coordinates distant from start point
        double x = (1 - ratio) * centerX + ratio * fingerX;
        double y = (1 - ratio) * centerY + ratio * fingerY;

        return new PointF((float) x, (float) y);
    }


    /**
     * Move -foreground depending on current finger coordinates
     * on screen, given from onTouch events
     *
     * @param fingerX x coordinate
     * @param fingerY y coordinate
     */
    private void move(float fingerX, float fingerY) {

        // delta and distance between view center and finger points
        double deltaX = fingerX - c.centerX;
        double deltaY = fingerY - c.centerY;
        distance = Math.sqrt(
                Math.pow(deltaX, 2) + Math.pow(deltaY, 2));

        // maximum foreground distance from center
        float maxDistance = keepInside ? c.minBgHalf - c.minFgHalf : c.minBgHalf;

        if (Math.abs(distance) > maxDistance) {

            //finger is outside the background area
            PointF movePoint = pointOnLine(c.centerX, c.centerY,
                    fingerX, fingerY, maxDistance);
            fgImageView.setX(movePoint.x - c.fgHalfWidth);
            fgImageView.setY(movePoint.y - c.fgHalfHeight);
        } else {

            //finger is inside the background area - move freely
            fgImageView.setX(fingerX - c.fgHalfWidth);
            fgImageView.setY(fingerY - c.fgHalfHeight);
        }

        // get rotational angle in degrees
        angle = angleBetween(c.centerX, c.centerY, fingerX, fingerY);

        // set last and get the current direction
        previousDirection = currentDirection;
        if (-deltaY > Math.abs(deltaX)) {
            currentDirection = DIRECTION_UP;
        } else if (deltaY > Math.abs(deltaX)) {
            currentDirection = DIRECTION_DOWN;
        } else if (-deltaX > Math.abs(deltaY)) {
            currentDirection = DIRECTION_LEFT;
        } else if (deltaX > Math.abs(deltaY)) {
            currentDirection = DIRECTION_RIGHT;
        }

        // call listener methods
        if (controllerListener != null) {

            // if direction is changed
            if (previousDirection != currentDirection) {
                int currentAction = (actions != null) ? actions[currentDirection] : -1;
                controllerListener.onDirectionChange(controllerId, currentDirection, currentAction);
            }

            // if finger is moved
            controllerListener.onMove(controllerId, angle, distance);
        }


        // stick the foreground
        if (isSticky && Math.abs(distance) > c.minBgHalf) {
            switch (currentDirection) {

                case DIRECTION_UP: {
                    fgImageView.setX(c.upX);
                    if (keepInside) {
                        fgImageView.setY(c.upY + c.minFgHalf);
                    } else {
                        fgImageView.setY(c.upY);
                    }
                }
                break;
                case DIRECTION_LEFT: {

                    fgImageView.setY(c.leftY);
                    if (keepInside) {
                        fgImageView.setX(c.leftX + c.minFgHalf);
                    } else {
                        fgImageView.setX(c.leftX);
                    }
                }
                break;
                case DIRECTION_DOWN: {
                    fgImageView.setX(c.downX);
                    if (keepInside) {
                        fgImageView.setY(c.downY - c.minFgHalf);
                    } else {
                        fgImageView.setY(c.downY);
                    }
                }
                break;
                case DIRECTION_RIGHT: {
                    fgImageView.setY(c.rightY);
                    if (keepInside) {
                        fgImageView.setX(c.rightX - c.minFgHalf);
                    } else {
                        fgImageView.setX(c.rightX);
                    }
                }
                break;
            }
        }

    }

    /**
     * Returns current direction value in a form of a string
     *
     * @return
     */
    public String getDirectionAsString() {
        return getDirectionAsString(currentDirection);
    }

    /**
     * Static method that converts direction into a string value
     *
     * @param direction
     * @return
     */
    public static String getDirectionAsString(int direction) {
        switch (direction) {
            case DIRECTION_CENTER:
                return "DIRECTION_CENTER";
            case DIRECTION_LEFT:
                return "DIRECTION_LEFT";
            case DIRECTION_RIGHT:
                return "DIRECTION_RIGHT";
            case DIRECTION_UP:
                return "DIRECTION_UP";
            case DIRECTION_DOWN:
                return "DIRECTION_DOWN";
        }
        return "";
    }


    /**
     * Get angle between two point -center of the view and -finger
     * position on screen
     *
     * @param centerX
     * @param centerY
     * @param fingerX
     * @param fingerY
     * @return
     */
    private double angleBetween(double centerX, double centerY, double fingerX, double fingerY) {

        double angleDegree = (Math.atan2(-(fingerY - centerY), fingerX - centerX) * 180 / Math.PI);
        return (angleDegree < 0) ? angleDegree + 360 : angleDegree;
    }

    /**
     * Update the drawable for background image view
     * @param drawable - new drawable for the view
     */
    public void updateBackground(Drawable drawable) {
        bgImageView.setImageDrawable(drawable);
    }

    /**
     * Update the drawable for foreground image view
     * @param drawable - new drawable for the view
     */
    public void updateForeground(Drawable drawable) {
        fgImageView.setImageDrawable(drawable);
    }

    /**
     * Get current rotational angle in degrees
     * @return
     */
    public double getAngle() {
        return angle;
    }

    /**
     * Set array with actions
     * @param actions
     */
    public void setActions(int[] actions) {
        this.actions = actions;
    }



    public void setControllerListner(ControllerListener controllerListener) {
        this.controllerListener = controllerListener;
    }

    /**
     * Controller Listener with methods that will be called
     * for a specific situation
     */
    public interface ControllerListener {

        /**
         * Call when controller direction is changed
         *
         * @param id - controller id, to identify controller for multiple controllers
         * @param direction - one of five base direction - up, down, left, right or center
         * @param action - actions that correspond to current direction
         */
        void onDirectionChange(int id, int direction, int action);

        /**
         * Called when finger is moving on top of the controller switch view
         * @param id - controller id, to identify controller for multiple controllers
         * @param angle - current rotational angle for the switch controller (degrees)
         * @param distance - finger distance from the center of the switch controller
         */
        void onMove(int id, double angle, double distance);
    }


    /**
     * Class with temporary constants, that are use by onTouch events,
     * they are changed only when parents view size is changed, or when
     * new drawable for -background and -foreground is set and there
     * size is changed. That way those constants are nor recalculated each
     * time a new onTouch event is called
     */
    private class Const {

        public float centerX;
        public float centerY;
        public float fgHalfWidth;
        public float fgHalfHeight;
        public float bgHalfWidth;
        public float bgHalfHeight;
        public float minBgHalf;
        public float minFgHalf;

        // foreground direction positions
        public float middleX;
        public float middleY;
        public float leftX;
        public float leftY;
        public float rightX;
        public float rightY;
        public float upX;
        public float upY;
        public float downX;
        public float downY;

        public Const(float bgWidth, float bgHeight, float fgWidth, float fgHeight,
                     float width, float height) {

            // center point is the middle of the view
            centerX = width / 2;
            centerY = height / 2;

            // get half foreground view width and height
            fgHalfWidth = fgWidth / 2;
            fgHalfHeight = fgHeight / 2;

            // get half background view width and height
            bgHalfWidth = bgWidth / 2;
            bgHalfHeight = bgHeight / 2;

            // get the min from background half sizes
            minBgHalf = Math.min(bgHalfWidth, bgHalfHeight);
            minFgHalf = Math.min(fgHalfWidth, fgHalfHeight);

            // foreground positions

            // middle position in parent
            middleX = centerX - fgHalfWidth;
            middleY = centerY - fgHalfHeight;

            // maximum left position
            leftX = middleX - minBgHalf;
            leftY = middleY;

            // maximum right position
            rightX = middleX + minBgHalf;
            rightY = middleY;

            // maximum up position
            upX = middleX;
            upY = middleY - minBgHalf;

            // maximum down position
            downX = middleX;
            downY = middleY + minBgHalf;

        }
    }


    /**
     * Builder class for simple and easy switch controller creation, using JAVA
     * with setter methods for each custom property.
     */
    public static class Builder {

        private Context context;
        private View parent;
        private Drawable foreground;
        private Drawable background;
        private float activeOpacity;
        private float inactiveOpacity;
        private boolean isSticky;
        private boolean detectTransparency;
        private boolean keepInside;
        private int width;
        private int height;

        public Builder(Context context) {
            this(context, 0, 0);
        }

        public Builder(Context context, int width, int height) {

            this.context = context;
            this.width = width;
            this.height = height;

            // default values
            activeOpacity = 1;
            inactiveOpacity = 1;
            isSticky = false;
            detectTransparency = false;
            keepInside = false;
        }

        public Builder withForegroundDrawable(Drawable drawable) {
            foreground = drawable;
            return this;
        }

        public Builder withBackgroundDrawable(Drawable drawable) {
            background = drawable;
            return this;
        }

        public Builder withActiveAlpha(float alpha) {
            activeOpacity = alpha;
            return this;
        }

        public Builder withInactiveAlpha(float alpha) {
            inactiveOpacity = alpha;
            return this;
        }

        public Builder withIsSticky(boolean isSticky) {
            this.isSticky = isSticky;
            return this;
        }

        public Builder withDetectTransparency(boolean detectTransparency) {
            this.detectTransparency = detectTransparency;
            return this;
        }

        public Builder withKeepInside(boolean keepInside) {
            this.keepInside = keepInside;
            return this;
        }

        public Builder withWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder withHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder withParentView(View parentView) {
            this.parent = parentView;
            return this;
        }

        public SwitchController build() {
            return new SwitchController(context, parent, width, height, background, foreground,
                    activeOpacity, inactiveOpacity, isSticky, detectTransparency, keepInside);
        }

    }

}
