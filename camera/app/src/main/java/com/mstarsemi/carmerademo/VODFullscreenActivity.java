package com.mstarsemi.mynetworkplayerapplication;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.TimedText;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import android.os.Handler;
import android.widget.Toast;


public class VODFullscreenActivity extends Activity implements View.OnClickListener,MediaPlayer.OnPreparedListener,MediaPlayer.OnCompletionListener {
    private final String TAG = "VODFullscreenActivity";
    private String rootRUL = null;
    private Button mFileListButton = null;
    private Button mPlayPauseButton = null;
    private LoadConfigFile mLoadConfigFile = null;
    private ListView mListView = null;
    private List<String> mCurrentfilelist = null;
    private ArrayAdapter<String> madapter = null;
    private ArrayAdapter<String> mAudioTrackadapter = null;
    private ArrayAdapter<String> mSubtitleTrackadapter = null;
    private MediaPlayer mMediaPlayer = null;
    private SurfaceView mSurfaceView = null;
    private SurfaceHolder mSurfaceHolder = null;
    private Stack<String> mStackURL = null;
    private String mCurrentURL = null;
    private List<MyTrackInfoListIndex> mAudioTrackInfo = null;
    private List<MyTrackInfoListIndex> mSubtitleTrackInfo = null;
    private Spinner mAudioTrackSpinner = null;
    private Spinner mSubtitleTrackSpinner = null;
    private TextView mSubtitleTextView = null;
    private TextView mTotalTime = null;
    private Button mSeekButton = null;
    private Button mSeekbackButton = null;
    private SeekBar mProgressSeekBare = null;
    private Thread mProgressBareHandle = null;
    private int mFileTotalDuration = 0;
    private Handler mHandler = null;
    private Handler mUpdateUIHandler = null;
    private boolean mbSeeking = false;
    private Toast mToast = null;
    private int mpreSubtitleTrackIndex = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vodfullscreen);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        rootRUL = bundle.getString("serverip");
        rootRUL=rootRUL+"/PV"+"/main.xml";

        mSurfaceView = (SurfaceView)findViewById(R.id.VODSurfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();

        mFileListButton = (Button) findViewById(R.id.FileList);
        mFileListButton.setOnClickListener(this);
        mPlayPauseButton = (Button)findViewById(R.id.PlayPause);
        mPlayPauseButton.setOnClickListener(this);

        mAudioTrackSpinner = (Spinner)findViewById(R.id.AudioTrackList);
        mAudioTrackSpinner.setOnItemSelectedListener(new MyAudioTrackSpinnerSelectedListerner());
        mSubtitleTrackSpinner = (Spinner)findViewById(R.id.SubtitleTrackList);
        mSubtitleTrackSpinner.setOnItemSelectedListener(new MySubtitleTrackSpinnerSelectedListerner());
        mSubtitleTextView = (TextView)findViewById(R.id.subtitletextDisplay);

        mListView = (ListView) findViewById(R.id.FileListView);
        mListView.setOnItemClickListener(new ListViewOnItemClickListener());
        mTotalTime = (TextView)findViewById(R.id.totoaltime);

        mSeekButton = (Button)findViewById(R.id.PlaySeek);
        mSeekbackButton =(Button)findViewById(R.id.PlaySeekback);
        mSeekbackButton.setOnClickListener(this);
        mSeekButton.setOnClickListener(this);

        mProgressSeekBare = (SeekBar)findViewById(R.id.timebar);
        mProgressSeekBare.setOnClickListener(this);

        mCurrentfilelist = new ArrayList<String>();
        mCurrentfilelist.clear();

        mStackURL = new Stack<String>();
        mStackURL.clear();
        mCurrentURL = rootRUL;

        mAudioTrackInfo = new ArrayList<MyTrackInfoListIndex>();
        mSubtitleTrackInfo = new ArrayList<MyTrackInfoListIndex>();
        mUpdateUIHandler = new UpdateUIHandler();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        switch (event.getKeyCode()){
            case KeyEvent.KEYCODE_BACK:
            {
                if(mProgressSeekBare.isFocused()){
                    mFileListButton.requestFocus();
                    return true;
                }else if(mListView.getVisibility() == View.VISIBLE){
                    mListView.setVisibility(View.GONE);
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode,event);
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.FileList: {
                if(mListView.getVisibility() == View.VISIBLE){
                    mListView.setVisibility(View.GONE);
                }else {
                    DisplayFileListView();
                }
                break;
            }
            case R.id.PlayPause: {
                if (mMediaPlayer != null) {
                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.pause();
                        mPlayPauseButton.setText("播放");
                    } else {
                        mMediaPlayer.start();
                        mPlayPauseButton.setText("暂停");
                    }
                }
                break;
            }
            case R.id.PlaySeek:{
                    //快进
                    DoSeek(true);
                    break;
            }
            case R.id.PlaySeekback:{
                //快退
                DoSeek(false);
                break;
            }

            case R.id.timebar:{
                int currentposition = mProgressSeekBare.getProgress();
                int totalposition = mProgressSeekBare.getMax();
                DisplayProgressSeekBareInfo();
                if(mMediaPlayer != null){
                    mMediaPlayer.seekTo(mFileTotalDuration*currentposition/totalposition);
                }
                mToast.cancel();
                mFileListButton.requestFocus();
                break;
            }
            default:
                break;
        }
    }
    @Override
    public void onCompletion(MediaPlayer mp)
    {
        mPlayPauseButton.setText("播放");
        mPlayPauseButton.requestFocus();
        mSubtitleTextView.setVisibility(View.GONE);
    }

    @Override
    public  void onPrepared(MediaPlayer mediaPlayer) {
        mMediaPlayer.start();
        if(mUpdateUIHandler != null){
            mUpdateUIHandler.sendMessage(new Message());
        }
    }

    private synchronized void DisplayViews(){
        mpreSubtitleTrackIndex =0;
        mSubtitleTextView.setVisibility(View.GONE);
        mPlayPauseButton.setVisibility(View.VISIBLE);
        mSeekButton.setVisibility(View.VISIBLE);
        mSeekbackButton.setVisibility(View.VISIBLE);
        DisplayAudioAndSubtitleTrackList();
        DisplayTimebar(true);
    }

    private synchronized void DisplayTimebar(boolean bDisplay){
        if(mHandler != null && mProgressBareHandle != null){
            mHandler.removeCallbacks(mProgressBareHandle);
            mHandler = null;
            mProgressBareHandle = null;
        }
        if(bDisplay) {
            if (mMediaPlayer != null) {
                mFileTotalDuration = mMediaPlayer.getDuration();
            }
            mTotalTime.setVisibility(View.VISIBLE);
            mProgressSeekBare.setVisibility(View.VISIBLE);
            mProgressSeekBare.setIndeterminate(false);
            mHandler = new myHandler();
            mProgressBareHandle = new MyProgramssBar();
            mProgressBareHandle.start();
        }
    }

    private synchronized void DoSeek(Boolean bseek){
        if(mMediaPlayer != null) {
            int currentPosition = mMediaPlayer.getCurrentPosition();
            if(bseek){ //seek front
                currentPosition += 5000;
                if(currentPosition > mFileTotalDuration){
                    currentPosition = mFileTotalDuration;
                }

            }else{//seek back
                currentPosition -=5000;
                if(currentPosition < 0){
                    currentPosition = 0;
                }
            }
            mMediaPlayer.seekTo(currentPosition);
        }
    }
    private synchronized void DisplayAudioAndSubtitleTrackList(){
        mAudioTrackInfo.clear();
        mSubtitleTrackInfo.clear();
        if(mMediaPlayer != null){
            try {
                MediaPlayer.TrackInfo[] trackInfo= mMediaPlayer.getTrackInfo();
                for(int i =0; i< trackInfo.length; i++)
                {
                   // Log.d(TAG,"TrackInof type is :"+trackInfo[i].getTrackType());
                    switch (trackInfo[i].getTrackType())
                    {
                        case MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO: {
                            mAudioTrackInfo.add(new MyTrackInfoListIndex(i,trackInfo[i]));
                            break;
                        }
                        case MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT:
                        case MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE:
                        case 5:{
                            mSubtitleTrackInfo.add(new MyTrackInfoListIndex(i, trackInfo[i]));
                            break;
                        }

                    }
                }
            }catch (IllegalStateException e){
                e.printStackTrace();
            }

            if(mAudioTrackInfo.size() > 1) {
                mAudioTrackadapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
                for(int i =0; i < mAudioTrackInfo.size();) {
                    ++i;
                    mAudioTrackadapter.add("音频"+i);
                }
                mAudioTrackadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mAudioTrackSpinner.setAdapter(mAudioTrackadapter);
                mAudioTrackSpinner.setVisibility(View.VISIBLE);
            }else{
                mAudioTrackSpinner.setVisibility(View.GONE);
            }
            //Log.d(TAG, "subtitle size is " + mSubtitleTrackInfo.size());
            if(mSubtitleTrackInfo.size() > 0) {
                mMediaPlayer.setOnTimedTextListener(new MyOnTimedTextListener());
                mSubtitleTrackadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
                mSubtitleTrackadapter.add("字幕关闭");
                for (int i = 0; i < mSubtitleTrackInfo.size(); ) {
                    i++;
                    mSubtitleTrackadapter.add("字幕" + i);
                }
                mSubtitleTrackadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mSubtitleTrackSpinner.setAdapter(mSubtitleTrackadapter);
                mSubtitleTrackSpinner.setVisibility(View.VISIBLE);
            }else{
                mSubtitleTextView.setVisibility(View.GONE);
                mSubtitleTrackSpinner.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onStop(){
        DisplayTimebar(false);
        PlayerFile(mCurrentURL, false);
        super.onStop();
    }

    private synchronized void PlayerFile(String URL, boolean bPlay)
    {
        if(mMediaPlayer != null) {
            //DisplayTimebar(false);
            //mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mFileTotalDuration = 0;
        }
        if(bPlay) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            try {
                mMediaPlayer.setDataSource(mSurfaceView.getContext(), Uri.parse(URL));
                mMediaPlayer.setDisplay(mSurfaceHolder);
                mMediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void DisplayFileListView()
    {
        //create a thread for download config file, and parse file
        Thread downloadHandler = new Thread(new Runnable() {
            @Override
            public void run() {
                mLoadConfigFile = LoadConfigFile.GetInstance();
                mLoadConfigFile.setCurrentURL(mCurrentURL);
                mLoadConfigFile.ParseLoadedFile();
            }
        });
        downloadHandler.start();
        try {
            downloadHandler.join(); //wait downlaod
            mListView.setVisibility(View.VISIBLE);
            mCurrentfilelist =  mLoadConfigFile.getFileList();
            if(!(mCurrentURL.equals(rootRUL))) {
                mCurrentfilelist.add("返回上一级目录");
            }
            madapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mCurrentfilelist);
            mListView.setAdapter(madapter);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    private synchronized String TimetoString(int time){
        int min =0, hour =0, second =0;
        String name = "00:00:00";
        if(time > 0)
        {
            int totaltimes = time/1000;
            min = totaltimes/60 %60;
            hour = totaltimes/60/60;
            second = totaltimes%60;
            name = String.format("%02d:%02d:%02d",hour,min,second);
        }
        return name;
    }

    private synchronized void DisplayProgressSeekBareInfo(){
        int currentposition = mProgressSeekBare.getProgress();
        int totalposition = mProgressSeekBare.getMax();
        String info= TimetoString(mFileTotalDuration * currentposition / totalposition);
        if(mToast != null){
            mToast.cancel();
        }
        mToast = Toast.makeText(this,info,Toast.LENGTH_SHORT);
        mToast.show();
    }

    private class ListViewOnItemClickListener implements AdapterView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int index = (int)id;
            boolean bIsMovie = false;
            String URL = mCurrentfilelist.get(index); // get the url
            if(URL.equals("返回上一级目录")) {
                if(mStackURL.empty()){
                    mCurrentURL = rootRUL;
                }else{
                    mCurrentURL = mStackURL.pop();
                    DisplayFileListView();
                }
            }else {
                bIsMovie = mLoadConfigFile.isMediaFile(index);
                if (bIsMovie) {
                    PlayerFile(URL,true);
                } else {
                    //is xml file
                    mStackURL.push(mCurrentURL);
                    mCurrentURL = URL;
                    DisplayFileListView();
                }
            }
        }
    }
    private class MyAudioTrackSpinnerSelectedListerner implements Spinner.OnItemSelectedListener{
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG,"selected index is: "+i);
                mMediaPlayer.selectTrack(mAudioTrackInfo.get(i).GetIndex());
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }

    private class MySubtitleTrackSpinnerSelectedListerner implements Spinner.OnItemSelectedListener{

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            if(mpreSubtitleTrackIndex != 0){
                mMediaPlayer.deselectTrack(mpreSubtitleTrackIndex);
            }
            if(i == 0) {
                Log.d(TAG,"关闭字幕");
                mpreSubtitleTrackIndex = 0;
            }else{
                mpreSubtitleTrackIndex =(mSubtitleTrackInfo.get(i - 1).GetIndex());
                mMediaPlayer.selectTrack(mpreSubtitleTrackIndex);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

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

    private class MyOnSeekCompleteListener implements MediaPlayer.OnSeekCompleteListener{
        @Override
        public void onSeekComplete(MediaPlayer mediaPlayer) {

        }
    }

    private class MyProgramssBar extends Thread {
        @Override
        public void run() {
                    if(mHandler != null){
                        mHandler.sendMessage(new Message());
                        mHandler.postDelayed(mProgressBareHandle,1000);
                    }
                }
    }

    private class myHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            if (mMediaPlayer != null) {
                if (!mbSeeking && mMediaPlayer.isPlaying()) {
                    int currentpostion = mMediaPlayer.getCurrentPosition();
                    if(mProgressSeekBare.isFocused()) {
                        DisplayProgressSeekBareInfo();
                    }else {
                        mProgressSeekBare.setProgress(mProgressSeekBare.getMax() * currentpostion / mFileTotalDuration);
                    }
                    mTotalTime.setText(TimetoString(currentpostion) + "/" + TimetoString(mFileTotalDuration));
                }
            }
            super.handleMessage(msg);
        }
    }



    private class UpdateUIHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            DisplayViews();
            super.handleMessage(msg);
        }
    }
}
