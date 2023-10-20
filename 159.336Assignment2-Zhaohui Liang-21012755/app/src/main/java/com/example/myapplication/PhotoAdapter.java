package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>{

    private Context context;
    private List<Photo> photos;
    private OnItemClickListener onItemClickListener;
    private DiskCache diskCache;
    private LruCache<String, Bitmap> memoryCache;
    private float scaleFactor = 1.0f; // 默认缩放因子



    public PhotoAdapter(Context context, List<Photo> photos) {
        this.context = context;
        this.photos = photos;
        this.diskCache = new DiskCache(context);

        // 初始化内存缓存，设置缓存大小
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8; // 使用最大内存的1/8作为缓存大小
        memoryCache = new LruCache<>(cacheSize);
    }

    public void updateScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
        notifyDataSetChanged(); // 通知 RecyclerView 更新数据
    }
    public int calculateNumberOfColumns(int screenWidth, float scaleFactor) {
        int minColumnWidth = context.getResources().getDimensionPixelSize(R.dimen.min_column_width); // 定义最小列宽
        int baseColumns = screenWidth / minColumnWidth; // 使用基本列数

        // 根据缩放因子调整列数
        int adjustedColumns = (int) (baseColumns * scaleFactor);

        // 确保列数至少为1
        return Math.max(1, adjustedColumns);
    }


    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.grid_item_layout, parent, false);
        return new PhotoViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Photo photo = photos.get(position);
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int columns = calculateNumberOfColumns(screenWidth,scaleFactor); // 计算列数
        int imageWidth = (int) (screenWidth / columns * scaleFactor); // 计算每个图片的宽度

        ViewGroup.LayoutParams layoutParams = holder.photoImageView.getLayoutParams();
        layoutParams.width = imageWidth;
        layoutParams.height = imageWidth; // 确保宽高相等，保持图片比例

        holder.photoImageView.setLayoutParams(layoutParams);

        // 尝试从内存缓存加载缩略图
        Bitmap thumbnailFromMemory = memoryCache.get(String.valueOf(photo.getId()));

        if (thumbnailFromMemory != null) {
            // 如果内存缓存中存在缩略图，直接显示
            holder.photoImageView.setImageBitmap(thumbnailFromMemory);
        } else {
            // 尝试从磁盘缓存加载缩略图
            Bitmap thumbnailFromCache = diskCache.loadBitmapFromDiskCache(photo.getId() + ".png");

            if (thumbnailFromCache != null) {
                // 如果磁盘缓存中存在缩略图，保存到内存缓存并显示
                memoryCache.put(String.valueOf(photo.getId()), thumbnailFromCache);
                holder.photoImageView.setImageBitmap(thumbnailFromCache);
            } else {
                // 缓存中不存在缩略图，从资源加载并保存到缓存
                Bitmap thumbnail = loadThumbnail(photo.getId(), photo.getOrientation());

                if (thumbnail != null) {
                    // 将缩略图保存到内存缓存
                    memoryCache.put(String.valueOf(photo.getId()), thumbnail);

                    holder.photoImageView.setImageBitmap(thumbnail);
                    // 将缩略图保存到磁盘缓存
                    diskCache.saveBitmapToDiskCache(photo.getId() + ".png", thumbnail);
                }
            }
        }

        // 设置点击监听器
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(v, position);
            }
        });
    }


    @Override
    public int getItemCount() {
        return photos.size();
    }

    public Photo getItem(int position) {
        return photos.get(position);
    }

    public class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView photoImageView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoImageView = itemView.findViewById(R.id.photo_image_view);
        }
    }
    private Bitmap loadThumbnail(long photoId, int orientation) {
        Uri photoUri = Uri.withAppendedPath(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                String.valueOf(photoId)
        );

        try (InputStream inputStream = context.getContentResolver().openInputStream(photoUri)) {
            // 使用 BitmapFactory 加载原始图像
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap fullSizeBitmap = BitmapFactory.decodeStream(inputStream, null, options);

            // 根据 orientation 旋转图像
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            assert fullSizeBitmap != null;
            Bitmap rotatedBitmap = Bitmap.createBitmap(fullSizeBitmap, 0, 0, fullSizeBitmap.getWidth(), fullSizeBitmap.getHeight(), matrix, true);

            // 创建缩略图
            Bitmap thumbnail = ThumbnailUtils.extractThumbnail(rotatedBitmap, 200, 200); // 调整缩略图大小

            // 释放不再需要的位图内存
            fullSizeBitmap.recycle();
            rotatedBitmap.recycle();

            return thumbnail;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
