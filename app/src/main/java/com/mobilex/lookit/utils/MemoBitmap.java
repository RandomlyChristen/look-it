package com.mobilex.lookit.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.StringTokenizer;

public class MemoBitmap {
    Bitmap bitmap;

    public MemoBitmap(String title, String content, int size) {
        bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawARGB(255, 255, 223, 64);
        canvas.drawText(title, 0.5f * (float)size, 0.15f * (float)size,
                titlePaint(size));

        StringTokenizer sTokenizer = new StringTokenizer(content, "\n");

        Paint p = contentPaint(size);
        for (float lineY = 0.25f; sTokenizer.hasMoreTokens(); lineY += 0.1f) {
            String line = sTokenizer.nextToken();
            canvas.drawText(line, 0.1f * (float)size, lineY * (float)size, p);
        }


    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    private Paint titlePaint(int canvasSize) {
        Paint result = new Paint();
        result.setStyle(Paint.Style.FILL);
        result.setColor(Color.BLACK);
        result.setTextSize(100
                * (float)canvasSize / 1080);
        result.setTypeface(Typeface.DEFAULT_BOLD);
        result.setTextAlign(Paint.Align.CENTER);
        return result;
    }

    private Paint contentPaint(int canvasSize) {
        Paint result = new Paint();
        result.setStyle(Paint.Style.FILL);
        result.setColor(Color.BLACK);
        result.setTextSize(70
                * (float)canvasSize / 1080);
        result.setTypeface(Typeface.DEFAULT);
        result.setTextAlign(Paint.Align.LEFT);
        return result;
    }
}
