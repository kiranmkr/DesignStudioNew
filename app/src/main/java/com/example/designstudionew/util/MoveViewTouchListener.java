package com.example.designstudionew.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;


public class MoveViewTouchListener implements View.OnTouchListener {

    public boolean isMoveAble = true;
    public EditTextCallBacks callBacks = null;
    private GestureDetector mGestureDetector;
    private ImageView mView;
    private Context mContext;
    private ScaleGestureDetector mScaleGestureDetector;
    private float mScaleFactor = 100f;
    public int mode;
    final int zoom = 2;
    final int move = 1;
    Resources r;
    float px;
    View doubleView;

    // For moving
    private float move_orgX = -1, move_orgY = -1;

    private Float parentWidth = 0f, parentHeight = 0f;

    private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent e) {

          /*  if (mContext instanceof EditingScreen) {
                doubleTapped = true;
            }*/

            return true;
        }
    };

    public MoveViewTouchListener(Context context, ImageView view) {

        mGestureDetector = new GestureDetector(context, mGestureListener);
        mContext = context;
        mView = view;
        mode = move;
        doubleView = view;
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
//        mScaleFactor = view.getTextSize();

        // Converts  dip into its equivalent px
        float dip = 409.50f;
        r = mContext.getResources();
        px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip,
                r.getDisplayMetrics()
        );


    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        mGestureDetector.onTouchEvent(event);
        mScaleGestureDetector.onTouchEvent(event);

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                mScaleGestureDetector = new ScaleGestureDetector(mContext, new ScaleListener());
                move_orgX = event.getRawX();
                move_orgY = event.getRawY();
                callBacks.setCurrentText(v);

                break;

            case MotionEvent.ACTION_MOVE:

                float offsetX = event.getRawX() - move_orgX;
                float offsetY = event.getRawY() - move_orgY;
                v.setX(v.getX() + offsetX);
                v.setY(v.getY() + offsetY);
                move_orgX = event.getRawX();
                move_orgY = event.getRawY();

                break;

            case MotionEvent.ACTION_UP:
                callBacks.showTextControls();
                break;
            default:
                return false;
        }
        return true;
    }

    public interface EditTextCallBacks {
        void showTextControls();

        void setCurrentText(View view);
    }

    public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            Log.e("scale", "Before: " + scaleGestureDetector.getScaleFactor());
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(49f, Math.min(mScaleFactor, 299f));
            Log.e("scale", "After: " + mScaleFactor);
            if (mode == zoom) {
                if (isMoveAble) {
                    if (mScaleFactor > 50 && mScaleFactor < 300) {
                       /* if (mContext instanceof EditingActivity) {
                            //((EditingActivity) mContext).onTextSize(Math.round(mScaleFactor));

                        }*/
                    }
                }
            }
            return true;

        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mode = zoom;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            // Intentionally empty
        }
    }


}

