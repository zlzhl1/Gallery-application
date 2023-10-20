package com.example.myapplication;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ScaleGestureDetector;
import android.view.MotionEvent; // 添加这行导入语句
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.InputStream;

public class SinglePhotoActivity extends AppCompatActivity {
    private ImageView imageView;
    private Button exitButton;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_photo);
        imageView = findViewById(R.id.single_photo_image_view);
        exitButton = findViewById(R.id.exit_button); // 获取退出按钮

        // 获取传递的照片信息
        Intent intent = getIntent();
        long photoId = intent.getLongExtra("photo_id", -1);
        int orientation = intent.getIntExtra("orientation", 0);

        // 根据照片ID加载原始照片并旋转
        Bitmap photoBitmap = loadPhotoWithOrientation(photoId, orientation);
        imageView.setImageBitmap(photoBitmap);
        // 设置退出按钮的点击监听器
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 点击退出按钮时关闭当前活动
                finish();
            }
        });

        // 初始化缩放手势检测器
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                // 设置缩放限制
                scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 2.0f));
                imageView.setScaleX(scaleFactor);
                imageView.setScaleY(scaleFactor);
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 将触摸事件传递给缩放手势检测器
        scaleGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private Bitmap loadPhotoWithOrientation(long photoId, int orientation) {
        Uri photoUri = Uri.withAppendedPath(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                String.valueOf(photoId)
        );

        try (InputStream inputStream = getContentResolver().openInputStream(photoUri)) {
            // 使用 BitmapFactory 加载原始照片
            Bitmap fullSizeBitmap = BitmapFactory.decodeStream(inputStream);

            // 根据照片的方向旋转图像
            return rotateBitmap(fullSizeBitmap, orientation);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        // 根据照片的方向旋转图像
        if (orientation != 0) {
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postRotate(orientation);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        return bitmap;
    }
}
