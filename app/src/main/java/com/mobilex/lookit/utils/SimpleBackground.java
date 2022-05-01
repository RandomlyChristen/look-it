package com.mobilex.lookit.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.mobilex.lookit.R;

public class SimpleBackground<T> {
    private T result;

    public SimpleBackground(Activity activity, String msg) {
        Dialog dialog = new Dialog(activity);
        @SuppressLint("InflateParams") View view =
                ((LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.progress_dialog, null);
        dialog.setContentView(view);
        dialog.setCancelable(false);
        TextView msgView = dialog.findViewById(R.id.progress_msg);
        msgView.setText(msg);
        dialog.show();

        new Thread(() -> {
            result = SimpleBackground.this.run();
            activity.runOnUiThread(() -> {
                dialog.dismiss();
                after(result);
            });
        }).start();
    }

    public T getResult() {
        return result;
    }

    public T run() {
        return null;
    }
    public void after(T result) {}
}
