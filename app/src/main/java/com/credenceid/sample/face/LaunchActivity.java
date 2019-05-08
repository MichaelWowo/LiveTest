package com.credenceid.sample.face;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.BiometricsManager;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

@SuppressWarnings({"unused", "StaticFieldLeak", "StatementWithEmptyBody"})
public class LaunchActivity
        extends Activity {

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
        this.initBiometrics();
    }

    /* --------------------------------------------------------------------------------------------
     *
     * Private helpers.
     *
     * --------------------------------------------------------------------------------------------
     */

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
                Intent intent = new Intent(this, FaceActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                this.finish();

            } else if (INTERMEDIATE == resultCode) {
                /* This code is never returned here. */

            } else if (FAIL == resultCode) {
                Toast.makeText(this, getString(R.string.biometrics_fail_init), LENGTH_LONG).show();
            }
        });
    }
}