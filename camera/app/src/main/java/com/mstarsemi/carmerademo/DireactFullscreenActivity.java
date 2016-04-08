package com.mstarsemi.mynetworkplayerapplication;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.TimedText;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IllegalFormatCodePointException;
import java.util.List;


public class DireactFullscreenActivity extends Activity implements SurfaceHolder.Callback, MediaPlayer.OnPreparedListener{

    private String TAG = "DirectFullscreenActivity";
    private String mDireactFullScreenUrl = null;
    private MediaPlayer mMediaPlayer = null;
    private SurfaceHolder mSurfaceHolder = null;
    private SurfaceView mSurfaceView = null;
    private LoadConfigFile mChanleList = null;
    private List<String> mChanleListURL = null;
    private int mCurrentChanel = 0;
    private int mTotalChangel = 0;

    private Spinner mAudioSpiner = null;
    private Spinner mSubtitleSpiner = null;
    private List<MyTrackInfoListIndex> mAudioTrackList = null;
    private List<MyTrackInfoListIndex> mSubtitleTrackList = null;
    private ArrayAdapter<String> mAudioTrackadapter = null;
    private ArrayAdapter<String> mSubtitleTrackadapter = null;
    private boolean mbInfoDisplayed = true;
    private boolean mbChannelListDisplayed = true;

    private ListView mListView = null;
    private TextView mChannelTextView = null;

