package com.mstarsemi.mynetworkplayerapplication;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by scott.cao on 2015/11/13.
 */
public class LoadConfigFile {

    private String TAG = "LoadConfigFile";
    private URL mURL = null;
    private ArrayList<FileContent>  mCurrentFileList = null;
    static private LoadConfigFile mInstance = null;
    private List<String> mCurrentFileLists = null;

    static LoadConfigFile GetInstance()
    {
        if(mInstance == null){
            mInstance = new LoadConfigFile();
        }
        return mInstance;
    }

    public  void setCurrentURL(String url){
        try {
            mURL = new URL(url);
        }catch (MalformedURLException e){
            e.printStackTrace();
        }
    }

    public boolean isMediaFile(int i)
    {
        if( mCurrentFileList.get(i).getUrladdrType().equals("movie")) {
            return true;
        }
        else{
            return false;
        }
    }

    public List<String> getFileList()
    {
        return mCurrentFileLists;
    }

    public  void ParseLoadedFile()
    {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser saxParser = factory.newSAXParser();
            DefaultHandler handler = new MyContentHandler();
            InputStream in = mURL.openStream();
            saxParser.parse(in,handler);
            in.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private LoadConfigFile( ) {
        mURL = null;
    }

    private class MyContentHandler extends DefaultHandler{

        private String mtageName = null;
        private FileContent mCurrentParseContent = null;

        public MyContentHandler(){
            mCurrentFileList = new ArrayList<FileContent>();
            mCurrentFileList.clear();
            mCurrentFileLists = new ArrayList<String>();
            mCurrentFileLists.clear();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if(localName.equals("MOVIE"))
            {
                mCurrentParseContent = new FileContent();
            }
            mtageName = localName;
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if(localName.equals("MOVIE")) {
                if (mCurrentFileList != null) {
                    mCurrentFileList.add(mCurrentParseContent);
                }
                mCurrentParseContent = null;
            }
            mtageName = null;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String str = new String(ch,start,length);
            if(mtageName == null) {
                return ;
            }
            if(mtageName.equals("URL"))
            {
                if(mCurrentParseContent != null) {
                    mCurrentParseContent.setAddr(str);
                    mCurrentFileLists.add(str);
                }
            }else if(mtageName.equals("Type")){
                if(mCurrentParseContent != null){
                    mCurrentParseContent.setUrladdrtype(str);
                }
            }
        }
    }

    // private class for update addr
    private  class FileContent {
        private String murladdr = null;
        private String murladdrtype= null;

        public void setAddr(String addr)
        {
            murladdr = addr;
        }
        public void setUrladdrtype(String type)
        {
            murladdrtype = type;
        }
        public String getAddr()
        {
            return murladdr;
        }
        public  String getUrladdrType()
        {
            return murladdrtype;
        }
    }

}
