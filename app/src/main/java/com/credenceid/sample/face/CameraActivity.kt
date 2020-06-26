package com.credenceid.sample.face

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.Camera
import android.hardware.Camera.Parameters.FLASH_MODE_OFF
import android.hardware.Camera.Parameters.FLASH_MODE_TORCH
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import com.android.camera.Utils
import kotlinx.android.synthetic.main.act_camera.*
import java.io.*
import java.util.*

private val TAG = CameraActivity::class.java.simpleName

/**
 * To obtain high face detection rate we use lowest possible camera resolution for preview.
 * For the actual picture size, we will use the largest available resolution so there is no
 * loss in face image quality.
 */
private const val P_WIDTH = 320
private const val P_HEIGHT = 240
private const val syncAPITimeoutMS = 3000

/**
 * It is always good to have a global context in case non-activity classes require it. In
 * this case "Beeper" class requires it so it may grab audio file from assets.
 */
@SuppressLint("StaticFieldLeak")
private var context: Context? = null

private var camera: Camera? = null

/**
 * If true then camera is in preview, if false it is not.
 */
private var inPreview = false
/**
 * Has camera preview settings been initialized. If true yes, false otherwise. This is required
 * so camera preview does not start without it first being configured.
 */
private var mIsCameraConfigured = false
private var surfaceHolder: SurfaceHolder? = null


private var sIsrecording = false
private var mImageStream = arrayOfNulls<Bitmap>(100)
private var imageCounter = 0
var mRecordingTimer: CountDownTimer? = null

private var mImageHandler: CredenceHandlerThread? = null


@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
class CameraActivity : Activity(), SurfaceHolder.Callback {

    /**
     * This callback is invoked after camera finishes taking a picture.
     */
    private val mOnPictureTakenCallback = Camera.PictureCallback { data, _ ->
        /* Produce "camera shutter" sound so user knows that picture was captured. */
        Beeper.click(context!!)
        /* Now that picture has been taken, turn off flash. */
        setTorchEnable(false)
        /* Camera is no longer in preview. */
        inPreview = false

        try {
            val intent = Intent(this, FaceActivity::class.java)
            intent.putExtra(getString(R.string.camera_image), data)

            finish()
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()

            Toast.makeText(this, "Unable to run face analysis, retry.", Toast.LENGTH_LONG).show()
        }
    }


    /**
     * This callback is invoked on each camera preview frame. In this callback will run call face
     * detection API and pass it preview frame.
     */
    private val mCameraPreviewCallback = { data: ByteArray, _: Camera -> detectFaceAsync(data) }

    /**
     * This callback is invoked each time camera finishes auto-focusing.
     */
    private val mAutoFocusCallback = Camera.AutoFocusCallback { _, _ ->
        /* Remove previous status since auto-focus is now done. */
        statusTextView.text = ""

        /* Tell DrawingView to stop displaying auto-focus circle by giving it a region of 0. */
        drawingView.setHasTouch(false, Rect(0, 0, 0, 0))
        drawingView.invalidate()

        /* Re-enable capture button. */
        setCaptureButtonVisibility(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_camera)

        context = this
        camera = null

        this.configureLayoutComponents()

        this.reset()
        this.doPreview()
    }

    /**
     * This is required to stop camera every time back button is pressed.
     */
    override fun onBackPressed() {

        Log.d(App.TAG, "onBackPressed()")
        super.onBackPressed()
        this.stopReleaseCamera()
        this.finish()
    }

    /**
     * This is required to stop camera preview every time activity loses focus.
     */
    override fun onPause() {

        Log.d(App.TAG, "onPause()")
        super.onPause()
        this.stopReleaseCamera()
    }

    /**
     * This is required to stop camera every time application is killed.
     */
    override fun onDestroy() {

        Log.d(App.TAG, "onDestroy()")
        super.onDestroy()
        this.stopReleaseCamera()
    }

