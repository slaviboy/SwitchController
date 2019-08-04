package com.slaviboy.switchcontrollerexample;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.slaviboy.switchcontroller.SwitchController;

import static com.slaviboy.switchcontrollerexample.Base.hideSystemUI;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    CanvasView canvasView;
    SwitchController switchController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // canvas view
        canvasView = findViewById(R.id.canvas);
        canvasView.setOnClickListener(this);

        // switch controller
        switchController = findViewById(R.id.controller);
        switchController.setControllerListner(canvasView);
        switchController.setOnClickListener(this);
        switchController.setControllerListner(canvasView);
        //createSwitch();
    }

    /**
     * Create switch controller using builder class
     */
    private void createSwitch() {

        // parent view
        View parentView = findViewById(android.R.id.content);

        // get drawables from resources
        Drawable bg = getResources().getDrawable(R.drawable.bg_controller1);
        Drawable fg = getResources().getDrawable(R.drawable.fg_controller2);

        // create switch controller using - builder
        SwitchController switchController2 = new SwitchController.Builder(this)
                .withParentView(parentView)
                .withWidth(222)
                .withHeight(999)
                .withBackgroundDrawable(bg)
                .withForegroundDrawable(fg)
                .withInactiveAlpha(0.5f)
                .withActiveAlpha(1)
                .withIsSticky(true)
                .withDetectTransparency(false)
                .withKeepInside(true)
                .build();
        switchController2.setBackgroundColor(Color.BLUE);
    }


    @Override
    protected void onStart() {
        super.onStart();
        canvasView.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        canvasView.stop();
    }


    @Override
    public void onClick(View v) {
        v.requestFocus();
        hideSystemUI((Activity) v.getContext());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI(this);
        }
    }

}
