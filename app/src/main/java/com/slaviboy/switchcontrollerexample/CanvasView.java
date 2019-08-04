package com.slaviboy.switchcontrollerexample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.slaviboy.switchcontroller.SwitchController;


/**
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
 *  CanvasView Class(Java)
 *  Class that is used to draw penguin object and uses
 *  thread to redraw the scene
 */
public class CanvasView extends SurfaceView implements Runnable,
        SwitchController.ControllerListener {

    public CanvasView(Context context) {
        super(context);
        init(context);
    }

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CanvasView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public CanvasView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private SurfaceHolder surfaceHolder;  // holder for the SurfaceView

    private Thread thread;                // thread object
    private boolean isRunning = true;     // if thread is running

    private Penguin penguin;              // penguin object

    private int direction;                // switch controller current direction
    private double angle;                 // switch controller current angle
    private double distance;              // switch controller current distance

    private void init(Context context) {
        surfaceHolder = this.getHolder();
    }

    @Override
    public void run() {

        penguin = new Penguin(getContext(), "Slaviboy", 0, 0, Color.BLUE, 1);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        Canvas canvas;
        while (isRunning) {

            canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas();

                synchronized (surfaceHolder) {
                    if (canvas != null) {

                        canvas.drawColor(Color.WHITE);
                        penguin.updatePosition(direction, angle, distance, getWidth(), getHeight());
                        penguin.updateFrame(System.currentTimeMillis());
                        penguin.draw(canvas, paint);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }


    @Override
    public void onDirectionChange(int id, int direction, int action) {
        this.direction = direction;
    }

    @Override
    public void onMove(int id, double angle, double distance) {
        this.angle = angle;
        this.distance = distance;
    }

    private void startThread() {
        if (thread == null) {
            isRunning = true;
            thread = new Thread(this); // set runnable, to new thread
            thread.start();
        }
    }

    private void stopThread() {
        if (thread != null) {
            isRunning = false;
            thread.interrupt();
            thread = null;
        }
    }

    public void restart() {
        stopThread();
        startThread();
    }

    public void start() {
        startThread();
    }

    public void stop() {
        stopThread();
    }
}
