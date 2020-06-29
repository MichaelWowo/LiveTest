    package com.credenceid.sample.face

import android.Manifest.permission.*
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT

/**
 * When permissions are requested you must pass a number to reference result by.
 */
private const val REQUEST_ALL_PERMISSIONS = 5791
/**
 * List of different permissions to request for.
 */
private val PERMISSIONS = arrayOf(CAMERA,
        WRITE_EXTERNAL_STORAGE,
        READ_EXTERNAL_STORAGE,
        READ_PHONE_STATE)

class LaunchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        this.requestPermissions()
    }

    /**
     * Each time a permission is either allowed/denied this callback is invoked.
     */
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {

        if (REQUEST_ALL_PERMISSIONS != requestCode)
            return

        /* Loop through each permission status. */
        for (i in permissions.indices) {
            /* If permission was denied show popup notifying user that certain features may not work
             * and stop checking rest of permission statuses.
             */
            if (PERMISSION_GRANTED != grantResults[i]) {

                val builder = android.app.AlertDialog.Builder(this)
                        .setMessage(getString(R.string.permission_denied))
                        .setPositiveButton(getString(R.string.ok)) { _, _ ->
                            this.requestPermissions()
                            /* Note: Androids built in method "requestPermissions()" can
                             * ONLY request one permission at a time. Every time user
                             * "Allows" a permission we then need to request for next one.
                             */
                        }
                        .setNegativeButton(getString(R.string.cancel)) { _, _ -> finish() }

                if (!this.isFinishing)
                    builder.create().show()
                return
            }
        }

        /* Reach here is all permissions have been granted. */
        this.initBiometrics()
    }

    /**
     * Requests for first permission that is not granted.
     */
    private fun requestPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in PERMISSIONS) {
                /* Requests for first permission not granted. When user grants permission, inside
                 * "Allow" callback we must then request for next non-granted permission.
                 *
                 * This is essentially a recursive function that goes through an array one-by-one.
                 */
                if (PERMISSION_GRANTED != checkSelfPermission(permission)) {
                    requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSIONS)
                    return
                }
            }

            /* Reach here if all permissions have been granted. */
            this.initBiometrics()
        }
    }


    private fun initBiometrics() {

        /* Launch main activity. */
        val intent = Intent(this, CameraActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        this.finish()


    }
}