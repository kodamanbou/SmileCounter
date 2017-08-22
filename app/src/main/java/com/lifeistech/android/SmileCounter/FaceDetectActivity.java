package com.lifeistech.android.SmileCounter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
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
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FaceDetectActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView textView;
    int score = 0;
    Bitmap bitmap;

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

        File file = new File(Environment.getExternalStorageDirectory() + "/SmileCounter" + "/camera_test.jpg");

        String bitUriString = FileProvider.getUriForFile(this, "com.lifeistech.android.SmileCounter" + ".fileprovider", file).getPath();

        Log.d("Bitmap_Test", bitUriString);

        bitmap = BitmapFactory.decodeFile(bitUriString);

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

        for(int i=0; i<faces.size(); i++) {
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
                if (j >= 3) {
                    score *= 2;
                }
            } else if (smilingProbability > 0.5f && smilingProbability <= 0.7f) {
                score = score + 7;
                j++;
                if (j >= 3) {
                    score *= 2;
                }
            } else if (smilingProbability > 0.7f) {
                score = score + 10;
                j++;
                if (j >= 3) {
                    score *= 2;
                }
            }

            tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);
        }
        imageView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
        textView.setText(String.valueOf(score) + "pt");

    }

    public void backToTitle(View v) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes = baos.toByteArray();

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPAN);
        String s = sdf.format(date);

        String bitmapPath = "";
        try {
            bitmapPath = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", new File(Environment.getExternalStorageDirectory() + "/SmileCounter/")).getPath();

            FileOutputStream fos = null;
            fos = new FileOutputStream(bitmapPath + "/smilecounter_" + s + ".jpg");
            fos.write(bytes);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(this, IndexActivity.class);
        intent.putExtra("SCORE", score);
        intent.putExtra("SCORE_IMAGE", bitmapPath + "/smilecounter_" + s + ".jpg");
        startActivity(intent);
    }
}
