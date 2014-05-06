package com.DVLA.testapp.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;
import android.os.Environment;
import android.net.Uri;

import org.opencv.android.OpenCVLoader;
import org.opencv.objdetect.CascadeClassifier;
import org.w3c.dom.Text;

import com.googlecode.tesseract.android.*;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createFileStructure();
        createTrainedData();
        if (!OpenCVLoader.initDebug()) {
            Log.i("OpenCVLoader","Failed");
        }
        gotoStart();
    }

    protected void gotoStart() {
        setContentView(R.layout.start);
        final Button photoButton = (Button) findViewById(R.id.photoButton);
        photoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gotoLiveCamera();
            }
        });

        final Button regButton = (Button) findViewById(R.id.regButton);
        regButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gotoManualVrm("");
            }
        });
    }
    public Texture texture;
    protected void gotoManualVrm(String vrm) {
        setContentView(R.layout.manual_vrm);
        TextView textView = (TextView)findViewById(R.id.editText);
        textView.setText(vrm);
        setupResultsSearch();
    }
    protected void gotoLiveCamera() {
        setContentView(R.layout.live_camera);
        mHandler.post(mUpdate);
        TextureView textureView= (TextureView)findViewById(R.id.textureView);
        texture = new Texture();
        texture.run(textureView);

        final Button imperfect = (Button) findViewById(R.id.imperfectVRM);
        imperfect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TextView textView= (TextView)findViewById(R.id.textView);
                gotoManualVrm(textView.getText().toString());
            }
        });

        final Button perfect = (Button) findViewById(R.id.perfectVRM);
        perfect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TextView textView= (TextView)findViewById(R.id.textView);
                hideSoftKeyboard();
                String param = textView.getText().toString();
                setContentView(R.layout.results);
                FrameLayout loadingFrame = (FrameLayout) findViewById(R.id.loadingFrame);
                TextView VRM = (TextView) findViewById(R.id.VRM);
                TextView Make = (TextView) findViewById(R.id.Make);
                TextView Model = (TextView) findViewById(R.id.Model);
                TextView FirstReg = (TextView) findViewById(R.id.FirstReg);
                TextView Tax = (TextView) findViewById(R.id.Tax);
                TextView MOT = (TextView) findViewById(R.id.MOT);
                TextView Insured = (TextView) findViewById(R.id.Insured);
                new HttpRequest().execute(param,VRM,Make,Model,FirstReg,Tax,MOT,Insured,loadingFrame);
            }
        });
    }

    protected void setupResultsSearch() {
        final Button searchVrm = (Button) findViewById(R.id.vrmButton);
        searchVrm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hideSoftKeyboard();
                EditText editText = (EditText)findViewById(R.id.editText);
                String param = editText.getText().toString();
                setContentView(R.layout.results);
                FrameLayout loadingFrame = (FrameLayout) findViewById(R.id.loadingFrame);
                TextView VRM = (TextView) findViewById(R.id.VRM);
                TextView Make = (TextView) findViewById(R.id.Make);
                TextView Model = (TextView) findViewById(R.id.Model);
                TextView FirstReg = (TextView) findViewById(R.id.FirstReg);
                TextView Tax = (TextView) findViewById(R.id.Tax);
                TextView MOT = (TextView) findViewById(R.id.MOT);
                TextView Insured = (TextView) findViewById(R.id.Insured);
                new HttpRequest().execute(param,VRM,Make,Model,FirstReg,Tax,MOT,Insured,loadingFrame);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createFileStructure() {
        File storageDir = new File (Environment.getExternalStorageDirectory().getAbsolutePath() + "/Taxed");
        File tessData = new File (Environment.getExternalStorageDirectory().getAbsolutePath() + "/Taxed/tessdata");
        storageDir.mkdirs();
        tessData.mkdirs();
    }

    public void hideSoftKeyboard() {
        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }


    private ImageView mImageView;
    private TextView mTextView;
    static public Boolean killHandler = false;
    static public String currResultText;
    static public Bitmap currResultBmp;
    static public Bitmap origBmp;


    Handler mHandler = new Handler();
    private Runnable mUpdate = new Runnable() {
        @Override
        public void run() {
            try {
                mImageView = (ImageView)findViewById(R.id.imageView);
                mTextView = (TextView)findViewById(R.id.textView);
                if(currResultBmp!=null) {mImageView.setImageBitmap(currResultBmp);}
                if(currResultText!=null) {mTextView.setText(currResultText);}
                if(killHandler == false) {
                    mHandler.postDelayed(this, 20);
                }
            } catch (Exception e) {
                Log.i("Err: ","Handler crapped out");
            }
        }
    };

    public void createTrainedData() {
        Context a = this;
        AssetManager assets = a.getAssets();
        try {
            FileOutputStream out2 = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Taxed/tessdata/eng.traineddata");
            InputStream in2 = assets.open("eng.traineddata");
            byte[] buffer2 = new byte[1024];
            int len2;
            while ((len2 = in2.read(buffer2)) != -1) {
                out2.write(buffer2, 0, len2);
            }

        } catch (Exception e) {
            Log.i("Failed","Full Failure");
        }
    }

}