package fbspiele.erstermai2017;

import android.Manifest;
import android.content.Context;
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
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import java.util.Arrays;


//https://inducesmile.com/android/android-camera2-api-example-tutorial/

public class MainActivity extends AppCompatActivity {

    TextureView textureView;
    Button button;
    String cameraId;
    Size imageDimension;
    private Handler handler;
    CameraDevice cameraDevice;
    protected CaptureRequest.Builder captureRequestBuilder;
    protected CameraCaptureSession cameraCaptureSession;
    private HandlerThread handlerThread;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = (TextureView) findViewById(R.id.textureView);
        button = (Button) findViewById(R.id.button);
        textureView.setSurfaceTextureListener(textureListener);

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

    public void openCamera(){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[1];
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
            e.printStackTrace();
        }
    }

    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    public void createCameraPreview(){
        try{
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(imageDimension.getWidth(),imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                public void onConfigured(CameraCaptureSession cameraCaptureSession2){
                    if (cameraDevice == null){
                        return;
                    }
                    cameraCaptureSession = cameraCaptureSession2;
                    updatePreview();
                }
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession2){
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
        Log.v("stop h t", "vor try");
        try{
            handlerThread.join();
            Log.v("stop h t", "zwischen join und thread null");
            handlerThread = null;
            Log.v("stop h t", "zwischen thread null und handler null");
            handler = null;
        }
        catch (InterruptedException e) {
            Log.v("stop h t", "interexception e");
            e.printStackTrace();
        }
        Log.v("stop h t","durch");
    }

    private void closecamera(){
        if (cameraDevice != null){
            cameraDevice.close();
            cameraDevice = null;
        }
    }
}
