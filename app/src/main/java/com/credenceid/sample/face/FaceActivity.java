package com.credenceid.sample.face;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.camera.Utils;
import com.credenceid.biometrics.Biometrics;
import com.credenceid.face.FaceEngine;

import java.util.ArrayList;

import static android.widget.Toast.LENGTH_LONG;
import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;
import static com.credenceid.biometrics.DeviceFamily.CredenceTwo;

public class FaceActivity
        extends Activity {

    private ProgressDialog mAnalyzingDialog;

    private ImageView mImageView;
    /* Displays detected faces gender. */
    private TextView mGenderTextView;
    /* Displays detected faces age. */
    private TextView mAgeTextView;
    /* Displays is detected face is wearing glasses or not. */
    private TextView mGlassesTextView;
    /* Displays detected faces emotion. */
    private TextView mDominantEmotionTextView;
    /* Displays what direction detected face is looking in. */
    private TextView mHeadPoseDirTextView;
    /* Displays image quality of detected face. */
    private TextView mImageQualityTextView;
    /* Button to re-capture image from camera. Goes back to previous "Activity". */
    private Button mReCaptureButton;

    @Override
    protected void
    onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_face);

        mAnalyzingDialog = new ProgressDialog(this);
        mAnalyzingDialog.setMessage(getString(R.string.processing));
        mAnalyzingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mAnalyzingDialog.setIndeterminate(true);
        mAnalyzingDialog.setCancelable(false);

        this.initializeLayoutComponents();
        this.configureLayoutComponents();

        /* If bytes were given to this activity, perform face analysis. */
        byte[] imageBytes = getIntent().getByteArrayExtra(getString(R.string.camera_image));
        if (null != imageBytes) {
            Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (CredenceTwo == App.DevFamily)
                image = Utils.rotateBitmap(image, 90);

            this.detectFace(image);
        } else {
            this.onBackPressed();

            new Handler().postDelayed(() ->
                            Toast.makeText(getApplicationContext(),
                                    "Failed to detect face.",
                                    LENGTH_LONG).show(),
                    1000);
        }
    }

    @Override
    public void
    onBackPressed() {

        super.onBackPressed();
        startActivity(new Intent(getApplicationContext(), CameraActivity.class));
    }

    /* --------------------------------------------------------------------------------------------
     *
     * Initialize & configure layout components..
     *
     * --------------------------------------------------------------------------------------------
     */

    private void
    initializeLayoutComponents() {

        mImageView = findViewById(R.id.face_imageview);
        mGenderTextView = findViewById(R.id.gender_textview);
        mAgeTextView = findViewById(R.id.age_textview);
        mGlassesTextView = findViewById(R.id.glasses_textview);
        mHeadPoseDirTextView = findViewById(R.id.pose_dir_textview);
        mDominantEmotionTextView = findViewById(R.id.emotion_textview);
        mImageQualityTextView = findViewById(R.id.image_quality_textview);
        mReCaptureButton = findViewById(R.id.recapture_button);
    }

    private void
    configureLayoutComponents() {

        mReCaptureButton.setOnClickListener(v -> onBackPressed());
    }

    /* --------------------------------------------------------------------------------------------
     *
     * Private helpers.
     *
     * --------------------------------------------------------------------------------------------
     */

    @SuppressLint("SetTextI18n")
    @SuppressWarnings("StatementWithEmptyBody")
    public void
    detectFace(Bitmap bitmap) {

        /* If invalid Bitmap, display Toast and exit out.*/
        if (null == bitmap) {
            Toast.makeText(this, getString(R.string.no_image_found_to_process), LENGTH_LONG).show();
            return;
        }

        /* Display dialog so user knows an operation is in progress. */
        this.showProgressDialog();

        /* Create new scaled image to run analysis on. */
        mImageView.setImageBitmap(bitmap);

        App.BioManager.analyzeFace(bitmap, (Biometrics.ResultCode resultCode,
                                            RectF rectF,
                                            ArrayList<PointF> arrayList,
                                            ArrayList<PointF> arrayList1,
                                            float[] floats,
                                            FaceEngine.HeadPoseDirection[] poseDir,
                                            FaceEngine.Gender gender,
                                            int age,
                                            FaceEngine.Emotion emotion,
                                            boolean glasses,
                                            int imageQuality) -> {

            /* If we got back data, populate CropView and other widgets with face data. */
            if (OK == resultCode) {
                mGenderTextView.setText(getString(R.string.gender_colon_arg) + gender.name());
                mAgeTextView.setText(getString(R.string.age_colon_arg) + age);
                mGlassesTextView.setText(getString(R.string.glasses_colon_arg) + glasses);
                mDominantEmotionTextView.setText(getString(R.string.emotiona_colon_arg) + emotion.name());
                mImageQualityTextView.setText(getString(R.string.imagequal_colon_arg) + imageQuality + "%");

                String text = getString(R.string.headposedir_colon_arg);
                if (poseDir[1] == FaceEngine.HeadPoseDirection.STRAIGHT) {
                    if (poseDir[2] == FaceEngine.HeadPoseDirection.STRAIGHT)
                        text += "STRAIGHT";
                    else
                        text += poseDir[2].name();

                } else if (poseDir[2] == FaceEngine.HeadPoseDirection.STRAIGHT)
                    text += poseDir[1].name();
                else text += poseDir[1].name() + "\n  &\n" + poseDir[2].name();

                mHeadPoseDirTextView.setText(text);

            } else if (INTERMEDIATE == resultCode) {
                /* This code is never returned for this API. */

            } else if (FAIL == resultCode) {
                this.onBackPressed();

                new Handler().postDelayed(() ->
                                Toast.makeText(getApplicationContext(),
                                        "Failed to detect face.",
                                        LENGTH_LONG).show(),
                        1000);
            }

            this.dismissProgressDialog();
        });
    }

    public void
    showProgressDialog() {

        if (!mAnalyzingDialog.isShowing())
            mAnalyzingDialog.show();
    }

    public void
    dismissProgressDialog() {

        if (mAnalyzingDialog.isShowing())
            mAnalyzingDialog.dismiss();
    }

}