    override fun surfaceChanged(holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int) {

        if (null == camera) {
            Log.w(TAG, "Camera object is null, will not set up preview.")
            return
        }

        try {
            this.initPreview()
            this.startPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {

        surfaceHolder = holder
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

        if (null == camera)
            return

        if (inPreview)
            camera!!.stopPreview()

        camera!!.release()
        camera = null
        inPreview = false
    }

    /**
     * Configure all layout file component objects. Assigns listeners, configurations, etc.
     */
    private fun configureLayoutComponents() {

        this.setFlashButtonVisibility(true)

        /* Only CredenceTAB family of device's support 8MP back camera resolution.  */

        previewFrameLayout.visibility = VISIBLE
        drawingView.visibility = VISIBLE
        scanImageView.visibility = VISIBLE
        if(null!=progressBar)
            progressBar!!.visibility = INVISIBLE

        surfaceHolder = scanImageView.holder
        surfaceHolder!!.addCallback(this)
        surfaceHolder!!.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        captureBtn.setOnClickListener { v: View ->
            if (!inPreview) {
                this.reset()
                this.doPreview()

                captureBtn.text = getString(R.string.capture_label)
            } else if (camera != null)
                doCapture()
        }

        startLivenessBtn.setOnClickListener {
//            App.BioManager!!.startThreadForFaceTemplateWithLiveness { resultCode: Biometrics.ResultCode, bytes: ByteArray ->
//                statusTextView.text = "Start thread res = " + resultCode
//            }

            mImageHandler = CredenceHandlerThread(applicationContext)
            mImageHandler!!.start()
        }


        stopLivenessBtn.setOnClickListener {
//            App.BioManager!!.stopThreadForFaceTemplateWithLiveness{ resultCode: Biometrics.ResultCode, bytes: ByteArray ->
//                statusTextView.text = "Stop thread res = " + resultCode
//            }
            mImageHandler!!.interrupt()
            mImageHandler = null
        }

        captureLivenessBtn.setOnClickListener{
            //new InitializationTask().execute();
            if(!sIsrecording){
                sIsrecording = true;
                mRecordingTimer = object: CountDownTimer(20000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {}

                    override fun onFinish() {
                        Log.d(TAG, "Timer Finished")
                        statusTextView.text = getString(R.string.stop_recoding_timeout)
                        sIsrecording = false
                        imageCounter = 0
                    }
                }
                Log.d(TAG, "Start recording")
                statusTextView.text = getString(R.string.start_recoding) + "\n" + getString(R.string.start_recoding_instruction)
                (mRecordingTimer as CountDownTimer).start()
            }else{
                mRecordingTimer?.cancel()
                sIsrecording = false
                imageCounter = 0
                Log.d(TAG, "stop recording (User)")
                statusTextView.text = getString(R.string.stop_recoding_user)
            }
        }

        flashOnBtn.setOnClickListener { this.setTorchEnable(true) }
        flashOffBtn.setOnClickListener { this.setTorchEnable(false) }
    }

    private fun sendImageToFaceEngine(img: Bitmap){
        if(sIsrecording){
            val sd_main = File(Environment.getExternalStorageDirectory().toString() + "/FaceSamples/")
            var success = true
            if (!sd_main.exists())
                success = sd_main.mkdir()

            if (success) {

                // directory exists or already created
                var outputFile: File? = File(sd_main, "face-image-from-camera-$imageCounter.jpg")
                if (imageCounter < 100) {
                    if (imageCounter < 10) {
                        outputFile = File(sd_main, "face-image-from-camera-00$imageCounter.jpg")
                    } else {
                    outputFile = File(sd_main, "face-image-from-camera-0$imageCounter.jpg")
                    }
                }

                try{
                    // Compress the bitmap and save in jpg format
                    val stream: OutputStream = FileOutputStream(outputFile)
                    img?.compress(Bitmap.CompressFormat.JPEG,100,stream)
                    if (outputFile != null) {
                        Log.d(TAG, "Uri " + Uri.parse(outputFile.absolutePath) + " send to service")
//                        App!!.BioManager?.sendImageToThreadForFaceTemplateWithLiveness(Uri.parse(outputFile.absolutePath)) { resultCode: Biometrics.ResultCode, bytes: ByteArray ->
//                            statusTextView.text = "Send image to thread res = " + resultCode
//                        }

                        mImageHandler!!.handleImage( Uri.parse(outputFile.absolutePath))

                    }
                    stream.flush()
                    stream.close()
                    imageCounter++
                }catch (e:IOException){
                    e.printStackTrace()
                }
            }
        }
    }

//    private fun performFaceCreateTemplateWithLiveness(){
//        Log.d(TAG, "Image stream length = " + imageCounter);
//
//        val sd_main = File(Environment.getExternalStorageDirectory().toString() + "/FaceSamples/")
//        var success = true
//        if (!sd_main.exists())
//            success = sd_main.mkdir()
//
//        if (success) {
//
//
//            for (x in 0 until imageCounter) {
//
//                // directory exists or already created
//                var outputFile: File? = File(sd_main, "face-image-from-camera-$x.jpg")
//                if (x < 10) {
//                    outputFile = File(sd_main, "face-image-from-camera-0$x.jpg")
//                }
//                try{
//                    // Compress the bitmap and save in jpg format
//                    val stream: OutputStream = FileOutputStream(outputFile)
//                    mImageStream[x]?.compress(Bitmap.CompressFormat.JPEG,100,stream)
//                    stream.flush()
//                    stream.close()
//                }catch (e:IOException){
//                    e.printStackTrace()
//                }
//            }
//        }
//
//        statusTextView.text = statusTextView.text.toString() + "\n" + getString(R.string.template_creation_start)
//        progressBar.setVisibility(View.VISIBLE)
//
//        App.BioManager!!.createFaceTemplateWithLivenessDetection(mImageStream){ rc: Biometrics.ResultCode,
//                                                                                template ->
//            /* If we got back data, populate CropView and other widgets with face data. */
//            when (rc) {
//                OK -> {
//                    statusTextView.text = getString(R.string.template_result_success)
//                    progressBar.setVisibility(View.INVISIBLE)
//                }
//                INTERMEDIATE -> {
//                    /* This code is never returned for this API. */
//                }
//                FAIL -> {
//                    statusTextView.text = getString(R.string.template_result_failed)
//                    progressBar.setVisibility(View.INVISIBLE)
//                }
//            }
//        }
//    }


    private fun initPreview() {

        if (null == camera || null == surfaceHolder!!.surface) {
            Log.d(TAG, "Either camera or SurfaceHolder was null, skip initPreview().")
            return
        }
        if (mIsCameraConfigured) {
            Log.d(TAG, "camera is already configured, no need to iniPreview().")
            return
        }

        try {
            Log.d(App.TAG, "Executing: camera.setPreviewDisplay(surfaceHolder)")
            camera!!.setPreviewDisplay(surfaceHolder)
        } catch (ignore: IOException) {
            return
        }

        /* Initialize camera preview in proper orientation. */
        this.setCameraPreviewDisplayOrientation()

        /* Get camera parameters. We will edit these, then write them back to camera. */
        val parameters = camera!!.parameters

        Log.d(App.TAG, "Camera Parameters : " + parameters.flatten())

        /* Enable auto-focus if available. */
        val focusModes = parameters.supportedFocusModes
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO

        /* For FaceEngine we show a preview with 320x240, but the actual image is
         * captured with largest available picture size, this way we get a high
         * resolution in final image.
         */
        val picSize = Utils.getLargestPictureSize(parameters)
        parameters.setPictureSize(picSize.width, picSize.height)


        /* Regardless of what size is returned we always use a 320x240 preview size for face
         * detection since it is extremely fast.
         *
         * This previewSize is used to set up dimensions of all camera views.
         */
        val previewSize = parameters.previewSize
        previewSize.width = P_WIDTH
        previewSize.height = P_HEIGHT

        previewFrameLayout.setAspectRatio(previewSize.width / previewSize.height.toDouble())

        val drawingViewLayoutParams = drawingView.layoutParams


        val prevParams = previewFrameLayout.layoutParams
        drawingViewLayoutParams.width = prevParams.width
        drawingViewLayoutParams.height = prevParams.height

        drawingView.layoutParams = drawingViewLayoutParams

        /* Need to set FaceEngine specific bitmap size so DrawingView knows
         * where and how to draw face detection points. Otherwise it would
         * assume the bitmap size is 0.
         */
        drawingView.setBitmapDimensions(P_WIDTH, P_HEIGHT)

        camera!!.parameters = parameters
        mIsCameraConfigured = true
    }

    private fun startPreview() {

        Log.d(App.TAG, "startPreview()")

        if (mIsCameraConfigured && null != camera) {
            Log.d(App.TAG, "Camera is configured & valid.")

            statusTextView.text = ""
            captureBtn.text = getString(R.string.capture_label)
            this.setCaptureButtonVisibility(true)

            previewFrameLayout.visibility = VISIBLE
            drawingView.visibility = VISIBLE
            scanImageView.visibility = VISIBLE

            Log.d(App.TAG, "Executing: camera.startPreview()")
            camera!!.startPreview()
            inPreview = true

        } else {
            Log.w(TAG, "Camera not configured, aborting start preview.")
        }
    }

    private fun doPreview() {

        Log.d(App.TAG, "doPreview()")
        try {
            /* If camera was not already opened, open it. */
            if (null == camera) {
                Log.d(App.TAG, "Camera is null, opening camera.")
                    camera = Camera.open(1)
                /* Tells camera to give us preview frames in these dimensions. */
                this.setPreviewSize(P_WIDTH, P_HEIGHT, P_WIDTH.toDouble() / P_HEIGHT)
            }

            if (null != camera) {
                Log.d(App.TAG, "Camera is not null, will set preview.")
                /* Tell camera where to draw frames to. */
                camera!!.setPreviewDisplay(surfaceHolder)
                /* Tell camera to invoke this callback on each frame. */
                camera!!.setPreviewCallback(mCameraPreviewCallback)
                /* Rotate preview frames to proper orientation based on DeviceType. */
                this.setCameraPreviewDisplayOrientation()
                /* Now we can tell camera to start preview frames. */
                this.startPreview()

            } else {
                Log.d(App.TAG, "Camera failed to open.")
            }
        } catch (e: Exception) {
            e.printStackTrace()

            if (null != camera)
                camera!!.release()

            camera = null
            inPreview = false
        }

    }

    /**
     * Tells camera to return preview frames in a certain width/height and aspect ratio.
     *
     * @param width Width of preview frames to send back.
     * @param height Height of preview frames to send back.
     * @param ratio Aspect ration of preview frames to send back.
     */
    private fun setPreviewSize(@Suppress("SameParameterValue") width: Int,
                               @Suppress("SameParameterValue") height: Int,
                               @Suppress("SameParameterValue") ratio: Double) {

        val parameters = camera!!.parameters
        parameters.setPreviewSize(width, height)
        previewFrameLayout.setAspectRatio(ratio)
        camera!!.parameters = parameters
    }

    /**
     * Tells camera to rotate captured pictured by a certain angle. This is required since on some
     * devices the physical camera hardware is 90 degrees, etc.
     */
    private fun setCameraPictureOrientation() {

        val parameters = camera!!.parameters

            parameters.setRotation(180)

        camera!!.parameters = parameters
    }

    /**
     * Tells camera to rotate preview frames by a certain angle. This is required since on some
     * devices the physical camera hardware is 90 degrees, etc.
     */
    private fun setCameraPreviewDisplayOrientation() {

        var orientation = 90

        /* For C-TAB, the BACK camera requires 0, but FRONT camera is 180. In this example FRONT
         * camera is not used, so that case was not programed in.
         */
        camera!!.setDisplayOrientation(orientation)
    }

    /**
     * Captures image, before capturing image it will set proper picture orientation.
     */
    private fun doCapture() {

        this.setCameraPictureOrientation()

        if (camera != null) {
            this.setCaptureButtonVisibility(false)
            statusTextView.text = getString(R.string.start_capture_hold_still)

            /* We are no longer going to be in preview. Set variable BEFORE telling camera to take
             * picture. Camera takes time to take a picture so we do not want any preview event to
             * take place while a picture is being captured.
             */
            inPreview = false
            camera!!.takePicture(null, null, null, mOnPictureTakenCallback)
        }
    }

    /**
     * Sets camera flash.
     *
     * @param useFlash If true turns on flash, if false disables flash.
     */
    private fun setTorchEnable(useFlash: Boolean) {

        /* If camera object was destroyed, there is nothing to do. */
        if (null == camera)
            return

        /* Camera flash parameters do not work on TAB/TRIDENT devices. In order to use flash on
         * these devices you must use the Credence APIs.
         */

        try {
            val p = camera!!.parameters
            p.flashMode = if (useFlash) FLASH_MODE_TORCH else FLASH_MODE_OFF
            camera!!.parameters = p
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Resets camera flash and UI back to camera preview state. */
    private fun reset() {

        Log.d(App.TAG, "reset()")

        /* Change capture button image to "Capture". */
        captureBtn.text = getString(R.string.capture_label)

        /* Turn off flash since new preview. */
        this.setTorchEnable(false)

        /* Display all buttons in their proper states. */
        this.setCaptureButtonVisibility(true)
        this.setFlashButtonVisibility(true)
    }

    /**
     * Stops camera preview, turns off torch, releases camera object, and sets it to null.
     */
    private fun stopReleaseCamera() {

        Log.d(App.TAG, "stopReleaseCamera()")

        if (null != camera) {
            Log.d(App.TAG, "Camera is not null, will release.")

            /* Tell camera to no longer invoke callback on each preview frame. */
            camera!!.setPreviewCallback(null)
            /* Turn off flash. */
            this.setTorchEnable(false)

            /* Stop camera preview. */
            if (inPreview) {
                Log.d(App.TAG, "Camera was in preview, executing: camera.stopPreview().")
                camera!!.stopPreview()
            }

            Log.d(App.TAG, "Executing: camera.release().")
            /* Release camera and nullify object. */
            camera!!.release()
            camera = null
            /* We are no longer in preview mode. */
            inPreview = false
        }

        /* Remove camera surfaces. */
        surfaceHolder!!.removeCallback(this)
        this.surfaceDestroyed(surfaceHolder!!)
    }

    /**
     * This method either hides or shows capture button allowing user to capture an image. This is
     * required because while camera is focusing user should not be allowed to press capture. Once
     * focusing finishes and a clear preview is available, only then should an image be allowed to
     * be taken.
     *
     * @param visibility If true button is shown, if false button is hidden.
     */
    private fun setCaptureButtonVisibility(visibility: Boolean) {

        captureBtn.visibility = if (visibility) VISIBLE else INVISIBLE
    }

    /**
     * This method either hides or shows flash buttons allowing user to control flash. This is
     * required because after an image is captured a user should not be allowed to control flash
     * since camera is no longer in preview. Instead of disabling the buttons we hide them from
     * the user.
     *
     * @param visibility If true buttons are show, if false they are hidden.
     */
    private fun setFlashButtonVisibility(@Suppress("SameParameterValue") visibility: Boolean) {

        flashOnBtn.visibility = if (visibility) VISIBLE else INVISIBLE
        flashOffBtn.visibility = if (visibility) VISIBLE else INVISIBLE
    }

    /**
     * Attempts to perform tap-to-focus on camera with given focus region.
     *
     * @param touchRect Region to focus on.
     */
    @SuppressLint("SetTextI18n")
    fun performTapToFocus(touchRect: Rect) {

        if (!inPreview)
            return

        this.setCaptureButtonVisibility(false)
        statusTextView.text = getString(R.string.autofocus_wait)

        val one = 2000
        val two = 1000

        /* Here we properly bound our Rect for a better tap to focus region */
        val targetFocusRect = Rect(
                touchRect.left * one / drawingView.width - two,
                touchRect.top * one / drawingView.height - two,
                touchRect.right * one / drawingView.width - two,
                touchRect.bottom * one / drawingView.height - two)

        /* Since Camera parameters only accept a List of  areas to focus, create a list. */
        val focusList = ArrayList<Camera.Area>()
        /* Convert Graphics.Rect to Camera.Rect for camera parameters to understand.
         * Add custom focus Rect. region to focus list.
         */
        focusList.add(Camera.Area(targetFocusRect, 1000))


        /* Call camera AutoFocus and pass callback to be called when auto focus finishes */
        camera!!.autoFocus(mAutoFocusCallback)
        /* Tell our drawing view we have a touch in the given Rect */
        drawingView.setHasTouch(true, touchRect)
        /* Tell our drawing view to Update */
        drawingView.invalidate()
    }

    private inline fun <T : Any, R> whenNotNull(input: T?, callback: (T) -> R): R? {
        return input?.let(callback)
    }

    /**
     * Attempts to detect face Rect. region in given image. If face image is found it updates
     * DrawingView on where to draw Rectangle and then tells it to perform an "onDraw()".
     *
     * @param bitmapBytes Bitmap image in byte format to run detection on.
     */
    private fun detectFaceAsync(bitmapBytes: ByteArray?) {

        /* If camera was closed, immediately after a preview callback exit out, this is to prevent
         * NULL pointer exceptions when using the camera object later on.
         */
        if (null == camera || null == bitmapBytes)
            return

        /* We need to stop camera preview callbacks from continuously being invoked while processing
         * is going on. Otherwise we would have a backlog of frames needing to be processed. To fix
         * this we remove preview callback, then re-enable it post-processing.
         *
         * - Preview callback invoked.
         * -- Tell camera to sto preview callbacks.
         * **** Meanwhile camera is still receiving frames, but continues to draw them. ****
         * -- Process camera preview frame.
         * -- Draw detected face Rect.
         * -- Tell camera to invoke preview callback with next frame.
         *
         * Using this technique does not drop camera frame-rate, so camera does not look "laggy".
         * Instead now we use every 5-th frame for face detection.
         */
        camera!!.setPreviewCallback(null)

        /* Need to fix color format of raw camera preview frames. */
        val outStream = ByteArrayOutputStream()
        val rect = Rect(0, 0, 320, 240)
        val yuvimage = YuvImage(bitmapBytes, ImageFormat.NV21, 320, 240, null)
        yuvimage.compressToJpeg(rect, 100, outStream)

        /* Save fixed color image as final good Bitmap. */
        var bm = BitmapFactory.decodeByteArray(outStream.toByteArray(), 0, outStream.size())


        sendImageToFaceEngine(bm);

        /* If camera was closed or preview stopped, immediately exit out. This is done so that
             * we do not continue to process invalid frames, or draw to NULL surfaces.
             */
            if (null == camera || !inPreview)
                return

            /* Tell camera to start preview callbacks again. */
            camera!!.setPreviewCallback(mCameraPreviewCallback)

        /* Detect face on finalized Bitmap image. */
//        App.BioManager!!.detectFace(bm) { rc: Biometrics.ResultCode,
//                                          rectF: RectF? ->
//
//            /* If camera was closed or preview stopped, immediately exit out. This is done so that
//             * we do not continue to process invalid frames, or draw to NULL surfaces.
//             */
//            if (null == camera || !inPreview)
//                return@detectFace
//
//            /* Tell camera to start preview callbacks again. */
//            camera!!.setPreviewCallback(mCameraPreviewCallback)
//
//            when (rc) {
//                OK -> {
//                    whenNotNull(rectF) {
//                        /* Tell view that it will need to draw a detected face's Rect. region. */
//                        drawingView.setHasFace(true)
//
//                        /* If CredenceTWO then bounding Rect needs to be scaled to properly fit. */
//                        if (CredenceTwo == App.DevFamily) {
//                            drawingView.setFaceRect(rectF!!.left + 40, rectF.top - 25,
//                                    rectF.right + 40, rectF.bottom - 50)
//                        } else {
//                            drawingView.setFaceRect(rectF!!.left, rectF.top,
//                                    rectF.right, rectF.bottom)
//                        }
//                    }
//                }
//                INTERMEDIATE -> {
//                    /* This code is never returned for this API. */
//                }
//                FAIL -> {
//                    /* Tell view to not draw face Rect. region on next "onDraw()" call. */
//                    drawingView.setHasFace(false)
//                }
//            }
//
//            /* Tell view to invoke an "onDraw()". */
//            drawingView.invalidate()
//        }
    }

}
