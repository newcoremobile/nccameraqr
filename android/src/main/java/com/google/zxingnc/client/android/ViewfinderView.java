/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxingnc.client.android;

import android.graphics.*;
import com.google.zxingnc.ResultPoint;
import com.google.zxingnc.client.android.camera.CameraManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import xinheyun.com.scanner.R;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 16L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;
    private static final int CORNER_LINE_LENGTH = 40;
    private static final int CORNER_LINE_WIDTH = 5;

    private CameraManager cameraManager;
    private final Paint paint;
    private final Paint cornerPaint;
    private Bitmap resultBitmap;
    private final int maskColor;
    private final int resultColor;
    private final int laserColor;
    private final int resultPointColor;
    private final int finderCornerColor;
    private int scannerAlpha;
    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;
    private int animLineTop = 0;
    private final Paint textPaint;
    private final float textWidth;
    private final float textTop;
    private final String text = "对准二维码到框内即可扫描";
    private boolean isDrawText = true;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(38);
        textWidth = textPaint.measureText(text);
        textTop = getResources().getDimension(R.dimen.dp32);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        resultColor = resources.getColor(R.color.result_view);
        laserColor = resources.getColor(R.color.viewfinder_laser);
        finderCornerColor = resources.getColor(R.color.viewfinder_corner);
        resultPointColor = resources.getColor(R.color.possible_result_points);
        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = null;
    }

    public void enableDrawText(boolean isDraw){
        isDrawText = isDraw;
    }
    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        Rect frame = cameraManager.getFramingRect();
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        if (animLineTop + 10 >= frame.bottom || animLineTop == 0) {
            animLineTop = frame.top;
        } else {
            animLineTop += 10;
        }
        Bitmap lineBmp = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_preview_line);
        int d = (lineBmp.getWidth() - frame.right + frame.left) / 2;
        frame.left -= d;
        frame.right += d;
        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        // corner dector
        cornerPaint.setColor(finderCornerColor);

// 左上角
        canvas.drawRect(frame.left - 1, frame.top - 1, frame.left + CORNER_LINE_LENGTH, frame.top + CORNER_LINE_WIDTH, cornerPaint);
        canvas.drawRect(frame.left - 1, frame.top - 1, frame.left + CORNER_LINE_WIDTH, frame.top + CORNER_LINE_LENGTH, cornerPaint);
// 右上角
        canvas.drawRect(frame.right - CORNER_LINE_LENGTH, frame.top - 1, frame.right + 1, frame.top + CORNER_LINE_WIDTH, cornerPaint);
        canvas.drawRect(frame.right - CORNER_LINE_WIDTH, frame.top - 1, frame.right + 1, frame.top + CORNER_LINE_LENGTH, cornerPaint);
// 左下角
        canvas.drawRect(frame.left - 1, frame.bottom - CORNER_LINE_WIDTH, frame.left + CORNER_LINE_LENGTH, frame.bottom + 1, cornerPaint);
        canvas.drawRect(frame.left - 1, frame.bottom - CORNER_LINE_LENGTH, frame.left + CORNER_LINE_WIDTH, frame.bottom + 1, cornerPaint);
// 右下角
        canvas.drawRect(frame.right - CORNER_LINE_LENGTH, frame.bottom - CORNER_LINE_WIDTH, frame.right + 1, frame.bottom + 1, cornerPaint);
        canvas.drawRect(frame.right - CORNER_LINE_WIDTH, frame.bottom - CORNER_LINE_LENGTH, frame.right + 1, frame.bottom + 1, cornerPaint);

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {
            canvas.drawBitmap(lineBmp, frame.left + 2, animLineTop, new Paint());
            if(isDrawText){
                canvas.drawText(text, frame.left + (frame.right-frame.left-textWidth)/2, frame.bottom + textTop, textPaint);
            }
            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY,
                    frame.left - POINT_SIZE,
                    frame.top - POINT_SIZE,
                    frame.right + POINT_SIZE,
                    frame.bottom + POINT_SIZE);
        }
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

}
