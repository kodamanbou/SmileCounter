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
import android.media.ExifInterface;
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
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

public class IndexActivity extends AppCompatActivity {

    private final String PREF_KEY = "DataSave";
    private final String FILE_NAME = "FileName";
    private final String DAILY_BONUS = "DailyBonus";
    private final String BONUS = "Bonus";
    private final String TOTAL_SCORE = "TotalScore";

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
    private TextView currentScore;
    private TextView totalTextView;

    private ListIterator<String> iterator;
    private ArrayList<String> fileNames;
    private ArrayList<Integer> scores;
    private ListIterator<Integer> integerListIterator;

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
        totalTextView = (TextView) findViewById(R.id.totalText);

        numberTextView.setTypeface(Typeface.createFromAsset(getAssets(), "JiyunoTsubasa.ttf"));
        textView.setTypeface(Typeface.createFromAsset(getAssets(), "JiyunoTsubasa.ttf"));
        labelTextView.setTypeface(Typeface.createFromAsset(getAssets(), "JiyunoTsubasa.ttf"));
        totalTextView.setTypeface(Typeface.createFromAsset(getAssets(), "JiyunoTsubasa.ttf"));

        gestureDetector = new GestureDetector(this, gestureListener);

        //FaceDetectActivityからスコアを取得する
        Intent intent = getIntent();
        score = intent.getIntExtra("SCORE", 0);
        String bitPath = intent.getStringExtra("SCORE_IMAGE");
        Bitmap bitmap = null;

        if (bitPath != null) {

            if (Build.VERSION.SDK_INT >= 19) {
                try {
                    File file = new File(getFilesDir(), "SmileCounter");
                    File imageFile = new File(file, bitPath);
                    InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(this, "com.lifeistech.android.SmileCounter" + ".fileprovider", imageFile));
                    bitmap = BitmapFactory.decodeStream(stream);
                    bitmap = resizeBitmap(bitmap, 0.55f, new Matrix());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                File file = new File(Environment.getExternalStorageDirectory(), "SmileCounter");
                File imageFile = new File(file, bitPath);
                //TODO ビットマップのサイズが変更できないバグ
                bitmap = resizeBitmap(BitmapFactory.decodeFile(imageFile.getPath()), 0.8f, getRotatedMatrix(imageFile.getPath()));
                bitmap = resizeBitmap(bitmap, 0.6f, new Matrix());
            }

        }

        SharedPreferences data = getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        int highestScore = data.getInt("Score", 0);
        String bitString = data.getString("ScoreImage", "");

        int total = data.getInt(TOTAL_SCORE, -1);

        if (total == -1) {
            SharedPreferences.Editor editor = data.edit();
            editor.putInt(TOTAL_SCORE, score);
            editor.apply();
            textView.setText(String.valueOf(score) + "pt");
        } else {
            SharedPreferences.Editor editor = data.edit();
            editor.putInt(TOTAL_SCORE, total + score);
            editor.apply();
            textView.setText(String.valueOf(total + score) + "pt");
        }

