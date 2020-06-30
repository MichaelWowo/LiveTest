package com.credenceid.sample.face;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.neurotec.biometrics.NBiometricCaptureOption;
import com.neurotec.biometrics.NBiometricOperation;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NBiometricTask;
import com.neurotec.biometrics.NFace;
import com.neurotec.biometrics.NLAttributes;
import com.neurotec.biometrics.NLivenessAction;
import com.neurotec.biometrics.NLivenessMode;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.client.NBiometricClient;
import com.neurotec.images.NImage;
import com.neurotec.lang.NCore;
import com.neurotec.licensing.NLicenseManager;
import com.neurotec.util.NCollectionChangedAction;
import com.neurotec.util.event.NCollectionChangeEvent;
import com.neurotec.util.event.NCollectionChangeListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;


public class CredenceHandlerThread extends Thread {

    Handler handler;
    private static NBiometricClient sBiometricClient;
    private boolean mIsNeurotecInit= false;
    //private static final String[] LICENSES = new String[]{"FaceClient"};
    private static final String[] LICENSES = new String[]{"FaceFastExtractor"};
    private Context mAppCtx;
    //public Handler mHandler;
    private static NFace sFace;
    private static NSubject sSubject;
    private static NBiometricTask sTask;
    private static NBiometricStatus sNStatus;
    public static Handler sImageHandler = null;
    private static List<NLAttributes> sMonitorredAtributes;
    private static PropertyChangeListener sPptChangeListener;

    private static final String TAG = "Credence ID SampleFace";

    private boolean mIsNeurotecInitialized;

