package se.if_then_el.avocadonitor;

import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Lots of bits and pieces taken from https://developer.android.com/things/training/doorbell/index.html
 *
 * The main purpose of this class is to fire up a number of routines that warm up the camera
 * module to allow for pictures to be snapped at an interval, as well as saving the snapped pictures
 * to different forms of storage.
 *
 * This code runs on an Android Things RPi3 setup that posts to this Twitter: @advocado_tree
 */
public class MainActivity extends AppCompatActivity implements ManualCameraListener {

    /**
     * A Handler for running tasks in the background.
     */
    private Handler mBackgroundHandler;
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;
    /**
     *  Camera capture device wrapper
     */
    private ManualCamera mCamera;
    /**
     * Listener for new camera images.
     */
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
        new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                // Get the raw image bytes
                Image image = reader.acquireLatestImage();
                ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                final byte[] imageBytes = new byte[imageBuf.remaining()];
                imageBuf.get(imageBytes);
                image.close();

                // Start the routines for storing of caught picture
                onPictureTaken(imageBytes);
            }
        };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mBackgroundThread.quitSafely();
    }

    /**
     * Starts a background thread and its Handler.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("InputThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Save picture locally, on a samba drive and conditionally send to Twitter
     */
    private void onPictureTaken(final byte[] imageBytes) {
        FileOutputStream outStream = null;
        File file = new File(getExternalFilesDir(null), "avocado-pic.jpg");
        try {
            // Write to SD Card
            outStream = new FileOutputStream(file);
            outStream.write(imageBytes);
            outStream.close();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmm");
            String filename = sdf.format(System.currentTimeMillis())+".jpg";
            saveToSambaServer(imageBytes, filename);

            DateTime dt = new DateTime();
            int hours = dt.getHourOfDay();

            // Tweet at fixed time every day
            if(hours == 10) {
                tweetIt();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveToSambaServer(byte[] imageBytes, String filename) {
        String url = "smb://[YOUR SAMBA SERVER FOLDER HERE!]/" +filename;

        SmbFile file = null;
        try {
            NtlmPasswordAuthentication auth = NtlmPasswordAuthentication.ANONYMOUS;
            file = new SmbFile(url, auth);

            SmbFileOutputStream out = new SmbFileOutputStream(file);
            out.write(imageBytes);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Share our picture with the world!
     */
    private void tweetIt() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        // http://apps.twitter.com details
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("[YOUR TWITTER CONSUMER KEY HERE!]")
                .setOAuthConsumerSecret("[YOUR TWITTER CONSUMER SECRET HERE!]")
                .setOAuthAccessToken("[YOUR TWITTER ACCESS TOKEN HERE!]")
                .setOAuthAccessTokenSecret("[YOUR TWITTER ACCESS TOKEN SECRET HERE!]");
        TwitterFactory tf = new TwitterFactory(cb.build());
        Twitter twitter = tf.getInstance();

        StatusUpdate status = null;
        try {
            // Re-use the image in local storage as status image
            String filename = "avocado-pic";
            File file = new File(getExternalFilesDir(null), filename);

            // Grab a random phrase from a string array, and use as status message
            String[] stringArray =   getResources().getStringArray(R.array.tree_sayings);
            Random r = new Random();
            int randomInt = r.nextInt(stringArray.length + 1);
            status = new StatusUpdate(stringArray[randomInt]);
            status.setMedia(file);
            twitter.updateStatus(status);
        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cameraInitialized() {
        mCamera.takePicture();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startBackgroundThread();
        mCamera = ManualCamera.getInstance();
        mCamera.addListener(this);
        Timer timer = new Timer();
        // Set up a picture grabber with a one hour interval
        timer.scheduleAtFixedRate(new photoTask(), 0, 1000 * 60 * 60);
    }

    /**
     * A task for warming up the camera and start the grabbing of some pixelated frames.
     * After pixels have been framed, a callback will be made to allow for additional proceduring.
     */
    private class photoTask extends TimerTask {
        @Override
        public void run() {
            mCamera.initializeCamera(MainActivity.this, mBackgroundHandler, mOnImageAvailableListener);
        }
    }
}
