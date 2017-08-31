package com.lifeistech.android.SmileCounter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.lifeistech.android.SmileCounter.R;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class FaceDetectActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView textView;
    private Button shareButton;
    int score = 0;
    Bitmap bitmap;

    private final String PREF_KEY = "DataSave";
    private final String FILE_NAME = "FileName";

    private static ArrayList<String> files = new ArrayList<>();

    private String thisFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detect);

        imageView = (ImageView) findViewById(R.id.detectImage);
        textView = (TextView) findViewById(R.id.point);

        Paint myRectPaint = new Paint();
        myRectPaint.setStrokeWidth(5);
        myRectPaint.setColor(Color.RED);
        myRectPaint.setStyle(Paint.Style.STROKE);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;

        //読み込んだ画像をビットマップに変換する------FileProvider を使う

        File file = new File(getFilesDir(), "SmileCounter");
        File imageFile = new File(file, "camera_test.jpg");

        if (Build.VERSION.SDK_INT >= 19) {
            try {
                InputStream stream = getContentResolver().openInputStream(FileProvider.getUriForFile(this, "com.lifeistech.android.SmileCounter" + ".fileprovider", imageFile));
                bitmap = resizeBitmap(BitmapFactory.decodeStream(new BufferedInputStream(stream)), 0.6f, new Matrix());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            File base = new File(Environment.getExternalStorageDirectory(), "SmileCounter");
            File image = new File(base, "camera_test.jpg");
            bitmap = resizeBitmap(BitmapFactory.decodeFile(image.getPath()), 0.6f, getRotatedMatrix(image.getPath()));
        }

        if (bitmap == null) {
            Log.d("Bitmap_null", "NULL!!");
        } else {
            Log.d("Bitmap_null", "NON NULL!!");
        }

        Bitmap tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(tempBitmap);
        tempCanvas.drawBitmap(bitmap, 0, 0, null);

        FaceDetector detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Face> faces = detector.detect(frame);

        for (int i = 0; i < faces.size(); i++) {
            int j = 0;
            Face thisFace = faces.valueAt(i);
            float x1 = thisFace.getPosition().x;
            float y1 = thisFace.getPosition().y;
            float x2 = x1 + thisFace.getWidth();
            float y2 = y1 + thisFace.getHeight();
            //笑顔かどうかを示す小数値
            float smilingProbability = thisFace.getIsSmilingProbability();
            //TODO 笑顔係数に応じてスコアを加算
            if (smilingProbability > 0.3f && smilingProbability <= 0.5f) {
                score = score + 5;
                j++;
            } else if (smilingProbability > 0.5f && smilingProbability <= 0.7f) {
                score = score + 7;
                j++;
            } else if (smilingProbability > 0.7f) {
                score = score + 10;
                j++;
            }

            if (j >= 3) {
                score += score / 2;
            }

            tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);
        }

        imageView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
        textView.setText(String.valueOf(score) + "pt");

        if (score >= 20 && score < 30) {
            textView.setTextColor(Color.parseColor("#00e5ff"));
        } else if (score >= 30 && score < 40) {
            textView.setTextColor(Color.parseColor("#388e3c"));
        } else if (score >= 40 && score < 50) {
            textView.setTextColor(Color.parseColor("#ffa726"));
        } else if (score >= 50 && score < 60) {
            textView.setTextColor(Color.parseColor("#d500f9"));
        } else if (score >= 60) {
            textView.setTextColor(Color.parseColor("#f44336"));
        } else {
            textView.setTextColor(Color.parseColor("#424242"));
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes = baos.toByteArray();

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPAN);
        String s = sdf.format(date);

        String bitmapPath = "";

        if (Build.VERSION.SDK_INT >= 19) {
            try {
                File file1 = new File(getFilesDir(), "SmileCounter");
                File imageFile1 = new File(file1, "smilecounter_" + s + ".jpg");
                bitmapPath = FileProvider.getUriForFile(this, "com.lifeistech.android.SmileCounter" + ".fileprovider", imageFile1).getPath();

                OutputStream stream = null;
                stream = getContentResolver().openOutputStream(FileProvider.getUriForFile(this, "com.lifeistech.android.SmileCounter" + ".fileprovider", imageFile1));
                stream.write(bytes);
                stream.flush();
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            File file1 = new File(Environment.getExternalStorageDirectory(), "SmileCounter");
            File image1 = new File(file1, "smilecounter_" + s + ".jpg");

            try {
                FileOutputStream fos = new FileOutputStream(image1);
                fos.write(bytes);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        files.add("smilecounter_" + s + ".jpg");
        thisFile = "smilecounter_" + s + ".jpg";

        SharedPreferences data = getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = data.edit();
        editor.putInt("smilecounter_" + s + ".jpg", score);

        String text = data.getString(FILE_NAME, null);

        if (text != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(text).append(",").append(files.get(files.size() - 1));
            text = sb.toString();
            editor.putString(FILE_NAME, text);
            editor.putInt(files.get(files.size() - 1), score);
        } else {
            editor.putString(FILE_NAME, files.get(files.size() - 1));
            editor.putInt(files.get(files.size() - 1), score);
        }

        editor.apply();

    }

    public void backToTitle(View v) {

        Intent intent = new Intent(this, IndexActivity.class);
        intent.putExtra("SCORE", score);
        intent.putExtra("SCORE_IMAGE", thisFile);
        startActivity(intent);
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

    private Bitmap resizeBitmap(Bitmap src, float scale, Matrix matrix) {

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
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float screenWidth = (float) metrics.widthPixels;
        float screenHeight = (float) metrics.heightPixels;
        Log.d("SCREEN_SIZE", "screenWidth = " + String.valueOf(screenWidth)
                + " px, screenHeight = " + String.valueOf(screenHeight) + " px");

        float widthScale = screenWidth * scale / srcWidth;
        float heightScale = screenHeight * scale / srcHeight;

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

    public void share(View v) {

        Intent intent = new Intent(this, ShareActivity.class);
        intent.putExtra("FileName", thisFile);
        startActivity(intent);

    }

}
