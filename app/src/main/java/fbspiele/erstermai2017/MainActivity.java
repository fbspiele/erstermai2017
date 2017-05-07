package fbspiele.erstermai2017;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.Arrays;

/*
*
*
        SharedPreferences settings = getSharedPreferences("settingsfile", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("datenpunkte", datenpunkte);
        editor.apply();
* */
//https://inducesmile.com/android/android-camera2-api-example-tutorial/

public class MainActivity extends AppCompatActivity {

    TextureView textureView;
    ImageButton ibtakepic;
    ImageButton ibchangecam;
    String cameraId;
    Size imageDimension;
    private Handler handler;
    CameraDevice cameraDevice;
    protected CaptureRequest.Builder captureRequestBuilder;
    protected CameraCaptureSession cameraCaptureSession;
    private HandlerThread handlerThread;
    int letztegeoffnetecamera=0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = (TextureView) findViewById(R.id.textureView);
        ibtakepic = (ImageButton) findViewById(R.id.takepic);
        ibchangecam = (ImageButton) findViewById(R.id.changecam);

        textureView.setSurfaceTextureListener(textureListener);

        SharedPreferences settings = getSharedPreferences("settingsfile", 0);
        letztegeoffnetecamera = settings.getInt("letztecamera", 0);
        Log.v("letztecam",letztegeoffnetecamera + " (0 = back-, 1 = frontfacing)");

        ibchangecam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (letztegeoffnetecamera){
                    case 0:
                        letztegeoffnetecamera=1;
                        ibchangecam.setImageDrawable(getDrawable(R.drawable.ic_camera_rear_white_48px));
                        Log.v("changecam","von back auf front");
                        break;
                    case 1:
                        letztegeoffnetecamera=0;
                        ibchangecam.setImageDrawable(getDrawable(R.drawable.ic_camera_front_white_48px));
                        Log.v("changecam","von front auf back");
                        break;
                    default:    //just in case
                        letztegeoffnetecamera=0;
                        ibchangecam.setImageDrawable(getDrawable(R.drawable.ic_camera_front_white_48px));
                        Log.v("changecam","default von front auf back");
                        break;
                }
                closecamera();
                openCamera();
            }
        });

        ibtakepic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takepic();
            }
        });
    }
    protected void onResume(){
        super.onResume();
        starthandlerthread();
        if(textureView.isAvailable()){
            openCamera();
        }
        else{
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    protected void onPause(){
        closecamera();
        stophandlerthread();
        super.onPause();
    }


    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    public void takepic(){
        if(cameraDevice == null){
            Log.e("takepic", "cameraDevice is null");
            return;
        }
        //CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.v("pic machen","gehtnochned\n!!!\n!!!\n!!!\n!!!\n!!!\n!!!\n!!!\n!!!\n!!!\n!!!");
    }

    public void openCamera(){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[letztegeoffnetecamera];
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert streamConfigurationMap != null;
            imageDimension = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},200); //200 = camera permission code
                return;
            }
            cameraManager.openCamera(cameraId, stateCallback,handler);
        }
        catch (CameraAccessException e){
            Toast.makeText(getApplicationContext(),"konnte die camera nicht öffnen",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    //testtemp
    public void createCameraPreview(){
        try{
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(imageDimension.getWidth(),imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession2){
                    if (cameraDevice == null){
                        return;
                    }
                    cameraCaptureSession = cameraCaptureSession2;
                    updatePreview();
                }
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession2){
                    Log.v("createcamprev","configuration changed");
                }
            },null);

        }
        catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    public void updatePreview(){
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            if(handler!=null){
                cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(),null,handler);
            }
        }
        catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    protected void starthandlerthread(){
        handlerThread = new HandlerThread("camera background");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    protected void stophandlerthread(){
        handlerThread.quitSafely();
        try{
            handlerThread.join();
            handlerThread = null;
            handler = null;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            Log.v("stophandlerthread","konnte handlerthread nicht stoppen");
        }
    }

    private void closecamera(){
        if (cameraDevice != null){
            cameraDevice.close();
            cameraDevice = null;
        }
    }
}