    private TextView mSubtitleTextView = null;
    private  int mpreSubtitleTrackIndex =0;
    private boolean mbStarted = false;
    private Handler mUpdateUIHandler = null;
    private SeamlessVideo mSeamlessVideo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direact_fullscreen);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        mDireactFullScreenUrl = bundle.getString("serverip");
        mDireactFullScreenUrl=mDireactFullScreenUrl+"/PV"+"/DT.xml";

        mSurfaceView = (SurfaceView)findViewById(R.id.zhiboSurfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        mAudioSpiner = (Spinner)findViewById(R.id.DirectAudioTrackList);
        mAudioSpiner.setOnItemSelectedListener(new MyDirectAudioTrackSelectedListener());
        mSubtitleSpiner = (Spinner)findViewById(R.id.DirectSubtitleTrackList);
        mSubtitleSpiner.setOnItemSelectedListener(new MyDirectSubtitleTrackSelectedListener());

        mListView = (ListView)findViewById(R.id.DirectFileListView);
        mListView.setOnItemClickListener(new MyListChannelOnItemClieckListener());

        mChannelTextView = (TextView)findViewById(R.id.channelDisplay);
        mSubtitleTextView = (TextView)findViewById(R.id.DirectsubtitletextDisplay);

        mChanleListURL = new ArrayList<String>();
        mChanleListURL.clear();
        GetChannelList();

        mAudioTrackList = new ArrayList<MyTrackInfoListIndex>();
        mSubtitleTrackList = new ArrayList<MyTrackInfoListIndex>();
        mbInfoDisplayed = true;
        mUpdateUIHandler = new UpdateUIHandler();
    }

    @Override
    public  void onPrepared(MediaPlayer mediaPlayer)
    {
        mediaPlayer.start();
        mbStarted=true;
        mpreSubtitleTrackIndex=0;
        if(mUpdateUIHandler != null){
            mUpdateUIHandler.sendMessage(new Message());
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        PlayChannel(true);
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height)
    {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {

    }

    @Override
    protected void onStop(){
        PlayChannel(false);
        super.onStop();
    }

    @Override
    public  boolean onKeyDown(int keyCode, KeyEvent event){

        switch (event.getKeyCode()){
            case KeyEvent.KEYCODE_CHANNEL_UP:
            case KeyEvent.KEYCODE_DPAD_UP:{
                if(mListView.getVisibility() != View.VISIBLE) {
                    if(mCurrentChanel  == mTotalChangel -1){
                        mCurrentChanel =0;
                    }else {
                        mCurrentChanel = (++mCurrentChanel) % mTotalChangel;
                        ;
                    }
                    PlayChannel(true);
                    return  true;
                }
                break;
            }
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
            case KeyEvent.KEYCODE_DPAD_DOWN:{
                if(mListView.getVisibility() != View.VISIBLE) {
                    if(mCurrentChanel == 0){
                        mCurrentChanel = mTotalChangel-1;
                    }else {
                        mCurrentChanel = (--mCurrentChanel) % mTotalChangel;
                    }
                    PlayChannel(true);
                    return true;
                }
                break;
            }
            case KeyEvent.KEYCODE_PROG_BLUE:{
                if(mbStarted) {
                    DisplayInfo(mbInfoDisplayed);
                }
                break;
            }
            case KeyEvent.KEYCODE_ENTER:{
                if(mbStarted){
                    if((mListView.getVisibility() != View.VISIBLE) && (mSubtitleSpiner.getVisibility() != View.VISIBLE) && (mAudioSpiner.getVisibility() != View.VISIBLE))
                    {
                        DisplayChannelList(mbChannelListDisplayed);
                    }
                }
                break;
            }
            case KeyEvent.KEYCODE_BACK:{
                if(mListView.getVisibility() == View.VISIBLE) {
                    DisplayChannelList(false);
                    return true;
                }
                if((mSubtitleSpiner.getVisibility() == View.VISIBLE) || (mAudioSpiner.getVisibility() == View.VISIBLE)){
                    DisplayInfo(false);
                    return true;
                }
                break;
            }
            default:
                break;
        }
        return super.onKeyDown(keyCode,event);
    }

    private synchronized void DisplayInfo(boolean bDisplay){
        if(bDisplay){
            GetAudioAndSubtitleTrackInfo();
            mbInfoDisplayed = false;
        }else{
            mSubtitleTextView.setVisibility(View.GONE);
            mAudioSpiner.setVisibility(View.GONE);
            mSubtitleSpiner.setVisibility(View.GONE);
            mbInfoDisplayed = true;
        }
    }

    private synchronized void DisplayChannelList(boolean bDisplay){
        if(bDisplay) {
            GetChannelList();
            ArrayAdapter<String> adapter = null;
            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mChanleListURL);
            mListView.setAdapter(adapter);
            mListView.setVisibility(View.VISIBLE);
            mbChannelListDisplayed = false;
        }else{
            mListView.setVisibility(View.GONE);
            mbChannelListDisplayed = true;
        }
    }

    private synchronized  void PlayChannel( boolean bPlay){
        mbStarted = false;
        try {
            if(mTotalChangel  > 0) {
                if(mMediaPlayer != null){
                    //mMediaPlayer.stop();
                    if((mSeamlessVideo != null) && bPlay){
                        mSeamlessVideo.SetSeamlessVideo(true);
                    }
                    mMediaPlayer.reset();
                    if(!bPlay) {
                        mMediaPlayer.release();
                        mMediaPlayer = null;
                    }
                }
                if(bPlay) {
                    mChannelTextView.setText(String.valueOf(mCurrentChanel + 1));
                    if(mMediaPlayer == null) {
                        mMediaPlayer = new MediaPlayer();
                        mSeamlessVideo = new SeamlessVideo();
                        mMediaPlayer.setOnPreparedListener(this);
                        mMediaPlayer.setDisplay(mSurfaceHolder);
                    }
                    mMediaPlayer.setDataSource(mSurfaceView.getContext(), Uri.parse(mChanleListURL.get(mCurrentChanel)));
                    mMediaPlayer.prepareAsync();
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void GetAudioAndSubtitleTrackInfo(){
        mAudioTrackList.clear();
        mSubtitleTrackList.clear();
        if(mMediaPlayer != null){
            MediaPlayer.TrackInfo[] trackInfo = mMediaPlayer.getTrackInfo();
            for(int i =0; i < trackInfo.length;i++){
                switch (trackInfo[i].getTrackType()){
                    case MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO:{
                        Log.d(TAG, "audio track info");
                        mAudioTrackList.add(new MyTrackInfoListIndex(i,trackInfo[i]));
                        break;
                    }
                    case MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT:
                    case MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE:
                    case 5: {//mstar extend
                        mSubtitleTrackList.add(new MyTrackInfoListIndex(i,trackInfo[i]));
                        break;
                    }
                    default:
                        break;
                }
            }
            boolean bdisplaytoast = true;
            if(mAudioTrackList.size() > 1) {
                mAudioTrackadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
                for (int i = 0; i < mAudioTrackList.size(); ) {
                    ++i;
                    mAudioTrackadapter.add("音频" + i);
                }
                mAudioTrackadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mAudioSpiner.setAdapter(mAudioTrackadapter);
                mAudioSpiner.setVisibility(View.VISIBLE);
                bdisplaytoast = false;
            }else{
                mAudioSpiner.setVisibility(View.GONE);
            }
            if(mSubtitleTrackList.size() > 0){
                mMediaPlayer.setOnTimedTextListener(new MyOnTimedTextListener());
                mSubtitleTrackadapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
                mSubtitleTrackadapter.add("字幕关闭");
                for(int i = 0; i < mSubtitleTrackList.size();){
                    i++;
                    mSubtitleTrackadapter.add("字幕" + i);
                    mSubtitleTrackadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    mSubtitleSpiner.setAdapter(mSubtitleTrackadapter);
                    mSubtitleSpiner.setVisibility(View.VISIBLE);
                    bdisplaytoast = false;
                }
            }else{
                mSubtitleTextView.setVisibility(View.GONE);
                mSubtitleSpiner.setVisibility(View.GONE);
            }
            if(bdisplaytoast){
                Toast.makeText(this,"单音轨,无字幕码流",Toast.LENGTH_SHORT).show();;
            }else{
                if(mAudioSpiner.getVisibility() == View.VISIBLE){
                    mAudioSpiner.requestFocus();
                }else if(mSubtitleSpiner.getVisibility() == View.VISIBLE){
                    mSubtitleSpiner.requestFocus();
                }
            }
        }
    }

    private void GetChannelList(){
        //create a thread for download config file, and parse file
        Thread downloadHandler = new Thread(new Runnable() {
            @Override
            public void run() {
                mChanleList = LoadConfigFile.GetInstance();
                mChanleList.setCurrentURL(mDireactFullScreenUrl);
                mChanleList.ParseLoadedFile();
            }
        });
        downloadHandler.start();
        try {
            downloadHandler.join(); //wait downlaod
            mChanleListURL =  mChanleList.getFileList();
            mTotalChangel = mChanleListURL.size();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    private class MyDirectAudioTrackSelectedListener implements Spinner.OnItemSelectedListener{
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            Log.d(TAG, "selected index is: " + i);
            mMediaPlayer.selectTrack(mAudioTrackList.get(i).GetIndex());
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }

    private class MyDirectSubtitleTrackSelectedListener implements Spinner.OnItemSelectedListener{

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            Log.d(TAG, "selected index is: " + i);
            if(mpreSubtitleTrackIndex != 0){
                mMediaPlayer.deselectTrack(mpreSubtitleTrackIndex);
            }
            if(i == 0) {
                Log.d(TAG,"关闭字幕");
                mpreSubtitleTrackIndex = 0;
            }else{
                mpreSubtitleTrackIndex = (mSubtitleTrackList.get(i - 1).GetIndex());
                mMediaPlayer.selectTrack(mpreSubtitleTrackIndex);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }

    private class MyOnTimedTextListener implements MediaPlayer.OnTimedTextListener{

        @Override
        public void onTimedText(MediaPlayer mediaPlayer, TimedText timedText) {

            if(timedText != null) {
                mSubtitleTextView.setVisibility(View.VISIBLE);
                mSubtitleTextView.setText(timedText.getText());
            }else{
                mSubtitleTextView.setVisibility(View.GONE);
            }
        }
    }

    private class MyTrackInfoListIndex{
        private MediaPlayer.TrackInfo mIndexTrackInfo;
        private int mindex;

        public MyTrackInfoListIndex(int index, MediaPlayer.TrackInfo trackInfo){
            mindex = index;
            mIndexTrackInfo = trackInfo;
        }

        public int GetIndex(){
            return mindex;
        }

        public MediaPlayer.TrackInfo GetTrackInfo(){
            return mIndexTrackInfo;
        }
    }

    private class MyListChannelOnItemClieckListener implements  AdapterView.OnItemClickListener{
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mCurrentChanel = position;
            PlayChannel(true);
        }
    }

    private class UpdateUIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            DisplayInfo(false);
        }
    }

    private class SeamlessVideo{
        private Method minvoke;
        private Method mnewRequest;
        private final int INVOKE_ID_SET_SEAMLESS_MODE = 2000;
        private final int INVOKE_ID_SET_FAST_START_MODE = 2001;

        public SeamlessVideo(){
            try {
                minvoke = mMediaPlayer.getClass().getDeclaredMethod("invoke", Parcel.class, Parcel.class);
                mnewRequest = mMediaPlayer.getClass().getDeclaredMethod("newRequest");
            }catch (NoSuchMethodException e){
                e.printStackTrace();
            }
        }

        public void SetSeamlessVideo(boolean bSeamless){
            if(bSeamless){
                try {
                    Parcel request = (Parcel)mnewRequest.invoke(mMediaPlayer);
                    Parcel repply = Parcel.obtain();
                    request.writeInt(INVOKE_ID_SET_SEAMLESS_MODE);
                    request.writeInt(1);
                    minvoke.invoke(mMediaPlayer,request,repply);
                }catch (InvocationTargetException e){
                    e.printStackTrace();;
                }catch (IllegalAccessException e){
                    e.printStackTrace();
                }
            }else{
                //this will be need fixed!!!!
            }
        }
    }
}
