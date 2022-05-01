package com.mobilex.lookit.utils;

import android.graphics.Bitmap;

import com.mobilex.lookit.db.DBManager;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

public class Memo {
    private final int memoId;
    private String title;
    private String content;
    private final String filename;
    private final String userId;
    private String shareId;

    private Mat queryImage;
    private Mat memoImage;

    public Memo(int id, String title, String content, String filename,
                String userId, String shareId) {
        this.memoId = id;
        this.title = title;
        this.content = content;
        this.filename = filename;
        this.userId = userId;
        this.shareId = shareId;
    }

    public Mat getMemoImage() {
        if (memoImage == null) {
            memoImage = new Mat();
            Bitmap bitmap = new MemoBitmap(title, content, 1080)
                    .getBitmap();
            Utils.bitmapToMat(bitmap, memoImage);
            Core.flip(memoImage.t(), memoImage, 0);
        }

        return memoImage;
    }

    public Mat getQueryImage(DBManager dbManager) {
        if (queryImage == null) {
            String json = dbManager.getJsonFromFile(filename);
            queryImage = MatJson.matFromJson(json);
        }

        return queryImage;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public boolean isEditable(String lastCredential) {
        return lastCredential.equals(userId);
    }

    public int getMemoId() {
        return memoId;
    }

    public String getShareId() {
        return shareId;
    }

    public String getFilename() {
        return filename;
    }
}
