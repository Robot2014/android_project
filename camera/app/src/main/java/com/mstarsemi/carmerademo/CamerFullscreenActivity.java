package com.mstarsemi.mynetworkplayerapplication;


import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.MemoryFile;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.ArrayMap;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CamerFullscreenActivity extends Activity implements View.OnClickListener {

    private final String TAG ="CamerFullscreenActivity";
    private Button mPreviewButton = null;
    private Button mStartDecoderButton = null;
    private SurfaceView mPreviewSurfaceView =null;
    private SurfaceHolder mPreviewsurfaceHolder = null;

    private AdapterView.OnItemSelectedListener mAdapterViewListener = null;
    private Spinner mResolution_spinner = null;
    private Spinner mBitratelist_spinner = null;
    private Spinner mPreviewFomatList_spinner = null;

    private Camera mCamera = null;
    private MediaCodec mEncoderCodec = null;
    private MyEncoderThread mEncoderCodecHandler = null;

    private SurfaceView mDecoderSurfaceView = null;
    private SurfaceHolder mDecoderSurfaceHolder = null;
    private MediaCodec mDecoderCodec = null;
    private MyDecoderThread mDecoderCodecHandler = null;
    private boolean mEncoderStarted = false;
    private Camera.PreviewCallback mPreviewCallback = null;

    private String mEncoderType = "video/avc";

    private int mCountfps = 0;
    private int mDecoderCountfps = 0;
    private Handler mCountfpsHandler = null;
    private Thread mCountfpsThread = null;
    private TextView mPreviewFPSTextView = null;
    private TextView mEncoderDecoderTextView = null;
    private int mBufferSize = 0;

    private int mPreviewParamterVideoWidth = 0; //default value
    private int mPreviewParamterViewoHeight = 0; //default value
    private int mPreviewImageFormat = ImageFormat.YUY2; //YV12  == 420P   NV21= 420sp
    private int mcoderType = MediaCodecInfo.CodecCapabilities.COLOR_FormatYCbYCr;

    private final long mWaitForFrame = 1000*500;

    List<Camera.Size> msuppertedPreviewSize = null;
    ArrayAdapter<String> mResolutionList = null;

    List<Integer> mPreviewFomatType = null;
    ArrayAdapter<String> mPreviewFomatTypeList = null;
    Map<Integer,Integer> mPreviewFomatTypeMaps = null;

    int[] mBitRateList = {1024*1024*8,2*1024*1024*8};
    ArrayAdapter<String> mBitRateDisplayList = null;
    int mEncoderBitrate = 1024*1024*8;

    MediaCodec.BufferInfo mbufferInfo = null;

    private int CamerStations = 0; //0 is not preview , 1 is preview, 2 is encodered and decoder

    private static int mNumOfPreivewBuffer = 1;
    byte[] mCallbackbytes;
	private static int mMetadataSize = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camer_fullscreen);

        mPreviewButton = (Button)findViewById(R.id.preview_button);
        mPreviewButton.setOnClickListener(this);
        mStartDecoderButton = (Button)findViewById(R.id.start_test_button);
        mStartDecoderButton.setOnClickListener(this);
        mPreviewFPSTextView = (TextView)findViewById(R.id.preview_fps);
        mEncoderDecoderTextView = (TextView)findViewById(R.id.encoder_fps);

        mPreviewSurfaceView = (SurfaceView)findViewById(R.id.preivew_surfaceView);
        mPreviewsurfaceHolder = mPreviewSurfaceView.getHolder();
        mDecoderSurfaceView = (SurfaceView)findViewById(R.id.decoder_surfaceView);
        mDecoderSurfaceHolder = mDecoderSurfaceView.getHolder();

        mAdapterViewListener = new MyAdapterViewOnItemSelectedListener();

        mResolution_spinner = (Spinner)findViewById(R.id.fenbianlv_spinner);
        mBitratelist_spinner = (Spinner)findViewById(R.id.bitratelist_spinner);
        mPreviewFomatList_spinner = (Spinner)findViewById(R.id.previewformat_type_spinner);

        mResolution_spinner.setOnItemSelectedListener(mAdapterViewListener);
        mBitratelist_spinner.setOnItemSelectedListener(mAdapterViewListener);
        mPreviewFomatList_spinner.setOnItemSelectedListener(mAdapterViewListener);

        OpenCamer();
        GetCameraInfoParameters();
        mPreviewParamterViewoHeight =msuppertedPreviewSize.get(0).height;
        mPreviewParamterVideoWidth =msuppertedPreviewSize.get(0).width;

        mCountfpsHandler = new MyCountHandler();
        mCountfpsThread = new MyCountFPSThread();
        mCountfpsThread.start();
        mbufferInfo =  new MediaCodec.BufferInfo();

        CamerStations = 0;
    }

    @Override
    protected void onStop(){
        if((mCountfpsHandler != null) && (mCountfpsThread != null))
        {
            mCountfpsHandler.removeCallbacks(mCountfpsThread);
            mCountfpsHandler = null;
            mCountfpsThread = null;
        }
        StartEncoderCodec(false);
        StartDecoderCodec(false);
        StopPreview();
        super.onStop();
    }

    @Override
    public void onClick(View view){
            switch (view.getId()){
                case R.id.preview_button:{
                    if(Camera.getNumberOfCameras() > 0) {
                        if(CamerStations > 1){
                            StopPreview();
                            StartEncoderCodec(false);
                            StartDecoderCodec(false);
                        }
                        StopPreview();
                        OpenCamer();
                        StartPreview();
                    }else{
                        Toast.makeText(this,"please insert usb camera",Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                case R.id.start_test_button:{
                    //StopPreview();
                    //OpenCamer();
                    //StartPreview();
                    StartDecoderCodec(true);
                    StartEncoderCodec(true);
                }
                default:
                    break;
            }
    }

    private void setprivewParameters(){
        Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewParamterVideoWidth, mPreviewParamterViewoHeight);
        parameters.setPreviewFormat(mPreviewImageFormat);
        mCamera.setParameters(parameters);

        mBufferSize = mPreviewParamterVideoWidth*mPreviewParamterViewoHeight*3/2;
        /*for(int i = 0; i < mNumOfPreivewBuffer; i++){
            byte[] callbackbytes = new byte[mBufferSize];
            mCamera.addCallbackBuffer(callbackbytes);
        }*/
    }

    private void OpenCamer(){
        if(mCamera == null) {
            int total = Camera.getNumberOfCameras();
            for(int index = 0; index < total; index++){
                mCamera = Camera.open(index);
                if(mCamera != null){
                    break;
                }
            }
        }
    }

    private void GetCameraInfoParameters(){
        if(mCamera != null){
            Parameters parameters = mCamera.getParameters();
            msuppertedPreviewSize = parameters.getSupportedPreviewSizes();
            if(mResolutionList == null){
                mResolutionList = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
            }else{
                mResolutionList.clear();
            }
            for(int i =0; i< msuppertedPreviewSize.size(); i++){
                mResolutionList.add(String.valueOf(msuppertedPreviewSize.get(i).width)+"*"+String.valueOf(msuppertedPreviewSize.get(i).height)+"resolution");
            }
            mResolutionList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mResolution_spinner.setAdapter(mResolutionList);
            mPreviewParamterViewoHeight =msuppertedPreviewSize.get(0).height;
            mPreviewParamterVideoWidth =msuppertedPreviewSize.get(0).width;


            if(mPreviewFomatTypeList == null){
                mPreviewFomatTypeList = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
            }else{
                mPreviewFomatTypeList.clear();
            }
            if(mPreviewFomatTypeMaps == null){
                mPreviewFomatTypeMaps = new ArrayMap<Integer, Integer>();
            }else{
                mPreviewFomatTypeMaps.clear();
            }
            mPreviewFomatType = parameters.getSupportedPreviewFormats();
            String type = null;
            int index = 0;
            for(int i = 0;i < mPreviewFomatType.size();i++){
                switch (mPreviewFomatType.get(i)){
                    case ImageFormat.YV12:
                        type="YUV420P";
                        mPreviewFomatTypeList.add(type);
                        mPreviewFomatTypeMaps.put(index, mPreviewFomatType.get(i));
                        index++;
                        break;
                    case ImageFormat.NV21:
                        type="YUV420SP";
                        mPreviewFomatTypeList.add(type);
                        mPreviewFomatTypeMaps.put(index, mPreviewFomatType.get(i));
                        index++;
                        break;
                    case ImageFormat.YUY2:
                        type="YUY2";
                        mPreviewFomatTypeList.add(type);
                        mPreviewFomatTypeMaps.put(index, mPreviewFomatType.get(i));
                        index++;
                        break;
                    default:
                        break;
                }
            }
            mPreviewFomatTypeList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mPreviewFomatList_spinner.setAdapter(mPreviewFomatTypeList);
            if(type == null){
                mPreviewFomatList_spinner.setVisibility(View.GONE);
            }else{
                mPreviewImageFormat =  mPreviewFomatTypeMaps.get(0);
                mPreviewImageFormat = ImageFormat.YUY2;
                switch (mPreviewImageFormat){
                    case ImageFormat.YV12:
                        mcoderType = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
                        break;
                    case ImageFormat.NV21:
                        mcoderType = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
                        break;
                    case ImageFormat.YUY2:
                        //mcoderType = MediaCodecInfo.CodecCapabilities.COLOR_FormatYCbYCr;
                        mcoderType = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
                        break;
                    default:
                        break;
                }
                mPreviewFomatList_spinner.setVisibility(View.VISIBLE);
            }

            // ����encoder ��bitrate
            if(mBitRateDisplayList == null){
                mBitRateDisplayList = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
            }else{
                mBitRateDisplayList.clear();
            }
            mBitRateDisplayList.add("1M bitrate");
            mBitRateDisplayList.add("2M bitrate");
            mBitRateDisplayList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mBitratelist_spinner.setAdapter(mBitRateDisplayList);
            mEncoderBitrate = mBitRateList[0];
        }
    }

    private void StartPreview(){
        if(mCamera != null){
            try {
                mCamera.setPreviewDisplay(mPreviewsurfaceHolder);
                setprivewParameters();
                mCamera.startPreview();
                CamerStations = 1;
            }catch (IOException e){
                e.printStackTrace();
            }
        }else{
            Toast.makeText(this,"Camera is NULL",Toast.LENGTH_SHORT).show();
        }
    }

    private   void StopPreview(){
        if(mCamera != null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            CamerStations = 0;
        }
    }

    private  void StartEncoderCodec(boolean bstart){
        if(mEncoderCodecHandler != null) {
            mEncoderStarted = false;
            mEncoderCodecHandler.stopEncoder();
            mEncoderCodecHandler = null;
        }
        if(bstart) {
            if(mPreviewCallback == null) {
                mPreviewCallback = new CamerPrviewCallback();
            }
            mCamera.setPreviewCallbackWithBuffer(mPreviewCallback);
		    mCallbackbytes = new byte[mMetadataSize];
		    mCamera.addCallbackBuffer(mCallbackbytes);

            mEncoderCodecHandler = new MyEncoderThread(mEncoderType);
            mEncoderCodecHandler.startEncoder();
            mEncoderCodecHandler.start();
            mEncoderStarted = true;
        }
    }

    private  void StartDecoderCodec(boolean bstart){
        if(mDecoderCodecHandler != null){
            mDecoderCodecHandler.stopDecoder();
            mDecoderCodecHandler = null;
        }
        if(bstart){
            mDecoderCodecHandler = new MyDecoderThread(mEncoderType);
            mDecoderCodecHandler.startDecoder();
            mDecoderCodecHandler.start();
        }
        mDecoderCountfps = 0;
        CamerStations = 2;
    }

    private  class MyEncoderThread extends Thread{
        private ByteBuffer[] mInputBuffers = null;
        private ByteBuffer[] mOutputBuffers = null;
        private MediaFormat mMediaFormat = null;
        private boolean mbStart = false;
        private byte[]  moutData = null;

        public  MyEncoderThread(String type){
            try {
                mEncoderCodec =  MediaCodec.createEncoderByType(type);
                if(mEncoderCodec != null){
                    mMediaFormat = MediaFormat.createVideoFormat(type,mPreviewParamterVideoWidth,mPreviewParamterViewoHeight);
                    mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,mEncoderBitrate);//A key describing the bitrate in bits/sec.
                    mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mcoderType);//A key describing the color format of the content in a video format.
                    mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);//A key describing the frame rate of a video format in frames/sec.
                    mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);//A key describing the frequency of I frames expressed in secs between I frames
                    mEncoderCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                } else{
                    Log.d(TAG,"can't create MediaCodec by type:"+type);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        public void startEncoder(){
            if(mEncoderCodec != null) {
                //get input buffers and out put buffers
                mEncoderCodec.start();
                mInputBuffers = mEncoderCodec.getInputBuffers();
                mOutputBuffers = mEncoderCodec.getOutputBuffers();
                moutData = new byte[mBufferSize];
                mbStart = true;
            }
        }
        public void stopEncoder(){
            if(mEncoderCodec != null) {
                mbStart = false;
                try {
                    mEncoderCodecHandler.join();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                mEncoderCodec.stop();
                mEncoderCodec.release();
                mEncoderCodec = null;
            }
        }

        public void EncoderQueueIputBuffer(byte[] bytes){
            int index;
            index = mEncoderCodec.dequeueInputBuffer(0);
            if (index >= 0) {
                mInputBuffers[index].clear();
                mInputBuffers[index].put(bytes);
                mEncoderCodec.queueInputBuffer(index, 0, mMetadataSize, -1, MediaCodec.BUFFER_FLAG_SYNC_FRAME);
            }
        }
        @Override
        public void run() {
            int index = -1;
            while (mbStart){
                if(mEncoderCodec != null) {
                    index = mEncoderCodec.dequeueOutputBuffer(mbufferInfo, mWaitForFrame);
                    if (index >= 0) {
                        mOutputBuffers[index].get(moutData);
                        if (mDecoderCodecHandler != null) {
                             mDecoderCodecHandler.queueFrame(moutData);
                        }
                        mOutputBuffers[index].clear();
                        mEncoderCodec.releaseOutputBuffer(index, false);

                        mCamera.addCallbackBuffer(mCallbackbytes);

                      /*  if((mDecoderCodecHandler != null) && bDecoder){
                             mDecoderCodecHandler.DisplayFrame();
                        }*/
                    } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        mOutputBuffers = mEncoderCodec.getOutputBuffers();
                    }
                }
            }
        }
    }

    private class MyDecoderThread extends Thread{
        private MediaFormat mMediaFormat = null;
        private ByteBuffer[] mInputBuffers;
        private boolean mbCodecConfig = false;
        private long mPTS = 0;
        MediaCodec.BufferInfo mbufferInfo = null;
        private boolean mDecoderStarted = false;

        public MyDecoderThread(String type) {
            try {
                if (mDecoderCodec == null) {
                    mDecoderCodec = MediaCodec.createDecoderByType(type);
                    mMediaFormat = MediaFormat.createVideoFormat(type, mPreviewParamterVideoWidth, mPreviewParamterViewoHeight);
                    mMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
                    mDecoderCodec.configure(mMediaFormat, mDecoderSurfaceHolder.getSurface(), null, 2);
                    mbCodecConfig = false;
                    mPTS = 0;
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        public void startDecoder(){
            if(mDecoderCodec != null){
                mbufferInfo = new MediaCodec.BufferInfo();
                mDecoderCodec.start();
                mInputBuffers = mDecoderCodec.getInputBuffers();
                mDecoderStarted = true;
            }
        }

        public void stopDecoder(){
            if(mDecoderCodec != null){
                mDecoderStarted = false;
               try{
                    mDecoderCodecHandler.join();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                mDecoderCodec.stop();
                mDecoderCodec.release();
                mDecoderCodec = null;
            }
        }

        public void queueFrame(byte[] bytes){
            int index = mDecoderCodec.dequeueInputBuffer(-1);
            if (index >= 0) {
                mInputBuffers[index].clear();
                mInputBuffers[index].put(bytes, 0, mBufferSize);
                if(mbCodecConfig){
                    mDecoderCodec.queueInputBuffer(index,0,mBufferSize,mPTS, MediaCodec.BUFFER_FLAG_SYNC_FRAME);
                    mPTS++;
                }else{
                    mDecoderCodec.queueInputBuffer(index,0,mBufferSize,0,MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                    mbCodecConfig = true;
                }
            }
        }

        public void DisplayFrame(){
            int index;
            if(mbCodecConfig){
                index = mDecoderCodec.dequeueOutputBuffer(mbufferInfo, mWaitForFrame);
                if (index >= 0) {
                    mDecoderCodec.releaseOutputBuffer(index, true);
                    mDecoderCountfps++;
                }
            }
        }

       @Override
        public void run(){
            int index;
            while(mDecoderStarted){
                index = mDecoderCodec.dequeueOutputBuffer(mbufferInfo, mWaitForFrame);
                if (index >= 0) {
                    mDecoderCodec.releaseOutputBuffer(index, true);
                    mDecoderCountfps++;
                }
            }
        }
    }

    private class MyCountFPSThread extends Thread{
        @Override
        public void run() {
            if(mCountfpsHandler != null){
                mCountfpsHandler.handleMessage(new Message());
                mCountfpsHandler.postDelayed(mCountfpsThread,1000);
            }
        }
    }

    private class MyCountHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            mPreviewFPSTextView.setText("Preview"+String.valueOf(mCountfps)+"FPS");
            mCountfps = 0;
            mEncoderDecoderTextView.setText("Codec"+String.valueOf(mDecoderCountfps)+"FPS");
            mDecoderCountfps = 0;
            super.handleMessage(msg);
        }
    }

    private class CamerPrviewCallback implements Camera.PreviewCallback{
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            if(bytes != null) {
                if (mEncoderStarted) {
                   mEncoderCodecHandler.EncoderQueueIputBuffer(bytes);
                }
                mCountfps++;
            }else{
                Log.d(TAG, "onPreviewFrame callback--------------------return");
            }
        }
    }

    private class MyAdapterViewOnItemSelectedListener implements AdapterView.OnItemSelectedListener{
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            switch (view.getId())
            {
                case R.id.fenbianlv_spinner:
                    mPreviewParamterViewoHeight =msuppertedPreviewSize.get(i).height;
                    mPreviewParamterVideoWidth =msuppertedPreviewSize.get(i).width;
                    break;
                case R.id.bitratelist_spinner:
                    mEncoderBitrate = mBitRateList[i];
                    break;
                case R.id.previewformat_type_spinner:
                    mPreviewImageFormat =  mPreviewFomatTypeMaps.get(i);
                    Log.i(TAG, "mPreviewImageFormat = " + mPreviewImageFormat);
                    switch (mPreviewImageFormat){
                        case ImageFormat.YV12:
                            mcoderType = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
                            break;
                        case ImageFormat.NV21:
                            mcoderType = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
                            break;
                        case ImageFormat.YUY2:
	                        //mcoderType = MediaCodecInfo.CodecCapabilities.COLOR_FormatYCbYCr;
    	                    mcoderType = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }


     /*private class MyCameraMediaRecoderAndMediaDecoder{
        private MediaRecorder mMediaRecoder = null;
        private FileOutputStream mOutPutStream = null;
        private MemoryFile mMemoryFile = null;
        private File mtempfile = null;


       public MyCameraMediaRecoderAndMediaDecoder(){
            if(mMediaRecoder == null){
                mMediaRecoder = new MediaRecorder();
            }
            if(mtempfile == null){
                try {
                    mtempfile = File.createTempFile("scott", null);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            if(mMemoryFile == null){
                try {
                    mMemoryFile = new MemoryFile("mstarcoder", 1000000);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }

            if(mMediaRecoder != null){
                mMediaRecoder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                //mMeidaRecoder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mMediaRecoder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mMediaRecoder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                mMediaRecoder.setVideoSize(mPreviewParamterVideoWidth, mPreviewParamterViewoHeight);
                mMediaRecoder.setVideoFrameRate(25);
                mMediaRecoder.setOutputFile(mMemoryFile.getFileDescriptor());
            }
        }
    }*/
}
