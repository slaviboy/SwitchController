package com.slaviboy.switchcontrollerexample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import com.slaviboy.switchcontroller.SwitchController;


/**
 *
 * Copyright (c) 2019 Stanislav Georgiev. (MIT License)
 * https://github.com/slaviboy
 *
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
 *
 *
 * Penguin Class(Java)
 * That is used to create penguin object that can be drawn on canvas
 */

public class Penguin {

    public static final int STATE_STILL = 0;
    public static final int STATE_MOVE = 1;

    private float x;               // penguin x position
    private float y;               // penguin y position
    private int width;             // penguin width
    private int height;            // penguin height
    private int color;             // penguin color

    private String name;           // penguin name
    private Rect nameBound;        // name text bound - width, height

    private Typeface typeface;     // typeface for penguin name
    private int fontSize;          // font size

    private Bitmap[][] images;     // bitmap images with the penguin

    private int stateIndex;        // state index showing current penguin state
    private int frameIndex;        // current frame
    private int reframeTime;       // change frame every -speed ms
    private long lastTime;         // last system time a frame is changed
    private double speed;          // penguin moving speed

    public Penguin(Context context, String name, int x, int y, int color, double sizePercentage) {

        this.name = name;
        this.x = x;
        this.y = y;
        this.color = color;


        reframeTime = 41; // 1000ms/24frame = 41 ms/f
        stateIndex = 1;

        // set bitmap size
        width = (int) (270 * sizePercentage);
        height = (int) (300 * sizePercentage);

        loadImages(context, "images/penguin/blue/");

        fontSize = (int) ((double) width / 5);
        typeface = Typeface.DEFAULT; // Typeface.createFromAsset(context.getAssets(), "fonts/BurbankBigRegular-Medium.otf");
        nameBound = measureText(name);
    }

    /**
     * Load all penguin images from assets folder to a 2-dimensional
     * bitmap array, first index correspond to -stateIndex, second
     * index is for -frameIndex
     *
     * @param context
     * @param url
     */
    public void loadImages(Context context, String url) {

        images = new Bitmap[9][8];
        for (int i = 0; i < images.length; i++) {
            for (int j = 0; j < images[0].length; j++) {
                Bitmap temp = Base.getBitmapFromAssets(context, url + i + "/" + j + ".png");
                images[i][j] = temp; //resizedBitmap(temp, width, height);
            }
        }
    }

    /**
     * Update frame index, the range is between [0,7],
     * update is needed for the states with animations
     *
     * @param currentTime
     */
    public void updateFrame(long currentTime) {

        // only for moving states
        if (stateIndex == STATE_STILL) {
            return;
        }

        // if re-frame time is reached update frame index
        if (currentTime - lastTime > reframeTime) {

            lastTime = currentTime;

            frameIndex++;
            if (frameIndex > 7) {
                frameIndex = 0;
            }
        }
    }

    /**
     * Update penguin position, using the switch controller angle
     *
     * @param direction
     * @param angle
     * @param distance
     * @param viewWidth
     * @param viewHeight
     */
    public void updatePosition(int direction, double angle, double distance,
                               int viewWidth, int viewHeight) {

        speed = distance / 30;
        speed = 8;

        // get new state depending on angle
        int newState = 0;
        if (angle >= 247.5 && angle < 292.5) {
            newState = 1;
        } else if (angle >= 202.5 && angle < 247.5) {
            newState = 2;
        } else if (angle >= 157.5 && angle < 202.5) {
            newState = 3;
        } else if (angle >= 112.5 && angle < 157.5) {
            newState = 4;
        } else if (angle >= 67.5 && angle < 112.5) {
            newState = 5;
        } else if (angle >= 22.5 && angle < 67.5) {
            newState = 6;
        } else if ((angle >= 337.5 && angle <= 360) || (angle >= 0 && angle < 22.5)) {
            newState = 7;
        } else if (angle >= 292.5 && angle < 337.5) {
            newState = 8;
        }

        // if penguin is moving
        if (direction !=  SwitchController.DIRECTION_CENTER) {

            // calculate new x,y positions
            double angleRad = Math.toRadians(angle);
            double newX = this.x + Math.cos(angleRad) * speed;
            double newY = this.y - Math.sin(angleRad) * speed;

            // move penguin to new -x only if penguin is in canvas bound
            if (newX > 0 && newX < viewWidth - this.width) {
                this.x = (float) newX;
            }

            // move penguin to new -y only if penguin is in canvas bound
            if (newY > 0 && newY < viewHeight - this.height) {
                this.y = (float) newY;
            }

            // only if new state
            if (newState != stateIndex) {
                setState(newState);
            }
        } else {
            // penguin is static
            setState(0);
            setFrame(newState - 1);
        }
    }


    public void setFrame(int index) {
        frameIndex = index;
        lastTime = System.currentTimeMillis();
    }

    public void setState(int index) {
        stateIndex = index;
        setFrame(0);
    }

    /**
     * Measure text and return the bound -width and height
     *
     * @param text
     * @return
     */
    private Rect measureText(String text) {

        Paint paint = new Paint();
        Rect bounds = new Rect();
        paint.setTypeface(typeface);
        paint.setTextSize(fontSize);
        paint.getTextBounds(text, 0, text.length(), bounds);
        return bounds;
    }

    /**
     * Draw penguin bitmap and name
     *
     * @param canvas
     * @param paint
     */
    public void draw(Canvas canvas, Paint paint) {
        //canvas.scale(2, 2);

        // draw bitmap
        Bitmap bitmap = images[stateIndex][frameIndex];
        canvas.drawBitmap(bitmap, x, y, paint);

        // draw text
        paint.setTextSize(fontSize);
        paint.setTypeface(typeface);
        double deltaX = (double) (width - nameBound.width()) / 2;
        double deltaY = height + 20;
        canvas.drawText(name, x + (int) deltaX, y + (int) deltaY, paint);
    }
}
