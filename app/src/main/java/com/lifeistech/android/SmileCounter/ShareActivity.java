package com.lifeistech.android.SmileCounter;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
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
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareButton;
import com.facebook.share.widget.ShareDialog;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class ShareActivity extends AppCompatActivity {

    private Button shareTweetButton;
    private ShareButton shareButton;
    private Button shareInstagramButton;
    private TextView shareTextView;

    private String fileName;
    private Uri selectedImageUri;

    private int score;

    private Bitmap image;

    private CallbackManager callbackManager;
    private ShareDialog shareDialog;
    private SharePhotoContent content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        shareTweetButton = (Button) findViewById(R.id.twitterButton);
        shareButton = (ShareButton) findViewById(R.id.facebookShare);
        shareInstagramButton = (Button) findViewById(R.id.instagramButton);
        shareTextView = (TextView) findViewById(R.id.shareText);

        shareTweetButton.setText("Twitter");
        shareTweetButton.setAllCaps(false);
        shareInstagramButton.setText("Instagram");
        shareInstagramButton.setAllCaps(false);

        shareTextView.setTypeface(Typeface.createFromAsset(getAssets(), "JiyunoTsubasa.ttf"));

        Intent intent = getIntent();
        fileName = intent.getStringExtra("FileName");
        score = intent.getIntExtra("Point", 0);

        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                //success
            }

            @Override
            public void onCancel() {
                //cancel
            }

            @Override
            public void onError(FacebookException error) {
                //error
            }
        });

        if (Build.VERSION.SDK_INT >= 19) {

            File file = new File(getFilesDir(), "SmileCounter");
            File imageFile = new File(file, fileName);

            selectedImageUri = FileProvider.getUriForFile(this, "com.lifeistech.android.SmileCounter" + ".fileprovider", imageFile);
        } else {
            File file = new File(Environment.getExternalStorageDirectory(), "SmileCounter");
            File imageFile = new File(file, fileName);
            selectedImageUri = Uri.fromFile(imageFile);
        }

        try {
            InputStream stream = getContentResolver().openInputStream(selectedImageUri);
            image = BitmapFactory.decodeStream(stream);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Facebookの投稿準備
        if (ShareDialog.canShow(SharePhotoContent.class)) {
            SharePhoto photo = new SharePhoto.Builder().setBitmap(image).build();

            content = new SharePhotoContent.Builder().addPhoto(photo)
                    .setShareHashtag(new ShareHashtag.Builder().setHashtag("#SmileCounter").build())
                    .build();
            shareButton.setShareContent(content);
        }

    }

    public void shareTweet(View v) {
        Twitter.initialize(this);

        TweetComposer.Builder builder = new TweetComposer.Builder(this)
                .text("#SmileCounter " + String.valueOf(score) + "pt !!")
                .image(selectedImageUri);
        builder.show();
    }

    public void shareFacebook(View v) {
        shareDialog.show(content);
    }

    private boolean appInstalledOrNot(String uri) {
        PackageManager pm = getPackageManager();
        boolean app_installed = false;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    public void shareInstagram(View v) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setData(selectedImageUri);
        intent.setType("image/jpg");
        intent.addCategory(Intent.CATEGORY_DEFAULT);

        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        String className = null;
        String packegeName = null;
        for (ResolveInfo info : resolveInfos) {
            if ("Instagram".equals(info.loadLabel(pm)) || "instagram".equals(info.loadLabel(pm))) {
                className = info.activityInfo.name;
                packegeName = info.activityInfo.packageName;
                break;
            }
        }

        if (className != null && packegeName != null) {
            intent.setClassName(packegeName, className);
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("image/jpg");
            intent.putExtra(Intent.EXTRA_STREAM, selectedImageUri);
            startActivity(intent);
        } else {
            //Instagramをインストールしてもらう
            Toast.makeText(this, "Instagramをインストールしてください", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void backToTitle(View v) {
        Intent intent = new Intent(this, IndexActivity.class);
        startActivity(intent);
    }

}
