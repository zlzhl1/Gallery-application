package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    private static final int REQUEST_PERMISSION = 1;
    private PhotoAdapter photoAdapter;
    private float scaleFactor = 1.0f; // 初始缩放因子
    private float maxScaleFactor = 2.0f; // 设置一个合适的最大缩放因子值
    private float minScaleFactor = 0.5f; // 设置一个合适的初始值
    ScaleGestureDetector scaleGestureDetector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 检查和请求权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        } else {
            // 权限已经被授予，初始化界面
            initializeUI();
        }

        // 在缩放手势检测器中监听缩放操作，并更新缩放因子
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureListener(this));
    }

    // 在 ScaleGestureListener 中的 onScale 方法中更新缩放因子
    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private MainActivity mainActivity;

        public ScaleGestureListener(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            // 设置缩放的最大和最小限制
            scaleFactor = Math.max(minScaleFactor, Math.min(scaleFactor, maxScaleFactor));

            // 将缩放因子传递给适配器并更新列数
            mainActivity.updateScaleFactor(scaleFactor);

            return true;
        }
    }

    // 更新缩放因子的方法
    private void updateScaleFactor(float newScaleFactor) {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);

        scaleFactor = Math.max(minScaleFactor, Math.min(newScaleFactor, maxScaleFactor));
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int numberOfColumns = calculateNumberOfColumns(screenWidth, scaleFactor);
        photoAdapter.updateScaleFactor(scaleFactor);
        photoAdapter.notifyDataSetChanged();
        GridLayoutManager layoutManager = new GridLayoutManager(this, numberOfColumns);
        recyclerView.setLayoutManager(layoutManager);
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，初始化界面
                initializeUI();
            } else {
                // 权限被拒绝，显示提示
                Toast.makeText(this, "需要读取照片权限才能继续使用应用", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeUI() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        int screenWidth = this.getResources().getDisplayMetrics().widthPixels;
        // 设置 RecyclerView 的布局管理器为网格布局
        int numberOfColumns = calculateNumberOfColumns(screenWidth, scaleFactor);
        GridLayoutManager layoutManager = new GridLayoutManager(this, numberOfColumns);
        recyclerView.setLayoutManager(layoutManager);

        // 从媒体存储获取照片数据
        List<Photo> photos = loadPhotos();

        // 创建并设置适配器
        photoAdapter = new PhotoAdapter(this, photos);
        recyclerView.setAdapter(photoAdapter);

        // 设置点击监听器以在单独的活动中打开照片
        photoAdapter.setOnItemClickListener((view, position) -> {
            Photo selectedPhoto = photoAdapter.getItem(position);
            if (selectedPhoto != null) {
                openPhoto(selectedPhoto);
            }
        });
    }

    private List<Photo> loadPhotos() {
        List<Photo> photos = new ArrayList<>();

        // 查询媒体存储以获取照片信息
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.ORIENTATION,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
        };

        // 获取照片数据的游标
        Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") long photoId = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                @SuppressLint("Range") int orientation = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION));
                @SuppressLint("Range") int width = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.WIDTH));
                @SuppressLint("Range") int height = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT));

                // 创建 Photo 对象并添加到列表
                Photo photo = new Photo(photoId, orientation, width, height);
                photos.add(photo);
            }
            cursor.close();
        }

        return photos;
    }

    private void openPhoto(Photo photo) {
        // 打开单独的活动以显示照片
        Intent intent = new Intent(this, SinglePhotoActivity.class);
        intent.putExtra("photo_id", photo.getId());
        intent.putExtra("orientation", photo.getOrientation());
        intent.putExtra("width", photo.getWidth());
        intent.putExtra("height", photo.getHeight());
        startActivity(intent);
    }

    public int calculateNumberOfColumns(int screenWidth, float scaleFactor) {
        int minColumnWidth = this.getResources().getDimensionPixelSize(R.dimen.min_column_width); // 定义最小列宽
        int maxColumns = screenWidth / minColumnWidth; // 计算最大列数

        // 根据缩放因子调整列数
        int adjustedColumns = (int) (maxColumns * scaleFactor);

        // 确保列数至少为1
        return Math.max(1, adjustedColumns);
    }

}
