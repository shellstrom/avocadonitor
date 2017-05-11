package se.if_then_el.avocadonitor;

/**
 * Create an interface to pass messages from the class working with the camera
 * to report to the class dealing with interaction and saving of pictures, that it's finished
 */
interface ManualCameraListener {
    void cameraInitialized();
}
