package com.example.snake;

import java.util.ArrayList;
import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

/**
 * SnakeView: implementation of a simple game of Snake
 *
 *
 */
public class SnakeView extends TileView {

    private static final String TAG = "Deaboway";

    /**
     * Current mode of application: READY to run, RUNNING, or you have already
     * lost. static final ints are used instead of an enum for performance
     * reasons.
     */
    // 游戏状态，默认值是准备状态
    private int mMode = READY;

    // 游戏的四个状态 暂停 准备 运行 和 失败
    public static final int PAUSE = 0;
    public static final int READY = 1;
    public static final int RUNNING = 2;
    public static final int LOSE = 3;

    // 游戏中蛇的前进方向，默认值北方
    private int mDirection = NORTH;
    // 下一步的移动方向，默认值北方
    private int mNextDirection = NORTH;

    // 游戏方向设定 北 南 东 西
    private static final int NORTH = 1;
    private static final int SOUTH = 2;
    private static final int EAST = 3;
    private static final int WEST = 4;

    /**
     * Labels for the drawables that will be loaded into the TileView class
     */
    // 三种游戏元
    private static final int RED_STAR = 1;
    private static final int YELLOW_STAR = 2;
    private static final int GREEN_STAR = 3;

    /**
     * mScore: used to track the number of apples captured mMoveDelay: number of
     * milliseconds between snake movements. This will decrease as apples are
     * captured.
     */
    // 游戏得分
    private long mScore = 0;

    // 移动延迟
    private long mMoveDelay = 600;

    /**
     * mLastMove: tracks the absolute time when the snake last moved, and is
     * used to determine if a move should be made based on mMoveDelay.
     */
    // 最后一次移动时的毫秒时刻
    private long mLastMove;

    /**
     * mStatusText: text shows to the user in some run states
     */
    // 显示游戏状态的文本组件
    private TextView mStatusText;

    /**
     * mSnakeTrail: a list of Coordinates that make up the snake's body
     * mAppleList: the secret location of the juicy apples the snake craves.
     */
    // 蛇身数组(数组以坐标对象为元素)
    private ArrayList<Coordinate> mSnakeTrail = new ArrayList<Coordinate>();

    // 苹果数组(数组以坐标对象为元素)
    private ArrayList<Coordinate> mAppleList = new ArrayList<Coordinate>();

    /**
     * Everyone needs a little randomness in their life
     */
    // 随机数
    private static final Random RNG = new Random();

    /**
     * Create a simple handler that we can use to cause animation to happen. We
     * set ourselves as a target and we can use the sleep() function to cause an
     * update/invalidate to occur at a later date.
     */
    // 创建一个Refresh Handler来产生动画： 通过sleep()来实现
    private RefreshHandler mRedrawHandler = new RefreshHandler();

    // 一个Handler
    class RefreshHandler extends Handler {

        // 处理消息队列
        @Override
        public void handleMessage(Message msg) {
            // 更新View对象
            SnakeView.this.update();
            // 强制重绘
            SnakeView.this.invalidate();
        }

        // 延迟发送消息
        public void sleep(long delayMillis) {
            this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    };

    /**
     * Constructs a SnakeView based on inflation from XML
     *
     * @param context
     * @param attrs
     */
    // 构造函数
    public SnakeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 构造时初始化
        initSnakeView();
    }

