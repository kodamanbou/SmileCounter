package com.lifeistech.android.SmileCounter;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lifeistech.android.SmileCounter.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class IndexActivity extends AppCompatActivity {

    private final String PREF_KEY = "DataSave";
    private final String FILE_NAME = "FileName";

    // 記録の表示及びその他の機能追加
    int score;
    private TextView textView;
    private Button button;
    private ImageView imageView;
    private ImageView galleryView;
    private Button lookButton;
    private Button clearButton;
    private GestureDetector gestureDetector;
    private TextView labelTextView;
    private TextView numberTextView;

    private ListIterator<String> iterator;
    private ArrayList<String> fileNames;

    private boolean isLooking = false;
    private boolean isChanged = false;

    //フリックを検知するための基準
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_MIN_DISTANCE = 50;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    static final int REQUEST_CAPTURE_IMAGE = 100;

    static Uri selectedImageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);

        textView = (TextView) findViewById(R.id.score);
        button = (Button) findViewById(R.id.takePicture);
        imageView = (ImageView) findViewById(R.id.bestImage);
        lookButton = (Button) findViewById(R.id.lookPicture);
        labelTextView = (TextView) findViewById(R.id.textView);
        numberTextView = (TextView) findViewById(R.id.numberText);

        numberTextView.setTypeface(Typeface.createFromAsset(getAssets(), "JiyunoTsubasa.ttf"));
        textView.setTypeface(Typeface.createFromAsset(getAssets(), "JiyunoTsubasa.ttf"));
        labelTextView.setTypeface(Typeface.createFromAsset(getAssets(), "JiyunoTsubasa.ttf"));

        gestureDetector = new GestureDetector(this, gestureListener);

        //FaceDetectActivityからスコアを取得する
        Intent intent = getIntent();
        score = intent.getIntExtra("SCORE", 0);
        String bitPath = intent.getStringExtra("SCORE_IMAGE");
        Bitmap bitmap = null;

        if (bitPath != null) {
            try {
                File file = new File(getFilesDir(), "SmileCounter");
                File imageFile = new File(file, bitPath);
                BitmapFactory.Options options = new BitmapFactory.Options();
                InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(this, "com.lifeistech.android.SmileCounter" + ".fileprovider", imageFile));
                bitmap = BitmapFactory.decodeStream(stream);
                bitmap = resizeBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SharedPreferences data = getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        int highestScore = data.getInt("Score", 0);
        String bitString = data.getString("ScoreImage", "");

        if (highestScore < score) {
            SharedPreferences.Editor editor = data.edit();
            editor.putInt("Score", score);
            highestScore = score;
            isChanged = true;
            editor.putString("ScoreImage", bitPath);
            editor.apply();

            if (bitPath != null) {
                imageView.setImageBitmap(bitmap);
            }
        } else if (highestScore == score && !(isChanged)) {
            if (!bitString.equals("")) {
                bitPath = data.getString("ScoreImage", "");
                File file = new File(getFilesDir(), "SmileCounter");
                File imageFile = new File(file, bitPath);

                try {
                    InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(this, "com.lifeistech.android.SmileCounter" + ".fileprovider", imageFile));
                    bitmap = BitmapFactory.decodeStream(stream);
                    bitmap = resizeBitmap(bitmap);
                } catch (Exception ee) {
                    ee.printStackTrace();
                }

                imageView.setImageBitmap(bitmap);
            } else {
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.droidkun);
                imageView.setImageBitmap(bitmap);
            }
        } else if (highestScore > score) {

            File file = new File(getFilesDir(), "SmileCounter");
            File imageFile = new File(file, bitString);

            try {
                InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(this, "com.lifeistech.android.SmileCounter" + ".fileprovider", imageFile));
                imageView.setImageBitmap(resizeBitmap(BitmapFactory.decodeStream(stream)));
            } catch (Exception ee) {
                ee.printStackTrace();
            }

            SharedPreferences.Editor editor = data.edit();
            editor.putString("ScoreImage", bitString);
            editor.apply();
        }

        textView.setText(String.valueOf(highestScore) + "pt");

        if (highestScore >= 20 && highestScore < 30) {
            textView.setTextColor(Color.BLUE);
        } else if (highestScore >= 30 && highestScore < 40) {
            textView.setTextColor(Color.GREEN);
        } else if (highestScore >= 40 && highestScore < 50) {
            textView.setTextColor(Color.YELLOW);
        } else if (highestScore >= 50 && highestScore < 60) {
            textView.setTextColor(Color.RED);
        }

    }

    public void picture(View v) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //FileProvider で保存・参照する
        File file = new File(getFilesDir(), "SmileCounter");
        File imageFile = new File(file, "camera_test.jpg");

        if (!file.exists()) {
            file.mkdirs();
        }

        selectedImageUri = FileProvider.getUriForFile(this, "com.lifeistech.android.SmileCounter" + ".fileprovider", imageFile);

        Log.d("selectedImageUri", selectedImageUri.getPath());

        intent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImageUri);
        startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);

    }

    public void look(View v) {

        isLooking = true;

        File file = new File(getFilesDir(), "SmileCounter");
        File imageFile = new File(file, "camera_test.jpg");

        Uri testUri;

        fileNames = new ArrayList<>();

        SharedPreferences preferences = getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        String names = preferences.getString(FILE_NAME, null);

        if (names != null) {
            String[] files = names.split(",");
            for (String s : files) {
                fileNames.add(s);
                Log.d("JPEG_DIRECTORY", s);
            }
        }

        if (fileNames.size() > 0) {

            iterator = fileNames.listIterator();

            galleryView = (ImageView) findViewById(R.id.gallery);
            clearButton = (Button) findViewById(R.id.clear_button);

            if (iterator.hasNext()) {

                Bitmap bitmap = null;

                try {
                    InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(this, "com.lifeistech.android.SmileCounter" + ".fileprovider", new File(file, iterator.next())));
                    bitmap = BitmapFactory.decodeStream(new BufferedInputStream(stream));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                galleryView.setImageBitmap(bitmap);
                numberTextView.setText(String.valueOf(iterator.nextIndex()) + "/" + fileNames.size());
            }

            textView.setVisibility(View.GONE);
            labelTextView.setVisibility(View.GONE);
            button.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
            lookButton.setVisibility(View.GONE);
            clearButton.setVisibility(View.VISIBLE);
            galleryView.setVisibility(View.VISIBLE);
            numberTextView.setVisibility(View.VISIBLE);

            Toast.makeText(this, "スワイプして写真を見る", Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(this, "写真がありません！", Toast.LENGTH_LONG).show();
            isLooking = false;
        }
    }

    public void clear(View v) {
        galleryView.setVisibility(View.GONE);
        textView.setVisibility(View.VISIBLE);
        labelTextView.setVisibility(View.VISIBLE);
        button.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.VISIBLE);
        lookButton.setVisibility(View.VISIBLE);
        clearButton.setVisibility(View.GONE);
        numberTextView.setVisibility(View.GONE);
    }

    private final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {

                if (!isLooking) {
                    return false;
                }

                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                    return false;
                } else if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    // 右から左
                    if (iterator.hasNext()) {

                        Bitmap bitmap = null;
                        File file = new File(getFilesDir(), "SmileCounter");

                        try {
                            InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(getApplicationContext(), "com.lifeistech.android.SmileCounter" + ".fileprovider", new File(file, iterator.next())));
                            bitmap = BitmapFactory.decodeStream(new BufferedInputStream(stream));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        numberTextView.setText(String.valueOf(iterator.nextIndex() + 1) + "/" + fileNames.size());
                        galleryView.setImageBitmap(bitmap);
                    } else {
                        iterator = fileNames.listIterator();
                        Bitmap bitmap = null;
                        File file = new File(getFilesDir(), "SmileCounter");

                        try {
                            InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(getApplicationContext(), "com.lifeistech.android.SmileCounter" + ".fileprovider", new File(file, iterator.next())));
                            bitmap = BitmapFactory.decodeStream(new BufferedInputStream(stream));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        numberTextView.setText(String.valueOf(iterator.nextIndex() + 1) + "/" + fileNames.size());
                        galleryView.setImageBitmap(bitmap);
                    }
                    return true;
                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    // 左から右
                    if (iterator.hasPrevious()) {

                        Bitmap bitmap = null;
                        File file = new File(getFilesDir(), "SmileCounter");

                        try {
                            InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(getApplicationContext(), "com.lifeistech.android.SmileCounter" + ".fileprovider", new File(file, iterator.previous())));
                            bitmap = BitmapFactory.decodeStream(new BufferedInputStream(stream));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        numberTextView.setText(String.valueOf(iterator.previousIndex() + 1) + "/" + fileNames.size());
                        galleryView.setImageBitmap(bitmap);
                    } else {
                        iterator = fileNames.listIterator(fileNames.size());
                        Bitmap bitmap = null;
                        File file = new File(getFilesDir(), "SmileCounter");

                        try {
                            InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(getApplicationContext(), "com.lifeistech.android.SmileCounter" + ".fileprovider", new File(file, iterator.previous())));
                            bitmap = BitmapFactory.decodeStream(new BufferedInputStream(stream));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        numberTextView.setText(String.valueOf(iterator.previousIndex() + 1) + "/" + fileNames.size());
                        galleryView.setImageBitmap(bitmap);
                    }
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_CAPTURE_IMAGE == requestCode
                && resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(this, FaceDetectActivity.class);
            startActivity(intent);
        }
    }

    private Bitmap resizeBitmap(Bitmap src) {

        final int dispWidth = 480,
                  dispHight = 800;

        int srcWidth = src.getWidth(); // 元画像のwidth
        int srcHeight = src.getHeight(); // 元画像のheight

        Display display = getWindowManager().getDefaultDisplay();
        Point p = new Point();
        display.getSize(p);

        Log.d("BITMAP_SIZE", "w : " + String.valueOf(srcWidth) + "h : " + String.valueOf(srcHeight));
        Log.d("DISPLAY_SIZE", "w : " + String.valueOf(p.x) + " " + "h : " + String.valueOf(p.y));

        // 画面サイズを取得する
        Matrix matrix = new Matrix();
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float screenWidth = (float) metrics.widthPixels;
        float screenHeight = (float) metrics.heightPixels;
        Log.d("SCREEN_SIZE", "screenWidth = " + String.valueOf(screenWidth)
                + " px, screenHeight = " + String.valueOf(screenHeight) + " px");

        float widthScale = screenWidth * 0.5f / srcWidth;
        float heightScale = screenHeight * 0.5f / srcHeight;

        if (widthScale > heightScale) {
            matrix.postScale(heightScale, heightScale);
        } else {
            matrix.postScale(widthScale, widthScale);
        }
        // リサイズ
        Bitmap dst = Bitmap.createBitmap(src, 0, 0, srcWidth, srcHeight, matrix, true);
        int dstWidth = dst.getWidth(); // 変更後画像のwidth
        int dstHeight = dst.getHeight(); // 変更後画像のheight

        Log.d("DST_BITMAP_SIZE", "w : " + String.valueOf(dstWidth) + ", h : " + String.valueOf(dstHeight));

        src = null;
        return dst;
    }

}
