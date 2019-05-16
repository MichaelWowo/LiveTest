package com.credenceid.sample.face;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.BiometricsManager;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

@SuppressWarnings({"unused", "StaticFieldLeak", "StatementWithEmptyBody"})
public class LaunchActivity
        extends Activity {

    /* When permissions are requested you must pass a number to reference result by. */
    private static final int REQUEST_ALL_PERMISSIONS = 5791;
    /* List of different permissions to request for. */
    private static final String[] PERMISSIONS = new String[]{CAMERA, WRITE_EXTERNAL_STORAGE};

    /* --------------------------------------------------------------------------------------------
     *
     * Android activity lifecycle event methods.
     *
     * --------------------------------------------------------------------------------------------
     */

    @Override
    protected void
    onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        this.requestPermissions();
    }

    /* Each time a permission is either allowed/denied this callback is invoked. */
    @Override
    public void
    onRequestPermissionsResult(int requestCode,
                               @NonNull String[] permissions,
                               @NonNull int[] grantResults) {

        if (REQUEST_ALL_PERMISSIONS != requestCode)
            return;

        /* Loop through each permission status. */
        for (int i = 0; i < permissions.length; ++i) {
            /* If permission was denied show popup notifying user that certain features may not work
             * and stop checking rest of permission statuses.
             */
            if (PERMISSION_GRANTED != grantResults[i]) {

                android.app.AlertDialog.Builder builder
                        = new android.app.AlertDialog.Builder(this)
                        .setMessage(getString(R.string.permission_denied))
                        .setPositiveButton(getString(R.string.ok),
                                (dialog, which) -> {
                                    /* Note: Androids built in method "requestPermissions()" can
                                     * ONLY request one permission at a time. Every time user
                                     * "Allows" a permission we then need to request for next one.
                                     */
                                    this.requestPermissions();
                                })
                        .setNegativeButton(getString(R.string.cancel), (dialog, which) -> finish());

                if (!this.isFinishing())
                    builder.create().show();

                return;
            }
        }

        /* Reach here is all permissions have been granted. */
        this.initBiometrics();
    }

    /* --------------------------------------------------------------------------------------------
     *
     * Private helpers.
     *
     * --------------------------------------------------------------------------------------------
     */

    /* Requests for first permission that is not granted. */
    private void
    requestPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : PERMISSIONS) {
                /* Requests for first permission not granted. When user grants permission, inside
                 * "Allow" callback we must then request for next non-granted permission.
                 *
                 * This is essentially a recursive function that goes through an array one-by-one.
                 */
                if (PERMISSION_GRANTED != checkSelfPermission(permission)) {
                    requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSIONS);
                    return;
                }
            }

            /* Reach here if all permissions have been granted. */
            this.initBiometrics();
        }
    }


    private void
    initBiometrics() {

        /*  Create new biometrics object. */
        App.BioManager = new BiometricsManager(this);

        /* Initialize object, meaning tell CredenceService to bind to this application. */
        App.BioManager.initializeBiometrics((Biometrics.ResultCode resultCode,
                                             String minimumVersion,
                                             String currentVersion) -> {

            if (OK == resultCode) {
                Toast.makeText(this, getString(R.string.biometrics_initialized), LENGTH_SHORT).show();

                App.DevFamily = App.BioManager.getDeviceFamily();
                App.DevType = App.BioManager.getDeviceType();

                /* Launch main activity. */
                Intent intent = new Intent(this, CameraActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                this.finish();

            } else if (INTERMEDIATE == resultCode) {
                /* This code is never returned for this API. */

            } else if (FAIL == resultCode) {
                Toast.makeText(this, getString(R.string.biometrics_fail_init), LENGTH_LONG).show();
            }
        });
    }
}