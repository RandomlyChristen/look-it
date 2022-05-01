//
// Created by 이수균 on 2021/06/25.
//

#include <jni.h>
#include <vector>
#include "opencv2/opencv.hpp"
#include "com_mobilex_lookit_CaptureActivity.h"
#include "com_mobilex_lookit_LookActivity.h"
#include "com_mobilex_lookit_utils_Matcher.h"

using namespace cv;
using namespace std;

extern "C" {

Ptr<Feature2D> detector;
Ptr<DescriptorMatcher> matcher;

struct memo {
    vector<KeyPoint> query_keypoints;
    Mat query_descriptor;
    Mat memo_image;
};

vector<memo> memos;

Mat result_mat;

JNIEXPORT void JNICALL Java_com_mobilex_lookit_utils_Matcher_getResult
        (JNIEnv *, jobject, jlong resultMatRgba) {
    if (result_mat.empty()) return;

    vector<Mat> channels;
    split(result_mat, channels);

    Mat mask = channels[3];

    Mat& result_mat_rgba = *(Mat*)resultMatRgba;
    result_mat.copyTo(result_mat_rgba, mask);
//    result_mat_rgba = result_mat;
}

JNIEXPORT void JNICALL Java_com_mobilex_lookit_utils_Matcher_matchQuery
        (JNIEnv *, jobject, jlong trainGrayAddr) {
    Mat train_mat = *(Mat*)trainGrayAddr;
    vector<KeyPoint> train_keypoints;
    Mat train_descriptor;
    Mat current_result(train_mat.rows, train_mat.cols,
                       CV_8UC4, Scalar(0, 0, 0, 0));

    detector->detectAndCompute(train_mat, noArray(), train_keypoints, train_descriptor);

    for (auto& memo : memos) {
        if (memo.query_descriptor.empty() || train_descriptor.empty()) continue;
        vector<vector<DMatch>> matches;

        matcher->knnMatch(memo.query_descriptor, train_descriptor, matches, 2);

        float RATIO_THRESH = 0.7f;
        int MATCH_THRESH = 20;

        vector<DMatch> good_matches;
        for (auto& matched_pair : matches) {
            if (matched_pair[0].distance
                    < RATIO_THRESH * matched_pair[1].distance)
                good_matches.push_back(matched_pair[0]);
        }

        if (good_matches.size() > MATCH_THRESH) {
            vector<Point2f> pts1, pts2;
            Mat homography;

            for (auto& match : good_matches) {
                pts1.push_back(memo.query_keypoints[match.queryIdx].pt);
                pts2.push_back(train_keypoints[match.trainIdx].pt);
            }

            homography = findHomography(pts1, pts2, RANSAC, 5.0);
            if (homography.empty()) continue;


            Mat local_result(train_mat.rows, train_mat.cols,
                               CV_8UC4, Scalar(0, 0, 0, 0));
            warpPerspective(memo.memo_image, local_result, homography,
                            Size(train_mat.cols, train_mat.rows));

            current_result += local_result;
        }
    }

    result_mat = current_result;
}

JNIEXPORT void JNICALL Java_com_mobilex_lookit_LookActivity_initCV
        (JNIEnv *, jobject) {
    memos.clear();
    detector = ORB::create(5000);
//    detector = SIFT::create();
    matcher = BFMatcher::create();
//    matcher = FlannBasedMatcher::create();
}

JNIEXPORT void JNICALL Java_com_mobilex_lookit_LookActivity_addMemo
        (JNIEnv *, jobject,
         jlong queryMatAddr, jlong imageMatAddr) {
    Mat& query_mat = *(Mat*)queryMatAddr;
    Mat& image_mat = *(Mat*)imageMatAddr;

    memo m;
    detector->detectAndCompute(query_mat, noArray(), m.query_keypoints, m.query_descriptor);
    m.memo_image = image_mat;

    memos.push_back(m);
}



JNIEXPORT void JNICALL Java_com_mobilex_lookit_CaptureActivity_drawAndGetInterest
        (JNIEnv *, jobject,
         jlong p_input, jlong p_interest, jlong p_memo) {

    Mat& input = *(Mat*)p_input;
    Mat& interest = *(Mat*)p_interest;
    Mat& memo = *(Mat*)p_memo;
    Rect interest_rect((input.cols - input.rows) / 2, 0,
                       input.rows, input.rows);

    interest = input(interest_rect).clone();
    cvtColor(interest, interest, COLOR_RGBA2GRAY);

    input(interest_rect) += memo;

    rectangle(input, interest_rect,
              Scalar(255, 255, 255), 2);
}


}