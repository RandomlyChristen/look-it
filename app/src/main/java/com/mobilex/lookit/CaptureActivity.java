package com.mobilex.lookit;

import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.Toast;

import com.mobilex.lookit.utils.MatJson;
import com.mobilex.lookit.utils.MemoBitmap;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import androidx.appcompat.app.AppCompatActivity;

public class CaptureActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private CameraBridgeViewBase cameraView;
    private final BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                cameraView.enableView();
                return;
            }
            super.onManagerConnected(status);
        }
    };

    private final Mat interest = new Mat();
    private final Mat memo = new Mat();

    private static String interestJson;
    public static String getInterestJson() { return interestJson; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        cameraView = findViewById(R.id.make_camera_view);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);
        cameraView.setCameraIndex(0);

        Button sendBtn = findViewById(R.id.make_send_btn);
        sendBtn.setOnClickListener(view -> sendCaptureResult());
    }

    @Override
    protected void onStart() {
        super.onStart();
        cameraView.setCameraPermissionGranted();
    }

    @Override
    protected void onResume() {
        super.onResume();
        interestJson = null;

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "라이브러리가 정상적으로 로드되지 않았습니다.",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null) cameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraView != null) cameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        String[] titleContent = HomeActivity.getFocusedTitleAndContent();
        Utils.bitmapToMat(
                new MemoBitmap(titleContent[0], titleContent[1], height).getBitmap(), memo);
        Core.flip(memo.t(), memo, 0);
    }

    public native void drawAndGetInterest(long input, long interest, long memo);

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat input = inputFrame.rgba();
        drawAndGetInterest(input.getNativeObjAddr(), interest.getNativeObjAddr(),
                memo.getNativeObjAddr());
        return input;
    }

    @Override
    public void onCameraViewStopped() {}

    private void sendCaptureResult() {
        interestJson = MatJson.matToJson(interest);
        setResult(RESULT_OK);
        finish();
    }
}