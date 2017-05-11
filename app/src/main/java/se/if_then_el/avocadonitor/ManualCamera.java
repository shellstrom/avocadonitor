package se.if_then_el.avocadonitor;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;

import java.util.Collections;

import static android.content.Context.CAMERA_SERVICE;

/**
 * Lots of bits and pieces taken from https://developer.android.com/things/training/doorbell/index.html
 *
 * The guts of this class is to manage the handlers, sessions and grabbing of pixeldata from the
 * camera module.
 */
public class ManualCamera {
    // Camera image parameters (device-specific)
    private static final int IMAGE_WIDTH  = 320; //Camera v2 spec supports 3280, but AT does not
    private static final int IMAGE_HEIGHT = 240; //Camera v2 spec supports 2464, but AT does not
    private static final int MAX_IMAGES   = 5;
    private String TAG = "Avocadonitor";
    // Image result processor
    private ImageReader mImageReader;
    // Active camera device connection
    private CameraDevice mCameraDevice;
    // Active camera capture session
    private CameraCaptureSession mCaptureSession;
    // Callback handling capture progress events
    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                                               CaptureRequest request,
                                               TotalCaptureResult result) {
                    if (session != null) {
                        session.close();
                        mCaptureSession = null;
                        Log.d(TAG, "CaptureSession closed");
                    }
                }
            };
    // Callback handling session state changes
    private final CameraCaptureSession.StateCallback mSessionCallback =
            new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    // When the session is ready, we start capture.
                    mCaptureSession = cameraCaptureSession;
                    triggerImageCapture();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Log.w(TAG, "Failed to configure camera");
                }
            };
    private ManualCameraListener mManualCameraListener;
    // Callback handling devices state changes
    private final CameraDevice.StateCallback mStateCallback =
            new CameraDevice.StateCallback() {

                @Override
                public void onOpened(CameraDevice cameraDevice) {
                    mCameraDevice = cameraDevice;
                    // Report back to listeners that the camera is ready for taking pictures
                    mManualCameraListener.cameraInitialized();
                }

                @Override
                public void onDisconnected(CameraDevice cameraDevice) {
                    Log.d(TAG, "Camera disconnected, closing.");
                    cameraDevice.close();
                }

                @Override
                public void onError(CameraDevice cameraDevice, int i) {
                    Log.d(TAG, "Camera device error, closing.");
                    cameraDevice.close();
                }

                @Override
                public void onClosed(CameraDevice cameraDevice) {
                    Log.d(TAG, "Closed camera, releasing");
                    mCameraDevice = null;
                }

            };

    public static ManualCamera getInstance() {
        return InstanceHolder.mCamera;
    }

    // Initialize a new camera device connection
    public void initializeCamera(Context context,
                                 Handler backgroundHandler,
                                 ImageReader.OnImageAvailableListener imageListener) {

        // Discover the camera instance
        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        String[] camIds = {};
        try {
            camIds = manager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.d(TAG, "Cam access exception getting IDs", e);
        }
        if (camIds.length < 1) {
            Log.d(TAG, "No cameras found");
            return;
        }
        String id = camIds[0];

        // Initialize image processor
        mImageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT,
                ImageFormat.JPEG, MAX_IMAGES);
        mImageReader.setOnImageAvailableListener(imageListener, backgroundHandler);

        // Open the camera resource
        try {
            manager.openCamera(id, mStateCallback, backgroundHandler);
        } catch (CameraAccessException cae) {
            Log.d(TAG, "Camera access exception", cae);
        } catch (SecurityException se) {
            se.printStackTrace();
        }
    }

    // Close the camera resources
    public void shutDown() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
    }

    public void takePicture() {
        if (mCameraDevice == null) {
            Log.w(TAG, "Cannot capture image. Camera not initialized.");
            return;
        }

        // Here, we create a CameraCaptureSession for capturing still images.
        try {
            mCameraDevice.createCaptureSession(
                    Collections.singletonList(mImageReader.getSurface()),
                    mSessionCallback,
                    null);
        } catch (CameraAccessException cae) {
            Log.d(TAG, "access exception while preparing pic", cae);
        }
    }

    private void triggerImageCapture() {
        try {
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

            mCaptureSession.capture(captureBuilder.build(), mCaptureCallback, null);
        } catch (CameraAccessException cae) {
            Log.d(TAG, "camera capture exception");
        }
    }

    public void addListener(ManualCameraListener toAdd) {
        mManualCameraListener = toAdd;
    }

    private static class InstanceHolder {
        private static ManualCamera mCamera = new ManualCamera();
    }
}
