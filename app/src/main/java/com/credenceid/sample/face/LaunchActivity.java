package com.credenceid.sample.face;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.BiometricsManager;
import com.credenceid.biometrics.DeviceFamily;
import com.credenceid.biometrics.DeviceType;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

@SuppressLint("StaticFieldLeak")
@SuppressWarnings({"unused"})
public class LaunchActivity
		extends Activity {

	private static final String TAG = LaunchActivity.class.getSimpleName();

	/* When requested for permissions you must specify a number which sort of links the permissions
	 * request to a "key". This way when you get back a permissions event you can tell from where
	 * that permission was requested from.
	 */
	private static final int REQUEST_ALL_PERMISSIONS = 0;
	/* List of all permissions we will request. */
	private static final String[] PERMISSIONS = new String[]{
			WRITE_EXTERNAL_STORAGE,
			READ_EXTERNAL_STORAGE,
			CAMERA
	};

	/* CredenceSDK biometrics object, used to interface with APIs. */
	private static BiometricsManager mBiometricsManager;
	/* Stores which Credence family of device's this app is running on. */
	private static DeviceFamily mDeviceFamily = DeviceFamily.InvalidDevice;
	/* Stores which specific device this app is running on. */
	private static DeviceType mDeviceType = DeviceType.InvalidDevice;

	/* --------------------------------------------------------------------------------------------
	 *
	 * Public getter methods.
	 *
	 * --------------------------------------------------------------------------------------------
	 */

	public static BiometricsManager
	getBiometricsManager() {

		return mBiometricsManager;
	}

	public static DeviceType
	getDeviceType() {

		return mDeviceType;
	}

	public static DeviceFamily
	getDeviceFamily() {

		return mDeviceFamily;
	}

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
	@SuppressWarnings("deprecation")
	@Override
	public void
	onRequestPermissionsResult(int requestCode,
							   @NonNull String permissions[],
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
						.setMessage(getString(R.string.permission_denied_text))
						.setPositiveButton(getString(R.string.dialog_retry),
								(dialog, which) -> {
									/* Note: Androids built in method "requestPermissions()" can
									 * ONLY request one permission at a time. Every time user
									 * "Allows" a permission we then need to request for next one.
									 */
									this.requestPermissions();
								})
						.setNegativeButton(getString(R.string.dialog_sure),
								(dialog, which) -> {
									/* If user denies any permission application will close. */
									this.onBackPressed();
								});

				if (!this.isFinishing())
					builder.create().show();

				return;
			}
		}

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
		mBiometricsManager = new BiometricsManager(this);

		/* Initialize object, meaning tell CredenceService to bind to this application. */
		mBiometricsManager.initializeBiometrics((Biometrics.ResultCode resultCode,
												 String minimumVersion,
												 String currentVersion) -> {

			/* This API will never return ResultCode.INTERMEDIATE. */

			if (OK == resultCode) {
				Toast.makeText(this, "Biometrics initialized.", LENGTH_SHORT).show();

				mDeviceFamily = mBiometricsManager.getDeviceFamily();
				mDeviceType = mBiometricsManager.getDeviceType();

				/* Launch main activity. */
				Intent intent = new Intent(this, FaceActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(intent);
				this.finish();
			} else if (FAIL == resultCode)
				Toast.makeText(this, "Biometrics FAILED to initialize.", LENGTH_LONG).show();
		});
	}

	/* Checks if permissions stated in manifest have been granted, if not it then requests them. */
	private void
	requestPermissions() {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED
					|| checkSelfPermission(READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED
					|| checkSelfPermission(CAMERA) != PERMISSION_GRANTED) {

				requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSIONS);
			} else
				this.initBiometrics();
		} else
			this.initBiometrics();
	}
}