    public CredenceHandlerThread(Context appContext) {
        super();
        mAppCtx = appContext;

        mIsNeurotecInitialized = false;

        System.setProperty("jna.nounpack", "true");
        System.setProperty("java.io.tmpdir", mAppCtx.getCacheDir().getAbsolutePath());
        NCore.setContext(mAppCtx);
        NLicenseManager.initialize();

        init();

        mIsNeurotecInitialized = true;

        sMonitorredAtributes = new ArrayList<>();

        sPptChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                Log.d(TAG, "sPptChangeListener - propertyChange");
                repaint();
            }
        };

        //mBiometricClient.setFacesLivenessMode(NLivenessMode.ACTIVE);
        sSubject.getFaces().add(sFace);

        try {
            // Start extraction from stream
            sBiometricClient.setFacesLivenessMode(NLivenessMode.ACTIVE);
            sFace.setCaptureOptions(EnumSet.of(NBiometricCaptureOption.STREAM));
            sFace.setHasMoreSamples(true);
            for(NLAttributes item : sFace.getObjects()){
                Log.d(TAG, "sMonitorredAtributes - set");
                sMonitorredAtributes.add(item);
                Log.d(TAG, "sMonitorredAtributes - set Property");
                item.addPropertyChangeListener(sPptChangeListener);
            }
            sTask = sBiometricClient.createTask(EnumSet.of(NBiometricOperation.CREATE_TEMPLATE), sSubject);
            sFace.addPropertyChangeListener(sPptChangeListener);
            sFace.getObjects().addCollectionChangeListener(new NCollectionChangeListener() {
                @Override
                public void collectionChanged(NCollectionChangeEvent nCollectionChangeEvent) {
                    Log.d(TAG, "NCollectionChangeListener - collectionChanged");
                    if (0 == nCollectionChangeEvent.getAction().compareTo(NCollectionChangedAction.RESET)){
                        Log.d(TAG, "NCollectionChangeListener - RESET");
                        for(Object item : sMonitorredAtributes) {
                            NLAttributes attributes = (NLAttributes) item;
                            attributes.removePropertyChangeListener(sPptChangeListener);
                        }
                        sMonitorredAtributes.clear();
                    }else{
                        if(0 == nCollectionChangeEvent.getAction().compareTo(NCollectionChangedAction.ADD)){
                            Log.d(TAG, "NCollectionChangeListener - ADD");
                            for(Object item : nCollectionChangeEvent.getNewItems()) {
                                NLAttributes attributes = (NLAttributes) item;
                                sMonitorredAtributes.add(attributes);
                                attributes.addPropertyChangeListener(sPptChangeListener);
                            }
                        } else {
                            if(0 == nCollectionChangeEvent.getAction().compareTo(NCollectionChangedAction.REMOVE)) {
                                Log.d(TAG, "NCollectionChangeListener - REMOVE");
                                for (Object item : nCollectionChangeEvent.getNewItems()) {
                                    NLAttributes attributes = (NLAttributes) item;
                                    sMonitorredAtributes.remove(attributes);
                                    attributes.removePropertyChangeListener(sPptChangeListener);
                                }
                            }
                        }
                    }
                    Log.d(TAG, "NCollectionChangeListener - collectionChanged");
                    repaint();
                }
            });
            //sTask = sBiometricClient.createTask(EnumSet.of(NBiometricOperation.DETECT), sSubject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleImage(Uri uriImage){
        sImageHandler.obtainMessage(0, uriImage).sendToTarget();
        Log.d(TAG, "Image received - send to target");
    }

    @SuppressLint("HandlerLeak")
    public void run() {
        Looper.prepare();

        sImageHandler = new Handler() {
            @Override
            public void handleMessage(Message inputMessage) {
                switch (inputMessage.what) {
                    case 0:
                        Log.d(TAG, "Image received in Handler");
                        if(mIsNeurotecInit) {
                            Uri imageUri = (Uri) inputMessage.obj;
                            Log.d(TAG, "Image URI = " + imageUri.toString());
                            NImage nFaceImage = null;
                            try {
                                nFaceImage = NImageUtils.fromUri(mAppCtx, imageUri);
                                sFace.setImage(nFaceImage);
                                if(null == sBiometricClient){
                                    Log.e(TAG, "sBiometricClient is null");
                                }
                                if(null == sTask){
                                    Log.e(TAG, "sTask is null");
                                }

                                long start_time_task = System.currentTimeMillis();

                                // Perform task
                                sBiometricClient.performTask(sTask);

                                    Log.d(TAG, "Neurotec detectFace performTask time = " + (System.currentTimeMillis()-start_time_task));

                                Throwable taskError = sTask.getError();
                                if (taskError != null) {
                                    Log.e(TAG, "mBiometricClient ERROR : " + taskError.getMessage());
                                }

                                Rect rect = null;

                                // Extract results
                                if (sTask.getStatus() == NBiometricStatus.OK) {
                                    NFace.ObjectCollection mAttributes = sSubject.getFaces().get(0).getObjects();
                                    Log.d(TAG, "mAttributes size = " + mAttributes.size());
                                    if (mAttributes.size() > 0) {

                                        Log.d(TAG, "Template CREATED.");

                                        NLAttributes nLAttributes = mAttributes.get(0);
                                        rect = nLAttributes.getBoundingRect();
                                        Log.d(TAG, "rect left=" + rect.left
                                                + ", top=" + (rect.top)
                                                + ", right=" + rect.right
                                                + ", bottom=" + rect.bottom);
                                    }
                                } else {
                                    Log.d(TAG, "sTask result  = " + sTask.getStatus().toString());
                                }

                                Log.d(TAG, "Template created");

                                /*Message replyMessage = Message.obtain(null, CREATE_TEMPLATE_LIVENESS_THREAD_IMAGE, ServiceConstants.RESULT_OK, 0);
                                Bundle bundle = new Bundle();
                                bundle.putParcelable(FACE_RECT, rect!=null?new RectF(rect.left, rect.top-30, rect.right, rect.bottom):null);
                                replyMessage.setData(bundle);
                                mCredenceService.sendMessageToClient(replyMessage);*/


//                                sBiometricClient.performTask(sTask);
//                                Throwable taskError = sTask.getError();
//                                if (taskError != null) {
//                                    Log.e(TAG, "mBiometricClient ERROR : " + taskError.getMessage());
//                                    mCredenceService.sendWarningMessageToClient(CREATE_TEMPLATE_LIVENESS_THREAD_IMAGE,
//                                            taskError.getMessage());
//                                }
//                                Log.d(TAG, "sImageHandler sTask.getStatus() = " + sTask.getStatus());
//                                Log.d(TAG, "sImageHandler sTask subject Size =" + sTask.getSubjects().size());
//                                Log.d(TAG, "sImageHandler sSubject getFace Size =" + sSubject.getFaces().size());
//                                if (sTask.getStatus() == NBiometricStatus.OK) {
//                                    Log.i(TAG, "Template extracted");
//                                    if (sSubject.getFaces().get(0).getObjects().size() > 1) {
//                                        // Get detection details if the face was detected
//                                        for (NLAttributes attributes :
//                                                sSubject.getFaces().get(0).getObjects()) {
//
//                                            Log.i(TAG, "face:");
//                                            Log.i(TAG, "location = (" + attributes.getBoundingRect().centerX() +
//                                                    " , " + attributes.getBoundingRect().centerY() + " )\n" +
//                                                    "width = " + (attributes.getBoundingRect().right - attributes.getBoundingRect().left) +
//                                                    " height = " + (attributes.getBoundingRect().bottom - attributes.getBoundingRect().top));
//
//                                            if (attributes.getRightEyeCenter().confidence > 0 || attributes.getLeftEyeCenter().confidence > 0) {
//                                                Log.i(TAG, "found eyes:");
//                                                if (attributes.getRightEyeCenter().confidence > 0)
//                                                    Log.i(TAG, "location = (" + attributes.getRightEyeCenter().x +
//                                                            " , " + attributes.getRightEyeCenter().y + " )\n" +
//                                                            "confidence =  " + attributes.getRightEyeCenter().confidence);
//                                                if (attributes.getLeftEyeCenter().confidence > 0)
//                                                    Log.i(TAG, "location = (" + attributes.getLeftEyeCenter().x +
//                                                            " , " + attributes.getLeftEyeCenter().y + " )\n" +
//                                                            "confidence =  " + attributes.getLeftEyeCenter().confidence);
//                                            }
//                                        }
//
//                                        // Save compressed template to file
//                                        Log.i(TAG, "Template saved successfully");
//                                        mCredenceService.sendMessageToClient(CREATE_TEMPLATE_LIVENESS_THREAD_IMAGE,
//                                                ServiceConstants.RESULT_OK);
//                                    }
//                                } else {
//                                Log.e(TAG, "Extraction failed: " + sTask.getStatus());
//                                    mCredenceService.sendWarningMessageToClient(CREATE_TEMPLATE_LIVENESS_THREAD_IMAGE,
//                                            sTask.getStatus().toString());
//                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.e(TAG, e.toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e(TAG, e.toString());
                            }
                        }else{
                            Log.e(TAG, "Neurotec is not initialized");
                        }

                        Log.d(TAG, "FaceEngine - CREATE_TEMPLATE_LIVENESS_THREAD_IMAGE - Image treated");

                        break;
                }
            }
        };
        Looper.loop();
    }

    static void repaint()
    {
        NLAttributes[] attributesArray = null;
        Log.d(TAG, "repaint, sSubject.getFaces() Size = " + sSubject.getFaces().size());
        if(sSubject.getFaces().size()>0) {
            Log.d(TAG, "repaint, sSubject sFace Image size= " + sSubject.getFaces().get(0).getImage().getImageSize());
            Log.d(TAG, "repaint, sSubject getObject size = " + sSubject.getFaces().get(0).getObjects().size());
            if(sSubject.getFaces().get(0).getObjects().size()>0){
                attributesArray = (NLAttributes[])sSubject.getFaces().get(0).getObjects().toArray();
                Log.d(TAG, "repaint, sSubject sFace attributesArray Size = " + attributesArray.length);
                if(attributesArray.length > 1) {
                    Rect rect = attributesArray[0].getBoundingRect();
                        Log.d(TAG, "rect left=" + rect.left
                            + ", top=" + (rect.top)
                            + ", right=" + rect.right
                            + ", bottom=" + rect.bottom);

                }

            }
        }

        if(null != attributesArray){
            Log.d(TAG, "repaint, attributesArray Size = " + attributesArray.length);

            for (int i = 0; i < attributesArray.length; i++)
            {

                NLAttributes attributes = attributesArray[i];
                EnumSet<NLivenessAction> action = attributes.getLivenessAction();
                Log.d(TAG, "repaint, attributes action size = " + action.size());
                Log.d(TAG, "repaint, Liveness Score = " + Byte.valueOf(attributes.getLivenessScore()));
                if(action.size()>1){
                    Log.d(TAG, "repaint, attributes action :");
                    Log.d(TAG, "=> " + (action.equals(NLivenessAction.ROTATE_YAW)?"YAW":" NOT YAW"));
                    Log.d(TAG, "=> " + (action.equals(NLivenessAction.KEEP_ROTATING_YAW)?"KEEP_ROTATING_YAW":" NOT KEEP_ROTATING_YAW"));
                    Log.d(TAG, "=> " + (action.equals(NLivenessAction.BLINK)?"BLINK":" NOT BLINK"));
                    Log.d(TAG, "=> " + (action.equals(NLivenessAction.KEEP_STILL)?"KEEP_STILL":" NOT KEEP_STILL"));
                    Log.d(TAG, "=> " + (action.equals(NLivenessAction.TURN_DOWN)?"TURN_DOWN":" NOT TURN_DOWN"));
                    Log.d(TAG, "=> " + (action.equals(NLivenessAction.TURN_LEFT)?"TURN_LEFT":" NOT TURN_LEFT"));
                    Log.d(TAG, "=> " + (action.equals(NLivenessAction.TURN_RIGHT)?"TURN_RIGHT":" NOT TURN_RIGHT"));
                    Log.d(TAG, "=> " + (action.equals(NLivenessAction.TURN_TO_CENTER)?"TURN_TO_CENTER":" NOT TURN_TO_CENTER"));
                    Log.d(TAG, "=> " + (action.equals(NLivenessAction.TURN_UP)?"TURN_UP":" NOT TURN_UP"));
                    boolean rotation = action.equals(NLivenessAction.ROTATE_YAW);
                    boolean blink = action.equals(NLivenessAction.BLINK);
                    boolean keepStill = action.equals(NLivenessAction.KEEP_STILL);

                    if (rotation)
                    {
                        float yaw = attributes.getYaw();
                        float targetYaw = attributes.getLivenessTargetYaw();
                        if (targetYaw > yaw)
                        {
                            Log.i(TAG, "rotate right");
                        }
                        if (yaw > targetYaw)
                        {
                            Log.i(TAG, "rotate left");
                        }
                    }

                    if (blink)
                        Log.i(TAG, "Blink");

                    if (keepStill)
                    {
                        Log.i(TAG, "Keep still");
                    }
                }

            }
        }else{
            Log.w(TAG, "attributes array is null");
        }
    }


    private void init() {

        Log.i(TAG, "Initialisation starting");

        try {
            NeurotecLicensingManager.getInstance().obtain(mAppCtx, neurotecFaceEngineAdditionalComponents());
            if (NeurotecLicensingManager.getInstance().obtain(mAppCtx, neurotecFaceEngineMandatoryComponents())) {
                Log.i(TAG, "Neurotech Face Engine Licenses obtained.");
            } else {
                mIsNeurotecInitialized = false;
                Log.i(TAG, "Neurotech Face Engine Licenses partially obtained.");
            }
        } catch (Exception e) {
            mIsNeurotecInitialized = false;
            Log.i(TAG, "Neurotech Face Engine License obtain failed - " +e.toString());
        }

        sBiometricClient = new NBiometricClient();

        if(null != sBiometricClient){
            // We initialize the client with features that will be required.
            sBiometricClient.setFacesDetectAllFeaturePoints(true);
            sBiometricClient.setFacesDetectBaseFeaturePoints(true);
            sBiometricClient.setFacesRecognizeExpression(true);
            sBiometricClient.setFacesDetectProperties(true);
            sBiometricClient.setFacesDetermineGender(true);
            sBiometricClient.setFacesDetermineAge(true);
            // Initialize NBiometricClient
            sBiometricClient.initialize();
            mIsNeurotecInitialized = true;
            Log.i(TAG, "Neurotech Face Client initialized.");
        }
        // Initialize NBiometricClient
        sSubject = new NSubject();
        sFace = new NFace();
        mIsNeurotecInit = true;
    }


    public static List<String> neurotecFaceEngineAdditionalComponents() {
        return Arrays.asList(NeurotecLicensingManager.LICENSE_FACE_STANDARDS,
                NeurotecLicensingManager.LICENSE_FACE_MATCHING_FAST,
                NeurotecLicensingManager.LICENSE_FACE_SEGMENTS_DETECTION);
    }

    public static List<String> neurotecFaceEngineMandatoryComponents() {
        return Arrays.asList(NeurotecLicensingManager.LICENSE_FACE_DETECTION,
                NeurotecLicensingManager.LICENSE_FACE_EXTRACTION,
                NeurotecLicensingManager.LICENSE_FACE_MATCHING);
    }
}
