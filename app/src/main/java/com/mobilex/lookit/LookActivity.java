package com.mobilex.lookit;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.Toast;

import com.mobilex.lookit.db.DBManager;
import com.mobilex.lookit.utils.Matcher;
import com.mobilex.lookit.utils.Memo;
import com.mobilex.lookit.utils.SimpleBackground;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

public class LookActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private DBManager dbManager;
    private Matcher matcher;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_look);

        dbManager = DBManager.getInstance(this);

        cameraView = findViewById(R.id.look_camera_view);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);
        cameraView.setCameraIndex(0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        cameraView.setCameraPermissionGranted();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "라이브러리가 정상적으로 로드되지 않았습니다.",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        initLook();
        loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null) cameraView.disableView();
        if (matcher != null) matcher.destroyThread();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraView != null) cameraView.disableView();
    }

    private void initLook() {
        new SimpleBackground<Void>(this, "초기화 중") {
            @Override
            public Void run() {
                initCV();
                matcher = new Matcher();
                matcher.start();
                for (Memo memo : HomeActivity.getSelectedMemos()) {
                    addMemo(memo.getQueryImage(dbManager).getNativeObjAddr(),
                            memo.getMemoImage().getNativeObjAddr());
                }
                return null;
            }
        };
    }

    public native void initCV();
    public native void addMemo(long queryMatAddr, long imageMatAddr);

    @Override
    public void onCameraViewStarted(int width, int height) {}

    @Override
    public void onCameraViewStopped() {}

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat trainGray = inputFrame.gray();
        Mat trainRgba = inputFrame.rgba();

        matcher.setTrainGray(trainGray);
        matcher.getResult(trainRgba.getNativeObjAddr());

        return trainRgba;
    }
}