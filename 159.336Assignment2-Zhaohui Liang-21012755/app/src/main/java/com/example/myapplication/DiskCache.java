package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DiskCache {

    private Context context;

    public DiskCache(Context context) {
        this.context = context;
    }

    // 将图片保存到磁盘缓存
    public void saveBitmapToDiskCache(String fileName, Bitmap bitmap) {
        try {
            File cacheDir = context.getCacheDir(); // 获取缓存目录
            File imageFile = new File(cacheDir, fileName);

            // 将图片保存到文件
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 从磁盘缓存加载图片
    public Bitmap loadBitmapFromDiskCache(String fileName) {
        try {
            File cacheDir = context.getCacheDir(); // 获取缓存目录
            File imageFile = new File(cacheDir, fileName);

            // 从文件加载图片
            if (imageFile.exists()) {
                return BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

