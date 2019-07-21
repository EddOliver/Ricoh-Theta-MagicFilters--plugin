/**
 * Copyright 2018 Ricoh Company, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theta360.opencvdetection;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;

import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;
import com.theta360.pluginlibrary.values.LedColor;
import com.theta360.pluginlibrary.values.LedTarget;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.opencv.core.Core.BORDER_DEFAULT;

public class MainActivity extends PluginActivity implements CvCameraViewListener2, ThetaController.CFCallback {

    private static final String TAG = "Plug-in::MainActivity";

    private ThetaController mOpenCvCameraView;
    private boolean isEnded = false;

    private Mat mOutputFrame;
    private BackgroundSubtractor mBackgroundSubtractor;
    private Mat mMask;
    private Mat mStructuringElement;
    private long mStartProcessingTime;
    private boolean variable = false;
    private int count = 1;



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        notificationLed3Show(LedColor.BLUE);
        Log.d(TAG, "OpenCV version: " + Core.VERSION);

        // Set a callback when a button operation event is acquired.
        setKeyCallback(new KeyCallback() {
            @Override
            public void onKeyDown(int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyReceiver.KEYCODE_CAMERA) {
                variable=true;
                    if(count>6)
                    {
                        count=1;
                    }
                    if(count==1)
                    {
                        notificationLedBlink(LedTarget.LED3, LedColor.BLUE, 100);
                    }
                    else if(count==2)
                    {
                        notificationLedBlink(LedTarget.LED3, LedColor.GREEN, 100);
                    }
                    else if(count==3)
                    {
                        notificationLedBlink(LedTarget.LED3, LedColor.CYAN, 100);
                    }
                    else if(count==4)
                    {
                        notificationLedBlink(LedTarget.LED3, LedColor.MAGENTA, 100);
                    }
                    else if(count==5)
                    {
                        notificationLedBlink(LedTarget.LED3, LedColor.YELLOW, 100);
                    }
                    else if(count==6)
                    {
                        notificationLedBlink(LedTarget.LED3, LedColor.WHITE, 100);
                    }
            }
            }

            @Override
            public void onKeyUp(int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyReceiver.KEYCODE_MEDIA_RECORD) {
                    count++;
                    if(count>6)
                    {
                        count=1;
                    }
                    if(count==1)
                    {
                        notificationLed3Show(LedColor.BLUE);
                    }
                    else if(count==2)
                    {
                        notificationLed3Show(LedColor.GREEN);
                    }
                    else if(count==3)
                    {
                        notificationLed3Show(LedColor.CYAN);
                    }
                    else if(count==4)
                    {
                        notificationLed3Show(LedColor.MAGENTA);
                    }
                    else if(count==5)
                    {
                        notificationLed3Show(LedColor.YELLOW);
                    }
                    else if(count==6)
                    {
                        notificationLed3Show(LedColor.WHITE);
                    }

                }

            }

            @Override
            public void onKeyLongPress(int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyReceiver.KEYCODE_MEDIA_RECORD) {
                    Log.d(TAG, "Do end process.");
                    closeCamera();
                }
            }
        });

        notificationCameraClose();

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (ThetaController) findViewById(R.id.opencv_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCFCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found.");
        } else {
            Log.d(TAG, "OpenCV library found inside package.");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        super.onPause();
    }

    @Override
    public void onShutter() {
        notificationAudioShutter();
    }

    @Override
    public void onPictureTaken() {

    }

    public void onCameraViewStarted(int width, int height) {
        mOutputFrame = new Mat(height, width, CvType.CV_8UC3);
        mBackgroundSubtractor = Video.createBackgroundSubtractorKNN();
        mMask = new Mat(height, width, CvType.CV_8UC1);
        mStructuringElement = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, new Size(3,3));

        mStartProcessingTime = System.currentTimeMillis();
    }

    public void onCameraViewStopped() {
        mStructuringElement.release();
        mMask.release();
        mOutputFrame.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        // detect moving area


        mOutputFrame = inputFrame.rgba();


        // wait during starting camera period, and avoid continuous shooting
        if (variable) {
            String dateTimeStr = getDateTimeStr();
            takePicture(dateTimeStr);
            saveProcessWindow(mOutputFrame, dateTimeStr);
            variable=false;
        }

        return mOutputFrame;
    }

    private void closeCamera() {
        if (isEnded) {
            return;
        }
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        close();
        isEnded = true;
    }

    private boolean canProcess() {

       if (false)
            return true;
        else
            return false;
    }

    private void takePicture(String dateTimeStr) {
        File outDir = new File(Constants.PLUGIN_DIRECTORY);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        String fileUrl = String.format("%s/%s.jpg", Constants.PLUGIN_DIRECTORY, dateTimeStr);
        mOpenCvCameraView.takePicture(fileUrl);
    }

    private void saveProcessWindow(Mat img, String dateTimeStr) {
        File outDir = new File(Constants.PLUGIN_DIRECTORY);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        String fileUrl = String.format("%s/%s_threshold.jpg", Constants.PLUGIN_DIRECTORY, dateTimeStr);

        if(count==1)
        {
            fileUrl = String.format("%s/%s_equalize.jpg", Constants.PLUGIN_DIRECTORY, dateTimeStr);
            Mat rgbImage = new Mat(img.size(), img.type());
            Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2YCrCb);
            List<Mat> channels = new ArrayList<Mat>();
            Core.split(img, channels);
            Imgproc.equalizeHist(channels.get(0), channels.get(0));
            Core.merge(channels, img);
            Imgproc.cvtColor(img, img, Imgproc.COLOR_YCrCb2BGR);
        }
        else if(count==2)
        {
            fileUrl = String.format("%s/%s_threshold.jpg", Constants.PLUGIN_DIRECTORY, dateTimeStr);
            Mat rgbImage = new Mat(img.size(), img.type());
            Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2YCrCb);
            Imgproc.threshold(img, img, 127.0, 255.0, Imgproc.THRESH_BINARY);
            Imgproc.cvtColor(img, img, Imgproc.COLOR_YCrCb2RGB);

        }
        else if(count==3)
        {
            fileUrl = String.format("%s/%s_gray.jpg", Constants.PLUGIN_DIRECTORY, dateTimeStr);
            Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2GRAY);

        }
        else if(count==4)
        {
            fileUrl = String.format("%s/%s_blur.jpg", Constants.PLUGIN_DIRECTORY, dateTimeStr);
            Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2BGR);
            Imgproc.blur(img, img, new Size(25,25));

        }
        else if(count==5)
        {
            fileUrl = String.format("%s/%s_erodedilate.jpg", Constants.PLUGIN_DIRECTORY, dateTimeStr);
            Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2GRAY);
            Imgproc.threshold(img, img, 0, 255, Imgproc.THRESH_BINARY_INV+Imgproc.THRESH_OTSU);
        }
        else if(count==6)
        {
            fileUrl = String.format("%s/%s_negative.jpg", Constants.PLUGIN_DIRECTORY, dateTimeStr);
            Core.bitwise_not(img,img);
        }


        Imgcodecs.imwrite(fileUrl, img);
        registerFile(fileUrl);
        if(count==1)
        {
            notificationLed3Show(LedColor.BLUE);
        }
        else if(count==2)
        {
            notificationLed3Show(LedColor.GREEN);
        }
        else if(count==3)
        {
            notificationLed3Show(LedColor.CYAN);
        }
        else if(count==4)
        {
            notificationLed3Show(LedColor.MAGENTA);
        }
        else if(count==5)
        {
            notificationLed3Show(LedColor.YELLOW);
        }
        else if(count==6)
        {
            notificationLed3Show(LedColor.WHITE);
        }
    }


    private String getDateTimeStr() {
        Date date = new Date(System.currentTimeMillis());

        String format = "yyyyMMddHHmmss";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        String text = sdf.format(date);
        return text;
    }


    private void registerFile(String path) {
        Uri uri = Uri.fromFile(new File(path));
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        sendBroadcast(mediaScanIntent);
    }

}
