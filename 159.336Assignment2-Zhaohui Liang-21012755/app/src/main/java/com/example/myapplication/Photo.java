package com.example.myapplication;

public class Photo {

    private long id;
    private int orientation;
    private int width;
    private int height;

    public Photo(long id, int orientation, int width, int height) {
        this.id = id;
        this.orientation = orientation;
        this.width = width;
        this.height = height;
    }

    public long getId() {
        return id;
    }

    public int getOrientation() {
        return orientation;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
