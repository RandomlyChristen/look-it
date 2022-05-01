package com.mobilex.lookit.utils;

import org.opencv.core.Mat;

public class Matcher extends Thread {
    private boolean loopFlag = true;
    private boolean threadFlag = true;

    private Mat trainGray;
    public void setTrainGray(Mat trainGray) { this.trainGray = trainGray; }

    @Override
    public void run() {
        while (threadFlag) {
            if (!loopFlag) continue;
            if (trainGray == null) continue;
            matchQuery(trainGray.getNativeObjAddr());
        }
    }

    public void pauseThread() {
        loopFlag = false;
    }

    public void resumeThread() {
        loopFlag = true;
    }

    public void destroyThread() {
        threadFlag = false;
    }

    public native void getResult(long resultMatRgba);
    private native void matchQuery(long trainGrayAddr);
}
