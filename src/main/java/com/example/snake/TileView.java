package com.example.snake;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
/**
 * TileView: a View-variant designed for handling arrays of "icons" or other
 * drawables.
 *
 */
// View 变种，用来处理 一组 贴片—— “icons”或其它可绘制的对象
public class TileView extends View {
    /**
     * Parameters controlling the size of the tiles and their range within view.
     * Width/Height are in pixels, and Drawables will be scaled to fit to these
     * dimensions. X/Y Tile Counts are the number of tiles that will be drawn.
     */
    protected static int mTileSize;
    // X轴的贴片数量
    protected static int mXTileCount;
    // Y轴的贴片数量
    protected static int mYTileCount;
    // X偏移量
    private static int mXOffset;
    // Y偏移量
    private static int mYOffset;
    /**
     * A hash that maps integer handles specified by the subclasser to the
     * drawable that will be used for that reference
     */
    // 贴片图像的图像数组
    private Bitmap[] mTileArray;
    /**
     * A two-dimensional array of integers in which the number represents the
     * index of the tile that should be drawn at that locations
     */
    // 保存每个贴片的索引——二维数组
    private int[][] mTileGrid;
    // Paint对象（画笔、颜料）
    private final Paint mPaint = new Paint();
    // 构造函数
    public TileView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.TileView);
        mTileSize = a.getInt(R.styleable.TileView_tileSize, 12);
        a.recycle();
    }
    public TileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.TileView);
        mTileSize = a.getInt(R.styleable.TileView_tileSize, 12);
        a.recycle();
    }
    /**
     * Rests the internal array of Bitmaps used for drawing tiles, and sets the
     * maximum index of tiles to be inserted
     *
     * @param tilecount
     */
    // 设置贴片图片数组
    public void resetTiles(int tilecount) {
        mTileArray = new Bitmap[tilecount];
    }
    // 回调：当该View的尺寸改变时调用，在onDraw()方法调用之前就会被调用，所以用来设置一些变量的初始值
    // 在视图大小改变的时候调用，比如说手机由垂直旋转为水平
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // 定义X轴贴片数量
        mXTileCount = (int) Math.floor(w / mTileSize);
        mYTileCount = (int) Math.floor(h / mTileSize);
        // X轴偏移量
        mXOffset = ((w - (mTileSize * mXTileCount)) / 2);
        // Y轴偏移量
        mYOffset = ((h - (mTileSize * mYTileCount)) / 2);
        // 定义贴片的二维数组
        mTileGrid = new int[mXTileCount][mYTileCount];
        // 清空所有贴片
        clearTiles();
    }
    /**
     * Function to set the specified Drawable as the tile for a particular
     * integer key.
     *
     * @param key
     * @param tile
     */
    // 给mTileArray这个Bitmap图片数组设置值
    public void loadTile(int key, Drawable tile) {
        Bitmap bitmap = Bitmap.createBitmap(mTileSize, mTileSize,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        tile.setBounds(0, 0, mTileSize, mTileSize);
        // 把一个drawable转成一个Bitmap
        tile.draw(canvas);
        // 在数组里存入该Bitmap
        mTileArray[key] = bitmap;
    }
    /**
     * Resets all tiles to 0 (empty)
     *
     */
    // 清空所有贴片
    public void clearTiles() {
        for (int x = 0; x < mXTileCount; x++) {
            for (int y = 0; y < mYTileCount; y++) {
                // 全部设置为0
                setTile(0, x, y);
            }
        }
    }
    /**
     * Used to indicate that a particular tile (set with loadTile and referenced
     * by an integer) should be drawn at the given x/y coordinates during the
     * next invalidate/draw cycle.
     *
     * @param tileindex
     * @param x
     * @param y
     */
    // 给某个贴片位置设置一个状态索引
    public void setTile(int tileindex, int x, int y) {
        mTileGrid[x][y] = tileindex;
    }
    // onDraw 在视图需要重画的时候调用，比如说使用invalidate刷新界面上的某个矩形区域
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int x = 0; x < mXTileCount; x += 1) {
            for (int y = 0; y < mYTileCount; y += 1) {
                // 当索引大于零，也就是不空时
                if (mTileGrid[x][y] > 0) {
                    // mTileGrid中不为零时画此贴片
                    canvas.drawBitmap(mTileArray[mTileGrid[x][y]], mXOffset + x
                            * mTileSize, mYOffset + y * mTileSize, mPaint);
                }
            }
        }
    }
}