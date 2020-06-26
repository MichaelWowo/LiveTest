package com.credenceid.sample.face;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.neurotec.licensing.NLicense;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class NeurotecLicensingManager {

    // ===========================================================
    // Credence ID constant definition
    // ===========================================================
    private final String SERVER_ADDRESS = "/local";
    private final int SERVER_PORT = 5000;


    // ===========================================================

    public interface LicensingStateCallback {
        void onLicensingStateChanged(NeurotecLicensingState state);
    }

    // ===========================================================
    // Public nested class
    // ===========================================================
    // Private static fields
    // ===========================================================

    private static final String TAG = NeurotecLicensingManager.class.getSimpleName();
    public static final String LICENSE_MEDIA = "Media";

    public static final String LICENSE_IMAGES_WSQ = "Images.WSQ";

    private static NeurotecLicensingManager sInstance;

    // ===========================================================
    // Public static fields
    // ===========================================================


    public static final String LICENSE_DEVICES_CAMERAS = "Devices.Cameras";
    public static final String LICENSE_DEVICES_MICROPHONES = "Devices.Microphones";

    public static final String LICENSE_FACE_DETECTION = "Biometrics.FaceDetection";
    public static final String LICENSE_FACE_EXTRACTION = "Biometrics.FaceExtraction";
    public static final String LICENSE_FACE_MATCHING = "Biometrics.FaceMatching";
    public static final String LICENSE_FACE_MATCHING_FAST = "Biometrics.FaceMatchingFast";
    public static final String LICENSE_FACE_SEGMENTATION = "Biometrics.FaceSegmentation";
    public static final String LICENSE_FACE_STANDARDS = "Biometrics.Standards.Faces";
    public static final String LICENSE_FACE_SEGMENTS_DETECTION = "Biometrics.FaceSegmentsDetection";

    public static final String LICENSE_FINGER_DETECTION = "Biometrics.FingerDetection";
    public static final String LICENSE_FINGER_EXTRACTION = "Biometrics.FingerExtraction";
    public static final String LICENSE_FINGER_MATCHING = "Biometrics.FingerMatching";
    public static final String LICENSE_FINGER_MATCHING_FAST = "Biometrics.FingerMatchingFast";
    public static final String LICENSE_FINGER_STANDARDS_FINGERS = "Biometrics.Standards.Fingers";
    public static final String LICENSE_FINGER_STANDARDS_FINGER_TEMPLATES = "Biometrics.Standards.FingerTemplates";
    public static final String LICENSE_FINGER_DEVICES_SCANNERS = "Devices.FingerScanners";
    public static final String LICENSE_FINGER_WSQ = "Images.WSQ";
    public static final String LICENSE_FINGER_NFIQ = "Biometrics.FingerQualityAssessmentBase";
    public static final String LICENSE_FINGER_CLASSIFICATION = "Biometrics.FingerSegmentsDetection";
    public static final String LICENSE_FINGER_SEGMENTATION = "Biometrics.FingerSegmentation";

    public static final String LICENSE_IRIS_DETECTION = "Biometrics.IrisDetection";
    public static final String LICENSE_IRIS_EXTRACTION = "Biometrics.IrisExtraction";
    public static final String LICENSE_IRIS_MATCHING = "Biometrics.IrisMatching";
    public static final String LICENSE_IRIS_MATCHING_FAST = "Biometrics.IrisMatchingFast";
    public static final String LICENSE_IRIS_STANDARDS = "Biometrics.Standards.Irises";
    public static final String LICENSE_IRIS_SEGMENTATION = "Biometrics.IrisSegmentation";

    public static final String LICENSE_VOICE_DETECTION = "Biometrics.VoiceDetection";
    public static final String LICENSE_VOICE_EXTRACTION = "Biometrics.VoiceExtraction";
    public static final String LICENSE_VOICE_MATCHING = "Biometrics.VoiceMatching";
    public static final String LICENSE_VOICE_MATCHING_FAST = "Biometrics.VoiceMatchingFast";
    public static final String LICENSE_VOICE_STANDARDS = "Biometrics.Standards.Voices";

    public static final String LICENSE_PALM_EXTRACTION = "Biometrics.PalmExtraction";
    public static final String LICENSE_PALM_MATCHING = "Biometrics.PalmMatching";
    public static final String LICENSE_PALM_MATCHING_FAST = "Biometrics.PalmMatchingFast";
    public static final String LICENSE_PALM_STANDARDS = "Biometrics.Standards.Palms";

    public static final String LICENSE_SENTISIGHT = "SentiSight";
    public static final String LICENSE_SENTIMASK = "SentiMask";
    public static final String LICENSE_SMARTCARDS = "SmartCards";

    public static final String LICENSING_PREFERENCES = "com.neurotec.samples.licensing.preference.LicensingPreferences";
    public static final String LICENSING_SERVICE = "com.neurotec.samples.licensing.LicensingService";

    public static final int REQUEST_CODE_LICENSING_PREFERENCES = 10;

    // ===========================================================
    // Public static method
    // ===========================================================

    public static synchronized NeurotecLicensingManager getInstance() {
        if (sInstance == null) {
            sInstance = new NeurotecLicensingManager();
        }
        return sInstance;
    }

    public static boolean isActivated(String license) {
        if (license == null) throw new NullPointerException("license");
        try {
            return NLicense.isComponentActivated(license);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            return false;
        }
    }

    public static boolean isFaceExtractionActivated() {
        return isActivated(LICENSE_FACE_EXTRACTION);
    }

    public static boolean isFaceMatchingActivated() {
        return isActivated(LICENSE_FACE_MATCHING) || isActivated(LICENSE_FACE_MATCHING_FAST);
    }

    public static boolean isFaceStandardsActivated() {
        return isActivated(LICENSE_FACE_STANDARDS);
    }

    public static boolean isFingerExtractionActivated() {
        return isActivated(LICENSE_FINGER_EXTRACTION);
    }

    public static boolean isFingerMatchingActivated() {
        return isActivated(LICENSE_FINGER_MATCHING) || isActivated(LICENSE_FINGER_MATCHING_FAST);
    }

    public static boolean isFingerStandardsActivated() {
        return isActivated(LICENSE_FINGER_STANDARDS_FINGERS) || isActivated(LICENSE_FINGER_STANDARDS_FINGER_TEMPLATES);
    }

    public static boolean isFingerWSQActivated() {
        return isActivated(LICENSE_FINGER_WSQ);
    }

    public static boolean isIrisExtractionActivated() {
        return isActivated(LICENSE_IRIS_EXTRACTION);
    }

    public static boolean isIrisMatchingActivated() {
        return isActivated(LICENSE_IRIS_MATCHING) || isActivated(LICENSE_IRIS_MATCHING_FAST);
    }

    public static boolean isIrisStandardsActivated() {
        return isActivated(LICENSE_IRIS_STANDARDS);
    }

    public static boolean isVoiceExtractionActivated() {
        return isActivated(LICENSE_VOICE_EXTRACTION);
    }

    public static boolean isVoiceMatchingActivated() {
        return isActivated(LICENSE_VOICE_MATCHING) || isActivated(LICENSE_IRIS_MATCHING_FAST);
    }

    public static boolean isVoiceStandardsActivated() {
        return isActivated(LICENSE_VOICE_STANDARDS);
    }

    public static boolean isPalmMatchingActivated() {
        return isActivated(LICENSE_PALM_MATCHING) || isActivated(LICENSE_PALM_MATCHING_FAST);
    }

    public static boolean isPalmStandardsActivated() {
        return isActivated(LICENSE_PALM_STANDARDS);
    }

    public static boolean isSentiSightActivated() {
        return isActivated(LICENSE_SENTISIGHT);
    }

    public static boolean isSentiMaskActivated() {
        return isActivated(LICENSE_SENTIMASK);
    }

    // ===========================================================
    // Private fields
    // ===========================================================

    private List<String> mComponents;

    // ===========================================================
    // Private constructor
    // ===========================================================

    private NeurotecLicensingManager() {
        mComponents = new ArrayList<String>();
    }

    // ===========================================================
    // Private methods
    // ===========================================================

    // ===========================================================
    // Public methods
    // ===========================================================

    public void reobtain(final Context context, final LicensingStateCallback callback) throws IOException {
        if (callback == null) throw new NullPointerException("callback");
        new AsyncTask<Boolean, Boolean, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                callback.onLicensingStateChanged(NeurotecLicensingState.OBTAINING);
            }
            @Override
            protected Boolean doInBackground(Boolean... params) {
                try {
                    return reobtain(context);
                } catch (Exception e) {
                    Log.e(TAG, "Exception", e);
                    return false;
                }
            }
            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                callback.onLicensingStateChanged(result ? NeurotecLicensingState.OBTAINED : NeurotecLicensingState.NOT_OBTAINED);
            }
        }.execute();
    }

    public boolean reobtain(Context context) throws IOException {
        List<String> reobtainedComponents = new ArrayList<String>(mComponents);
        return obtain(context, reobtainedComponents);
    }

    public void obtain(Context context, LicensingStateCallback callback, List<String> components) {
        if (context == null) throw new NullPointerException("context");
        obtain(callback, components, SERVER_ADDRESS, SERVER_PORT);
    }

    public void obtain(final LicensingStateCallback callback, final List<String> components, final String address, final int port) {
        if (callback == null) throw new NullPointerException("callback");
        new AsyncTask<Boolean, Boolean, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                callback.onLicensingStateChanged(NeurotecLicensingState.OBTAINING);
            }
            @Override
            protected Boolean doInBackground(Boolean... params) {
                try {
                    return obtain(components, address, port);
                } catch (Exception e) {
                    Log.e(TAG, "Exception", e);
                    return false;
                }
            }
            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                callback.onLicensingStateChanged(result ? NeurotecLicensingState.OBTAINED : NeurotecLicensingState.NOT_OBTAINED);
            }
        }.execute();
    }

    public boolean obtain(Context context, List<String> components) throws IOException {
        if (context == null) throw new NullPointerException("context");
        return obtain(components,SERVER_ADDRESS, SERVER_PORT);
    }

    public List<String> obtainLicenses(Context context, String[] licenses) throws IOException {
        if (context == null) throw new NullPointerException("context");
        return obtainLicenses(SERVER_ADDRESS, SERVER_PORT, licenses);
    }

    public List<String> obtainLicenses(String address, int port, String[] licenses) throws IOException {
        if (licenses == null) throw new NullPointerException("licenses");
        List<String> obtainedLicenses = new ArrayList<String>();

        Log.i(TAG, String.format("Obtaining licenses from server %s:%s", address, port));

        boolean result = false;
        for (String license : licenses) {
            boolean available = NLicense.obtain(address, port, license);
            if (available) {
                obtainedLicenses.add(license);
            }
            Log.i(TAG, String.format("Obtaining '%s' license %s.", license, available ? "succeeded" : "failed"));
        }
        return obtainedLicenses;
    }

    public boolean obtain(List<String> components, String address, int port) throws IOException {
        if (components == null) throw new NullPointerException("components");
        if (components.isEmpty()) throw new IllegalArgumentException("List of components is empty");

        Log.i(TAG, String.format("Obtaining licenses from server %s:%s", address, port));

        boolean result = true;
        mComponents.addAll(components);
        for (String component : components) {
            boolean available = false;
            available = NLicense.obtainComponents(address, port, component);
            result &= available;
            Log.i(TAG, String.format("Obtaining '%s' license %s.", component, available ? "succeeded" : "failed"));
        }
        return result;
    }

}


