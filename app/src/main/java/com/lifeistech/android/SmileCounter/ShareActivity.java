package com.lifeistech.android.SmileCounter;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import java.io.File;

public class ShareActivity extends AppCompatActivity {

    private Button shareTweetButton;

    private String fileName;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        shareTweetButton = (Button) findViewById(R.id.twitterButton);

        shareTweetButton.setText("Twitter");

        Intent intent = getIntent();
        fileName = intent.getStringExtra("FileName");

        if (Build.VERSION.SDK_INT >= 19) {

            File file = new File(getFilesDir(), "SmileCounter");
            File imageFile = new File(file, fileName);

            selectedImageUri = FileProvider.getUriForFile(this, "com.lifeistech.android.SmileCounter" + ".fileprovider", imageFile);
        } else {
            File file = new File(Environment.getExternalStorageDirectory(), "SmileCounter");
            File imageFile = new File(file, fileName);
            selectedImageUri = Uri.fromFile(imageFile);
        }

    }

    public void shareTweet(View v) {
        Twitter.initialize(this);

        TweetComposer.Builder builder = new TweetComposer.Builder(this)
                .text("just setting up my Twitter Kit.")
                .image(selectedImageUri);
        builder.show();
    }

    public void shareFacebook(View v) {
        //Facebookで共有する方法わからない
    }

}
