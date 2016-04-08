package com.mstarsemi.mynetworkplayerapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends Activity implements View.OnClickListener {

    private String TAG= "MyNetWorkPlayerApplication";
    private Button mzhiboButton = null;
    private Button mdianboButton = null;
    private Button mshipintonghuaButton = null;
    private EditText mServerEditText = null;
    private TextView mSelectConfigTextView = null;
    private CheckBox mFastStarCheckBox = null;
    private CheckBox mFreeRunCheckBox = null;

    private CheckBox.OnCheckedChangeListener myCheckBoxListener = null;
    private String mServerAddr = null;
    private MySystemProperty mProperty = null;
    private boolean mbIsMstarPlatform = true;
    private  boolean mbServerStatuse = false;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mzhiboButton = (Button)findViewById(R.id.ZhiboButton);
        mzhiboButton.setOnClickListener(this);
        mdianboButton = (Button)findViewById(R.id.DianboButton);
        mdianboButton.setOnClickListener(this);
        mshipintonghuaButton = (Button)findViewById(R.id.ShipintonghuaButton);
        mshipintonghuaButton.setOnClickListener(this);

        myCheckBoxListener = new MyCheckBoxOnCheckedChangeListener();

        mFastStarCheckBox = (CheckBox)findViewById(R.id.select_fastStar);
        mFastStarCheckBox.setOnCheckedChangeListener(myCheckBoxListener);
        mFreeRunCheckBox = (CheckBox)findViewById(R.id.select_freerun);
        mFreeRunCheckBox.setOnCheckedChangeListener(myCheckBoxListener);

        mSelectConfigTextView = (TextView)findViewById(R.id.select_config);
        mServerEditText = (EditText)findViewById(R.id.EditTextIp);
        mServerEditText.addTextChangedListener(new ServerTextWatcher());
        mzhiboButton.requestFocus();
    }

    @Override
    protected void onResume(){
        isConectedMainserver();
        DisplayMore();
        super.onResume();
    }

    @Override
    public void onClick(View view) {
        Intent intent= new Intent();
        switch (view.getId()) {
            case R.id.ZhiboButton:
                if(getMainServerStatuse()) {
                    intent.setClass(this, DireactFullscreenActivity.class);
                    intent.putExtra("serverip", mServerEditText.getText().toString().replaceAll("\r|\n|\t| ", ""));
                    startActivity(intent);
                }else{
                    Toast.makeText(this,"无法连接服务器, 请检查服务器网络",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.DianboButton:
                if(getMainServerStatuse()) {
                    intent.setClass(this, VODFullscreenActivity.class);
                    intent.putExtra("serverip", mServerEditText.getText().toString().replaceAll("\r|\n|\t| ", ""));
                    startActivity(intent);
                }else{
                    Toast.makeText(this,"无法连接服务器, 请检查服务器网络",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.ShipintonghuaButton:
                intent.setClass(this,CamerFullscreenActivity.class);
                startActivity(intent);
                break;
        }
    }

    private void DisplayMore(){
        if(mProperty == null){
            mProperty = MySystemProperty.GetInstance();
        }
        mbIsMstarPlatform = mProperty.IsMStarPlatform();
        if(mbIsMstarPlatform){
            mFreeRunCheckBox.setFocusable(true);
            mFastStarCheckBox.setFocusable(true);
            mFreeRunCheckBox.setVisibility(View.VISIBLE);
            mFastStarCheckBox.setVisibility(View.VISIBLE);
            mSelectConfigTextView.setVisibility(View.VISIBLE);
        }else{
            mFreeRunCheckBox.setFocusable(false);
            mFastStarCheckBox.setFocusable(false);
            mFreeRunCheckBox.setVisibility(View.GONE);
            mFastStarCheckBox.setVisibility(View.GONE);
            mSelectConfigTextView.setVisibility(View.GONE);
        }
    }

    private  boolean getMainServerStatuse(){
        return  mbServerStatuse;
    }


    private  void isConectedMainserver(){
       new Thread(new Runnable() {
            @Override
            public void run() {
                URL url = null;
                HttpURLConnection http = null;
                try
                {
                    url = new URL(mServerEditText.getText().toString().replaceAll("\r|\n|\t| ", "") + "/PV/DT.xml");
                     http = (HttpURLConnection) url.openConnection();
                    http.setConnectTimeout(5000);
                    int nRc = http.getResponseCode();
                    if (nRc == HttpURLConnection.HTTP_OK) {
                        mbServerStatuse = true;
                    }
                }
                catch(MalformedURLException e) {
                    e.printStackTrace();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
                finally {
                    if(http != null) {
                        http.disconnect();
                    }
                }
            }
        }).start();
    }

    private class ServerTextWatcher implements TextWatcher{
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            isConectedMainserver();
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }
    }

    private class MyCheckBoxOnCheckedChangeListener implements CheckBox.OnCheckedChangeListener{
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            switch (compoundButton.getId()){
                case R.id.select_fastStar:{
                    SetFastStart(b);
                    break;
                }
                case R.id.select_freerun:{
                    SetFreeRun(b);
                    break;
                }
            }
        }

        private void SetFastStart(boolean bselect){
            if(bselect){
                mProperty.SetProperty("ms.mstplayer.fast.start", "true");
            }else{
                mProperty.SetProperty("ms.mstplayer.fast.start", "false");
            }
        }

        private void  SetFreeRun(boolean bselect){
            if(bselect){
                mProperty.SetProperty("ms.avp.no.avsync", "true");
            }else{
                mProperty.SetProperty("ms.avp.no.avsync", "false");
            }
        }
    }
}
