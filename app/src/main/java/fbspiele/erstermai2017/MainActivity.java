package fbspiele.erstermai2017;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.util.SparseIntArray;

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
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

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
                saveletztecam();
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
    void saveletztecam(){
        SharedPreferences settings = getSharedPreferences("settingsfile", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("letztecamera", letztegeoffnetecamera);
        editor.apply();
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
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            int width = 640;
            int height = 480;
            if (jpegSizes != null && jpegSizes.length > 0){
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader imageReader = ImageReader.newInstance(width,height,ImageFormat.JPEG,1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(imageReader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            Log.v("saving","capturerequestbuilder initialisen");
            final CaptureRequest.Builder CaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE,CameraMetadata.CONTROL_MODE_AUTO);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},123); //200 = camera permission code
                return;
            }
            File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+File.separator+"erstermai2017");
            if(!folder.exists()){
                if(folder.mkdir()){
                    Log.v("directory",folder.toString()+" erstellt");
                    Toast.makeText(getApplicationContext(),folder.toString()+" erstellt",Toast.LENGTH_LONG).show();
                }
                else {
                    Log.v("directory","erstermai2017 ordner konnte nicht erstellt werden");
                    Toast.makeText(getApplicationContext(),"erstermai2017 ordner konnte nicht erstellt werden :(",Toast.LENGTH_LONG).show();
                }
            }
            final File file = new File(folder+File.separator+"pic.jpg");
            Log.v("saving","readerlistener initialisen");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        Log.v("saving","bytes new byte");
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        Log.v("saving","bevor save(bytes)");
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    Log.v("saving","private void save");
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                        Log.v("saving","saved file");
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            Log.v("saving","imageavailablelistener setten");
            imageReader.setOnImageAvailableListener(readerListener,handler);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(getApplicationContext(), "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureRequestBuilder.build(), captureListener, handler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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