    public SnakeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initSnakeView();
    }

    // 初始化
    private void initSnakeView() {
        // 可选焦点
        setFocusable(true);

        Resources r = this.getContext().getResources();

        // 设置贴片图片数组
        resetTiles(4);

        // 把三种图片存到Bitmap对象数组
        loadTile(RED_STAR, r.getDrawable(R.drawable.redstar));
        loadTile(YELLOW_STAR, r.getDrawable(R.drawable.yellowstar));
        loadTile(GREEN_STAR, r.getDrawable(R.drawable.greenstar));

    }

    // 开始新的游戏——初始化
    private void initNewGame() {
        // 清空ArrayList列表
        mSnakeTrail.clear();
        mAppleList.clear();

        // For now we're just going to load up a short default eastbound snake
        // that's just turned north
        // 创建蛇身

        mSnakeTrail.add(new Coordinate(7, 7));
        mSnakeTrail.add(new Coordinate(6, 7));
        mSnakeTrail.add(new Coordinate(5, 7));
        mSnakeTrail.add(new Coordinate(4, 7));
        mSnakeTrail.add(new Coordinate(3, 7));
        mSnakeTrail.add(new Coordinate(2, 7));

        // 新的方向 ：北方
        mNextDirection = NORTH;

        // 2个随机位置的苹果
        addRandomApple();
        addRandomApple();

        // 移动延迟
        mMoveDelay = 600;
        // 初始得分0
        mScore = 0;
    }

    /**
     * Given a ArrayList of coordinates, we need to flatten them into an array
     * of ints before we can stuff them into a map for flattening and storage.
     *
     * @param cvec
     *            : a ArrayList of Coordinate objects
     * @return : a simple array containing the x/y values of the coordinates as
     *         [x1,y1,x2,y2,x3,y3...]
     */
    // 坐标数组转整数数组，把Coordinate对象的x y放到一个int数组中——用来保存状态
    private int[] coordArrayListToArray(ArrayList<Coordinate> cvec) {
        int count = cvec.size();
        int[] rawArray = new int[count * 2];
        for (int index = 0; index < count; index++) {
            Coordinate c = cvec.get(index);
            rawArray[2 * index] = c.x;
            rawArray[2 * index + 1] = c.y;
        }
        return rawArray;
    }

    /**
     * Save game state so that the user does not lose anything if the game
     * process is killed while we are in the background.
     *
     * @return a Bundle with this view's state
     */
    // 保存状态
    public Bundle saveState() {

        Bundle map = new Bundle();

        map.putIntArray("mAppleList", coordArrayListToArray(mAppleList));
        map.putInt("mDirection", Integer.valueOf(mDirection));
        map.putInt("mNextDirection", Integer.valueOf(mNextDirection));
        map.putLong("mMoveDelay", Long.valueOf(mMoveDelay));
        map.putLong("mScore", Long.valueOf(mScore));
        map.putIntArray("mSnakeTrail", coordArrayListToArray(mSnakeTrail));

        return map;
    }

    /**
     * Given a flattened array of ordinate pairs, we reconstitute them into a
     * ArrayList of Coordinate objects
     *
     * @param rawArray
     *            : [x1,y1,x2,y2,...]
     * @return a ArrayList of Coordinates
     */
    // 整数数组转坐标数组，把一个int数组中的x y放到Coordinate对象数组中——用来恢复状态
    private ArrayList<Coordinate> coordArrayToArrayList(int[] rawArray) {
        ArrayList<Coordinate> coordArrayList = new ArrayList<Coordinate>();

        int coordCount = rawArray.length;
        for (int index = 0; index < coordCount; index += 2) {
            Coordinate c = new Coordinate(rawArray[index], rawArray[index + 1]);
            coordArrayList.add(c);
        }
        return coordArrayList;
    }

    /**
     * Restore game state if our process is being relaunched
     *
     * @param icicle
     *            a Bundle containing the game state
     */
    // 恢复状态
    public void restoreState(Bundle icicle) {

        setMode(PAUSE);

        mAppleList = coordArrayToArrayList(icicle.getIntArray("mAppleList"));
        mDirection = icicle.getInt("mDirection");
        mNextDirection = icicle.getInt("mNextDirection");
        mMoveDelay = icicle.getLong("mMoveDelay");
        mScore = icicle.getLong("mScore");
        mSnakeTrail = coordArrayToArrayList(icicle.getIntArray("mSnakeTrail"));
    }

    /*
     * handles key events in the game. Update the direction our snake is
     * traveling based on the DPAD. Ignore events that would cause the snake to
     * immediately turn back on itself.
     *
     * (non-Javadoc)
     *
     * @see android.view.View#onKeyDown(int, android.os.KeyEvent)
     */
    // 监听用户键盘操作，并处理这些操作
    // 按键事件处理，确保贪吃蛇只能90度转向，而不能180度转向
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {

        // 向上键
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            // 准备状态或者失败状态时
            if (mMode == READY | mMode == LOSE) {
                /*
                 * At the beginning of the game, or the end of a previous one,
                 * we should start a new game.
                 */
                // 初始化游戏
                initNewGame();
                // 设置游戏状态为运行
                setMode(RUNNING);
                // 更新
                update();
                // 返回
                return (true);
            }

            // 暂停状态时
            if (mMode == PAUSE) {
                /*
                 * If the game is merely paused, we should just continue where
                 * we left off.
                 */
                // 设置成运行状态
                setMode(RUNNING);
                update();
                // 返回
                return (true);
            }

            // 如果是运行状态时，如果方向原有方向不是向南，那么方向转向北
            if (mDirection != SOUTH) {
                mNextDirection = NORTH;
            }
            return (true);
        }

        // 向下键
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            // 原方向不是向上时，方向转向南
            if (mDirection != NORTH) {
                mNextDirection = SOUTH;
            }
            // 返回
            return (true);
        }

        // 向左键
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            // 原方向不是向右时，方向转向西
            if (mDirection != EAST) {
                mNextDirection = WEST;
            }
            // 返回
            return (true);
        }

        // 向右键
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            // 原方向不是向左时，方向转向东
            if (mDirection != WEST) {
                mNextDirection = EAST;
            }
            // 返回
            return (true);
        }

        // 按其他键时按原有功能返回
        return super.onKeyDown(keyCode, msg);
    }

    /**
     * Sets the TextView that will be used to give information (such as "Game
     * Over" to the user.
     *
     * @param newView
     */
    // 设置状态显示View
    public void setTextView(TextView newView) {
        mStatusText = newView;
    }

    /**
     * Updates the current mode of the application (RUNNING or PAUSED or the
     * like) as well as sets the visibility of textview for notification
     *
     * @param newMode
     */
    // 设置游戏状态
    public void setMode(int newMode) {

        // 把当前游戏状态存入oldMode
        int oldMode = mMode;
        // 把游戏状态设置为新状态
        mMode = newMode;

        // 如果新状态是运行状态，且原有状态为不运行，那么就开始游戏
        if (newMode == RUNNING & oldMode != RUNNING) {
            // 设置mStatusTextView隐藏
            mStatusText.setVisibility(View.INVISIBLE);
            // 更新
            update();
            return;
        }

        Resources res = getContext().getResources();
        CharSequence str = "";

        // 如果新状态是暂停状态，那么设置文本内容为暂停内容
        if (newMode == PAUSE) {
            str = res.getText(R.string.mode_pause);
        }

        // 如果新状态是准备状态，那么设置文本内容为准备内容
        if (newMode == READY) {
            str = res.getText(R.string.mode_ready);
        }

        // 如果新状态时失败状态，那么设置文本内容为失败内容
        if (newMode == LOSE) {
            // 把上轮的得分显示出来
            str = res.getString(R.string.mode_lose_prefix) + mScore
                    + res.getString(R.string.mode_lose_suffix);
        }

        // 设置文本
        mStatusText.setText(str);
        // 显示该View
        mStatusText.setVisibility(View.VISIBLE);
    }

    /**
     * Selects a random location within the garden that is not currently covered
     * by the snake. Currently _could_ go into an infinite loop if the snake
     * currently fills the garden, but we'll leave discovery of this prize to a
     * truly excellent snake-player.
     *
     */
    // 添加苹果
    private void addRandomApple() {
        // 新的坐标
        Coordinate newCoord = null;
        // 防止新苹果出席在蛇身下
        boolean found = false;
        // 没有找到合适的苹果，就在循环体内一直循环，直到找到合适的苹果
        while (!found) {
            // 为苹果再找一个坐标,先随机一个X值
            int newX = 1 + RNG.nextInt(mXTileCount - 2);
            // 再随机一个Y值
            int newY = 1 + RNG.nextInt(mYTileCount - 2);
            // 新坐标
            newCoord = new Coordinate(newX, newY);

            // Make sure it's not already under the snake
            // 确保新苹果不在蛇身下，先假设没有发生冲突
            boolean collision = false;

            int snakelength = mSnakeTrail.size();
            // 和蛇占据的所有坐标比较
            for (int index = 0; index < snakelength; index++) {
                // 只要和蛇占据的任何一个坐标相同，即认为发生冲突了
                if (mSnakeTrail.get(index).equals(newCoord)) {
                    collision = true;
                }
            }
            // if we're here and there's been no collision, then we have
            // a good location for an apple. Otherwise, we'll circle back
            // and try again
            // 如果有冲突就继续循环，如果没冲突flag的值就是false,那么自然会退出循环，新坐标也就诞生了
            found = !collision;
        }

        if (newCoord == null) {
            Log.e(TAG, "Somehow ended up with a null newCoord!");
        }
        // 生成一个新苹果放在苹果列表中（两个苹果有可能会重合——这时候虽然看到的是一个苹果，但是呢，分数就是两个分数。）
        mAppleList.add(newCoord);
    }

    /**
     * Handles the basic update loop, checking to see if we are in the running
     * state, determining if a move should be made, updating the snake's
     * location.
     */
    // 更新 各种动作，特别是 贪吃蛇 的位置， 还包括：墙、苹果等的更新
    public void update() {
        // 如果是处于运行状态
        if (mMode == RUNNING) {

            long now = System.currentTimeMillis();

            // 如果当前时间距离最后一次移动的时间超过了延迟时间
            if (now - mLastMove > mMoveDelay) {
                //
                clearTiles();
                updateWalls();
                updateSnake();
                updateApples();
                mLastMove = now;
            }
            // Handler 会话进程sleep一个延迟时间单位
            mRedrawHandler.sleep(mMoveDelay);
        }

    }

    /**
     * Draws some walls.
     *
     */
    // 更新墙
    private void updateWalls() {
        for (int x = 0; x < mXTileCount; x++) {
            // 给上边线的每个贴片位置设置一个绿色索引标识
            setTile(GREEN_STAR, x, 0);
            // 给下边线的每个贴片位置设置一个绿色索引标识
            setTile(GREEN_STAR, x, mYTileCount - 1);
        }
        for (int y = 1; y < mYTileCount - 1; y++) {
            // 给左边线的每个贴片位置设置一个绿色索引标识
            setTile(GREEN_STAR, 0, y);
            // 给右边线的每个贴片位置设置一个绿色索引标识
            setTile(GREEN_STAR, mXTileCount - 1, y);
        }
    }

    /**
     * Draws some apples.
     *
     */
    // 更新苹果
    private void updateApples() {
        for (Coordinate c : mAppleList) {
            setTile(YELLOW_STAR, c.x, c.y);
        }
    }

    /**
     * Figure out which way the snake is going, see if he's run into anything
     * (the walls, himself, or an apple). If he's not going to die, we then add
     * to the front and subtract from the rear in order to simulate motion. If
     * we want to grow him, we don't subtract from the rear.
     *
     */
    // 更新蛇
    private void updateSnake() {
        // 生长标志
        boolean growSnake = false;

        // 得到蛇头坐标
        Coordinate head = mSnakeTrail.get(0);
        // 初始化一个新的蛇头坐标
        Coordinate newHead = new Coordinate(1, 1);

        // 当前方向改成新的方向
        mDirection = mNextDirection;

        // 根据方向确定蛇头新坐标
        switch (mDirection) {
            // 如果方向向东（右），那么X加1
            case EAST: {
                newHead = new Coordinate(head.x + 1, head.y);
                break;
            }
            // 如果方向向西（左），那么X减1
            case WEST: {
                newHead = new Coordinate(head.x - 1, head.y);
                break;
            }
            // 如果方向向北（上），那么Y减1
            case NORTH: {
                newHead = new Coordinate(head.x, head.y - 1);
                break;
            }
            // 如果方向向南（下），那么Y加1
            case SOUTH: {
                newHead = new Coordinate(head.x, head.y + 1);
                break;
            }
        }

        // Collision detection
        // For now we have a 1-square wall around the entire arena
        // 冲突检测 新蛇头是否四面墙重叠，那么游戏结束
        if ((newHead.x < 1) || (newHead.y < 1) || (newHead.x > mXTileCount - 2)
                || (newHead.y > mYTileCount - 2)) {
            // 设置游戏状态为Lose
            setMode(LOSE);
            // 返回
            return;

        }

        // Look for collisions with itself
        // 冲突检测 新蛇头是否和自身坐标重叠，重叠的话游戏也结束
        int snakelength = mSnakeTrail.size();

        for (int snakeindex = 0; snakeindex < snakelength; snakeindex++) {
            Coordinate c = mSnakeTrail.get(snakeindex);
            if (c.equals(newHead)) {
                // 设置游戏状态为Lose
                setMode(LOSE);
                // 返回
                return;
            }
        }

        // Look for apples
        // 看新蛇头和苹果们是否重叠
        int applecount = mAppleList.size();
        for (int appleindex = 0; appleindex < applecount; appleindex++) {
            Coordinate c = mAppleList.get(appleindex);
            if (c.equals(newHead)) {
                // 如果重叠，苹果坐标从苹果列表中移除
                mAppleList.remove(c);
                // 再立刻增加一个新苹果
                addRandomApple();
                // 得分加一
                mScore++;
                // 延迟是以前的90%
                mMoveDelay *= 0.9;
                // 蛇增长标志改为真
                growSnake = true;
            }
        }

        // push a new head onto the ArrayList and pull off the tail
        // 在蛇头的位置增加一个新坐标
        mSnakeTrail.add(0, newHead);
        // except if we want the snake to grow
        // 如果没有增长
        if (!growSnake) {
            // 如果蛇头没增长则删去最后一个坐标，相当于蛇向前走了一步
            mSnakeTrail.remove(mSnakeTrail.size() - 1);
        }

        int index = 0;
        // 重新设置一下颜色，蛇头是黄色的（同苹果一样），蛇身是红色的
        for (Coordinate c : mSnakeTrail) {
            if (index == 0) {
                setTile(YELLOW_STAR, c.x, c.y);
            } else {
                setTile(RED_STAR, c.x, c.y);
            }
            index++;
        }

    }

    /**
     * Simple class containing two integer values and a comparison function.
     * There's probably something I should use instead, but this was quick and
     * easy to build.
     *
     */
    // 坐标内部类——原作者说这是临时做法
    private class Coordinate {
        public int x;
        public int y;

        // 构造函数
        public Coordinate(int newX, int newY) {
            x = newX;
            y = newY;
        }

        // 重写equals
        public boolean equals(Coordinate other) {
            if (x == other.x && y == other.y) {
                return true;
            }
            return false;
        }

        // 重写toString
        @Override
        public String toString() {
            return "Coordinate: [" + x + "," + y + "]";
        }
    }

}