        Date today = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.JAPAN);
        String previous = data.getString(DAILY_BONUS, null);

        if (previous != null) {

            try {

                Date before = sdf.parse(previous);

                int bonus = data.getInt(BONUS, 0);
                bonus++;

                Calendar now = Calendar.getInstance();
                now.setTime(today);
                int nowYear = now.get(Calendar.YEAR);
                int nowMonth = now.get(Calendar.MONTH);
                int nowDate = now.get(Calendar.DATE);

                Calendar past = Calendar.getInstance();
                past.setTime(before);
                int pastYear = past.get(Calendar.YEAR);
                int pastMonth = past.get(Calendar.MONTH);
                int pastDate = past.get(Calendar.DATE);

                if (nowYear - pastYear == 1 && nowMonth - pastMonth == 1 && nowDate - pastDate == 1) {
                    if (score != 0) {
                        if (bonus >= 3 && bonus < 7) {
                            score += 5;
                        } else if (bonus >= 7 && bonus < 10) {
                            score += 7;
                        } else if (bonus >= 14) {
                            score += 15;
                        }
                    }

                    SharedPreferences.Editor editor = data.edit();
                    editor.putInt(BONUS, bonus);
                    editor.putString(DAILY_BONUS, sdf.format(today));
                    editor.apply();
                } else {
                    SharedPreferences.Editor editor = data.edit();
                    editor.putInt(BONUS, 0);
                    editor.putString(DAILY_BONUS, sdf.format(today));
                    editor.apply();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            SharedPreferences.Editor editor = data.edit();
            editor.putString(DAILY_BONUS, sdf.format(today));
            editor.apply();
        }

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

                if (Build.VERSION.SDK_INT >= 19) {
                    try {
                        InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(this, "com.lifeistech.android.SmileCounter" + ".fileprovider", imageFile));
                        bitmap = BitmapFactory.decodeStream(stream);
                        bitmap = resizeBitmap(bitmap, 0.5f, new Matrix());
                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }
                } else {
                    File base = new File(Environment.getExternalStorageDirectory(), "SmileCounter");
                    File image = new File(base, bitPath);
                    bitmap = resizeBitmap(BitmapFactory.decodeFile(image.getPath()), 0.5f, getRotatedMatrix(image.getPath()));
                }

                imageView.setImageBitmap(bitmap);
            } else {
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.droidkun);
                bitmap = resizeBitmap(bitmap, 0.55f, new Matrix());
                imageView.setImageBitmap(bitmap);
            }
        } else if (highestScore > score) {

            File file = new File(getFilesDir(), "SmileCounter");
            File imageFile = new File(file, bitString);

            if (Build.VERSION.SDK_INT >= 19) {
                try {
                    InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(this, "com.lifeistech.android.SmileCounter" + ".fileprovider", imageFile));
                    imageView.setImageBitmap(resizeBitmap(BitmapFactory.decodeStream(stream), 0.5f, new Matrix()));
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            } else {
                File base = new File(Environment.getExternalStorageDirectory(), "SmileCounter");
                File image = new File(base, bitString);
                bitmap = resizeBitmap(BitmapFactory.decodeFile(image.getPath()), 0.5f, getRotatedMatrix(image.getPath()));
                imageView.setImageBitmap(bitmap);
            }

            SharedPreferences.Editor editor = data.edit();
            editor.putString("ScoreImage", bitString);
            editor.apply();
        }

        if (total + score >= 20 && total + score < 30) {
            textView.setTextColor(Color.parseColor("#00e5ff"));
        } else if (total + score >= 30 && total + score < 50) {
            textView.setTextColor(Color.parseColor("#388e3c"));
        } else if (total + score >= 50 && total + score < 100) {
            textView.setTextColor(Color.parseColor("#ffa726"));
        } else if (total + score >= 100 && total + score < 200) {
            textView.setTextColor(Color.parseColor("#d500f9"));
        } else if (total + score >= 200) {
            textView.setTextColor(Color.parseColor("#f44336"));
        } else {
            textView.setTextColor(Color.parseColor("#424242"));
        }

    }

    public void picture(View v) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);


        if (Build.VERSION.SDK_INT >= 19) {
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
        } else {
            File base = new File(Environment.getExternalStorageDirectory(), "SmileCounter");
            File image = new File(base, "camera_test.jpg");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
            startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
        }

    }

    public void look(View v) {

        isLooking = true;

        File file = new File(getFilesDir(), "SmileCounter");
        File imageFile = new File(file, "camera_test.jpg");

        Uri testUri;

        fileNames = new ArrayList<>();
        scores = new ArrayList<>();

        SharedPreferences pref = getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        int highestScore = pref.getInt("Score", -1);

        if (highestScore == -1) {
            textView.setText(String.valueOf(0) + "pt");
        } else {
            textView.setText(String.valueOf(highestScore) + "pt");
        }

        if (Build.VERSION.SDK_INT >= 19) {
            SharedPreferences preferences = getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
            String names = preferences.getString(FILE_NAME, null);

            if (names != null) {
                String[] files = names.split(",");
                for (String s : files) {
                    fileNames.add(s);
                    Log.d("JPEG_DIRECTORY", s);
                }
            }
        } else {
            File[] files;

            files = new File(Environment.getExternalStorageDirectory(), "SmileCounter").listFiles();

            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isFile() && files[i].getName().startsWith("smilecounter_")) {
                        fileNames.add(files[i].getName());
                    }
                }
                Log.d("FILES_NULL?", "NON NULL!!!!!");
            } else {
                Log.d("FILES_NULL?", "NULL!!!!!");
            }

        }

        for (String s : fileNames) {
            SharedPreferences data = getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
            scores.add(data.getInt(s, 0));
        }

        if (fileNames.size() > 0) {

            Log.d("FILE_NUMBER", String.valueOf(fileNames.size()));

            iterator = fileNames.listIterator();
            integerListIterator = scores.listIterator();

            galleryView = (ImageView) findViewById(R.id.gallery);
            clearButton = (Button) findViewById(R.id.clear_button);
            currentScore = (TextView) findViewById(R.id.currentScore);
            currentScore.setTypeface(Typeface.createFromAsset(getAssets(), "JiyunoTsubasa.ttf"));

            //対応する写真のポイントを表示する処理

            if (iterator.hasNext()) {

                Bitmap bitmap = null;

                if (Build.VERSION.SDK_INT >= 19) {

                    try {
                        InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(this, "com.lifeistech.android.SmileCounter" + ".fileprovider", new File(file, iterator.next())));
                        bitmap = resizeBitmap(BitmapFactory.decodeStream(new BufferedInputStream(stream)), 0.55f, new Matrix());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    File base = new File(Environment.getExternalStorageDirectory(), "SmileCounter");
                    File image = new File(base, iterator.next());
                    bitmap = resizeBitmap(BitmapFactory.decodeFile(image.getPath()), 0.55f, getRotatedMatrix(image.getPath()));
                }

                int point = integerListIterator.next();

                currentScore.setText(String.valueOf(point) + "pt");
                galleryView.setImageBitmap(bitmap);
                numberTextView.setText(String.valueOf(iterator.nextIndex()) + "/" + fileNames.size());

                if (point >= 20 && point < 30) {
                    currentScore.setTextColor(Color.parseColor("#00e5ff"));
                } else if (point >= 30 && point < 40) {
                    currentScore.setTextColor(Color.parseColor("#388e3c"));
                } else if (point >= 40 && point < 50) {
                    currentScore.setTextColor(Color.parseColor("#ffa726"));
                } else if (point >= 50 && point < 60) {
                    currentScore.setTextColor(Color.parseColor("#d500f9"));
                } else if (point >= 60) {
                    currentScore.setTextColor(Color.parseColor("#f44336"));
                } else {
                    currentScore.setTextColor(Color.parseColor("#424242"));
                }

            }

            textView.setVisibility(View.VISIBLE);
            button.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
            lookButton.setVisibility(View.GONE);
            totalTextView.setVisibility(View.GONE);
            clearButton.setVisibility(View.VISIBLE);
            galleryView.setVisibility(View.VISIBLE);
            numberTextView.setVisibility(View.VISIBLE);
            currentScore.setVisibility(View.VISIBLE);
            labelTextView.setVisibility(View.VISIBLE);

            Toast.makeText(this, "スワイプして写真を見る", Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(this, "写真がありません！", Toast.LENGTH_LONG).show();
            isLooking = false;
        }
    }

    public void clear(View v) {
        galleryView.setVisibility(View.GONE);
        textView.setVisibility(View.VISIBLE);
        labelTextView.setVisibility(View.GONE);
        button.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.VISIBLE);
        lookButton.setVisibility(View.VISIBLE);
        totalTextView.setVisibility(View.VISIBLE);
        clearButton.setVisibility(View.GONE);
        numberTextView.setVisibility(View.GONE);
        currentScore.setVisibility(View.GONE);
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

                        if (Build.VERSION.SDK_INT >= 19) {
                            try {
                                InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(getApplicationContext(), "com.lifeistech.android.SmileCounter" + ".fileprovider", new File(file, iterator.next())));
                                bitmap = resizeBitmap(BitmapFactory.decodeStream(new BufferedInputStream(stream)), 0.55f, new Matrix());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            File base = new File(Environment.getExternalStorageDirectory(), "SmileCounter");
                            File image = new File(base, iterator.next());
                            bitmap = resizeBitmap(BitmapFactory.decodeFile(image.getPath()), 0.55f, getRotatedMatrix(image.getPath()));
                            Log.d("IMAGE_FILE_PATH", image.getPath());
                        }

                        int point = integerListIterator.next();

                        currentScore.setText(String.valueOf(point) + "pt");
                        numberTextView.setText(String.valueOf(iterator.nextIndex()) + "/" + fileNames.size());
                        galleryView.setImageBitmap(bitmap);

                        if (point >= 20 && point < 30) {
                            currentScore.setTextColor(Color.parseColor("#00e5ff"));
                        } else if (point >= 30 && point < 40) {
                            currentScore.setTextColor(Color.parseColor("#388e3c"));
                        } else if (point >= 40 && point < 50) {
                            currentScore.setTextColor(Color.parseColor("#ffa726"));
                        } else if (point >= 50 && point < 60) {
                            currentScore.setTextColor(Color.parseColor("#d500f9"));
                        } else if (point >= 60) {
                            currentScore.setTextColor(Color.parseColor("#f44336"));
                        } else {
                            currentScore.setTextColor(Color.parseColor("#424242"));
                        }

                    } else {
                        Bitmap bitmap = null;
                        File file = new File(getFilesDir(), "SmileCounter");

                        if (Build.VERSION.SDK_INT >= 19) {
                            try {
                                iterator = fileNames.listIterator();
                                integerListIterator = scores.listIterator();
                                InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(getApplicationContext(), "com.lifeistech.android.SmileCounter" + ".fileprovider", new File(file, iterator.next())));
                                bitmap = resizeBitmap(BitmapFactory.decodeStream(new BufferedInputStream(stream)), 0.55f, new Matrix());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            iterator = fileNames.listIterator();
                            integerListIterator = scores.listIterator();
                            File base = new File(Environment.getExternalStorageDirectory(), "SmileCounter");
                            File image = new File(base, iterator.next());
                            bitmap = resizeBitmap(BitmapFactory.decodeFile(image.getPath()), 0.55f, getRotatedMatrix(image.getPath()));
                        }

                        int point = integerListIterator.next();

                        currentScore.setText(String.valueOf(point) + "pt");
                        numberTextView.setText(String.valueOf(iterator.nextIndex()) + "/" + fileNames.size());
                        galleryView.setImageBitmap(bitmap);

                        if (point >= 20 && point < 30) {
                            currentScore.setTextColor(Color.parseColor("#00e5ff"));
                        } else if (point >= 30 && point < 40) {
                            currentScore.setTextColor(Color.parseColor("#388e3c"));
                        } else if (point >= 40 && point < 50) {
                            currentScore.setTextColor(Color.parseColor("#ffa726"));
                        } else if (point >= 50 && point < 60) {
                            currentScore.setTextColor(Color.parseColor("#d500f9"));
                        } else if (point >= 60) {
                            currentScore.setTextColor(Color.parseColor("#f44336"));
                        } else {
                            currentScore.setTextColor(Color.parseColor("#424242"));
                        }

                    }
                    return true;
                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    // 左から右
                    if (iterator.hasPrevious()) {

                        Bitmap bitmap = null;
                        File file = new File(getFilesDir(), "SmileCounter");

                        int point = integerListIterator.previous();

                        currentScore.setText(String.valueOf(point) + "pt");
                        numberTextView.setText(String.valueOf(iterator.previousIndex() + 1) + "/" + fileNames.size());

                        if (point >= 20 && point < 30) {
                            currentScore.setTextColor(Color.parseColor("#00e5ff"));
                        } else if (point >= 30 && point < 40) {
                            currentScore.setTextColor(Color.parseColor("#388e3c"));
                        } else if (point >= 40 && point < 50) {
                            currentScore.setTextColor(Color.parseColor("#ffa726"));
                        } else if (point >= 50 && point < 60) {
                            currentScore.setTextColor(Color.parseColor("#d500f9"));
                        } else if (point >= 60) {
                            currentScore.setTextColor(Color.parseColor("#f44336"));
                        } else {
                            currentScore.setTextColor(Color.parseColor("#424242"));
                        }

                        if (Build.VERSION.SDK_INT >= 19) {
                            try {
                                InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(getApplicationContext(), "com.lifeistech.android.SmileCounter" + ".fileprovider", new File(file, iterator.previous())));
                                bitmap = resizeBitmap(BitmapFactory.decodeStream(new BufferedInputStream(stream)), 0.55f, new Matrix());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            Log.d("ITERATOR_TEST", "Index=" + String.valueOf(iterator.previousIndex()));
                        } else {
                            File base = new File(Environment.getExternalStorageDirectory(), "SmileCounter");
                            File image = new File(base, iterator.previous());
                            bitmap = resizeBitmap(BitmapFactory.decodeFile(image.getPath()), 0.55f, getRotatedMatrix(image.getPath()));
                        }

                        galleryView.setImageBitmap(bitmap);

                    } else {
                        iterator = fileNames.listIterator(fileNames.size());
                        integerListIterator = scores.listIterator(scores.size());
                        Bitmap bitmap = null;
                        File file = new File(getFilesDir(), "SmileCounter");

                        int point = integerListIterator.previous();

                        numberTextView.setText(String.valueOf(iterator.previousIndex() + 1) + "/" + fileNames.size());
                        currentScore.setText(String.valueOf(point) + "pt");

                        if (point >= 20 && point < 30) {
                            currentScore.setTextColor(Color.parseColor("#00e5ff"));
                        } else if (point >= 30 && point < 40) {
                            currentScore.setTextColor(Color.parseColor("#388e3c"));
                        } else if (point >= 40 && point < 50) {
                            currentScore.setTextColor(Color.parseColor("#ffa726"));
                        } else if (point >= 50 && point < 60) {
                            currentScore.setTextColor(Color.parseColor("#d500f9"));
                        } else if (point >= 60) {
                            currentScore.setTextColor(Color.parseColor("#f44336"));
                        } else {
                            currentScore.setTextColor(Color.parseColor("#424242"));
                        }

                        if (Build.VERSION.SDK_INT >= 19) {
                            try {
                                InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(getApplicationContext(), "com.lifeistech.android.SmileCounter" + ".fileprovider", new File(file, iterator.previous())));
                                bitmap = resizeBitmap(BitmapFactory.decodeStream(new BufferedInputStream(stream)), 0.55f, new Matrix());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            Log.d("ITERATOR_TEST", "Index=" + String.valueOf(iterator.previousIndex()));
                        } else {
                            File base = new File(Environment.getExternalStorageDirectory(), "SmileCounter");
                            File image = new File(base, iterator.previous());
                            bitmap = resizeBitmap(BitmapFactory.decodeFile(image.getPath()), 0.55f, getRotatedMatrix(image.getPath()));
                        }

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

    private Bitmap resizeBitmap(Bitmap src, float scale, Matrix matrix) {

        final int dispWidth = 480,
                dispHight = 800;

        Matrix mat = new Matrix(matrix);

        int srcWidth = src.getWidth(); // 元画像のwidth
        int srcHeight = src.getHeight(); // 元画像のheight

        Display display = getWindowManager().getDefaultDisplay();
        Point p = new Point();
        display.getSize(p);

        Log.d("BITMAP_SIZE", "w : " + String.valueOf(srcWidth) + "h : " + String.valueOf(srcHeight));
        Log.d("DISPLAY_SIZE", "w : " + String.valueOf(p.x) + " " + "h : " + String.valueOf(p.y));

        // 画面サイズを取得する
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float screenWidth = (float) metrics.widthPixels;
        float screenHeight = (float) metrics.heightPixels;
        Log.d("SCREEN_SIZE", "screenWidth = " + String.valueOf(screenWidth)
                + " px, screenHeight = " + String.valueOf(screenHeight) + " px");

        float widthScale = screenWidth * scale / srcWidth;
        float heightScale = screenHeight * scale / srcHeight;

        if (widthScale > heightScale) {
            mat.postScale(heightScale, heightScale);
        } else {
            mat.postScale(widthScale, widthScale);
        }
        // リサイズ
        Bitmap dst = Bitmap.createBitmap(src, 0, 0, srcWidth, srcHeight, mat, true);
        int dstWidth = dst.getWidth(); // 変更後画像のwidth
        int dstHeight = dst.getHeight(); // 変更後画像のheight

        Log.d("DST_BITMAP_SIZE", "w : " + String.valueOf(dstWidth) + ", h : " + String.valueOf(dstHeight));

        src = null;
        return dst;
    }

    private Matrix getRotatedMatrix(String path) {
        ExifInterface exifInterface = null;
        Matrix matrix = new Matrix();

        try {
            exifInterface = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
            return matrix;
        }

        // 画像の向きを取得
        int orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        // 画像を回転させる処理をマトリクスに追加
        switch (orientation) {
            case ExifInterface.ORIENTATION_UNDEFINED:
                break;
            case ExifInterface.ORIENTATION_NORMAL:
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                // 水平方向にリフレクト
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                // 180度回転
                matrix.postRotate(180f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                // 垂直方向にリフレクト
                matrix.postScale(1f, -1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                // 反時計回り90度回転
                matrix.postRotate(90f);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                // 時計回り90度回転し、垂直方向にリフレクト
                matrix.postRotate(-90f);
                matrix.postScale(1f, -1f);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                // 反時計回り90度回転し、垂直方向にリフレクト
                matrix.postRotate(90f);
                matrix.postScale(1f, -1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                // 反時計回りに270度回転（時計回りに90度回転）
                matrix.postRotate(-90f);
                break;
        }
        return matrix;
    }


}
