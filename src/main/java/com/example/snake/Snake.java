package com.example.snake;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
/**
 * Snake: a simple game that everyone can enjoy.
 *
 * This is an implementation of the classic Game "Snake", in which you control a
 * serpent roaming around the garden looking for apples. Be careful, though,
 * because when you catch one, not only will you become longer, but you'll move
 * faster. Running into yourself or the walls will end the game.
 *
 */
// 贪吃蛇： 经典游戏，在一个花园中找苹果吃，吃了苹果会变长，速度变快。碰到自己和墙就挂掉。
public class Snake extends Activity {
    private SnakeView mSnakeView;
    private static String ICICLE_KEY = "snake-view";
    /**
     * Called when Activity is first created. Turns off the title bar, sets up
     * the content views, and fires up the SnakeView.
     *
     */
    // 在 activity 第一次创建时被调用
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.snake_layout);
        mSnakeView = (SnakeView) findViewById(R.id.snake);
        mSnakeView.setTextView((TextView) findViewById(R.id.text));
        // 检查存贮状态以确定是重新开始还是恢复状态
        if (savedInstanceState == null) {
            // 存储状态为空，说明刚启动可以切换到准备状态
            mSnakeView.setMode(SnakeView.READY);
        } else {
            // 已经保存过，那么就去恢复原有状态
            Bundle map = savedInstanceState.getBundle(ICICLE_KEY);
            if (map != null) {
                // 恢复状态
                mSnakeView.restoreState(map);
            } else {
                // 设置状态为暂停
                mSnakeView.setMode(SnakeView.PAUSE);
            }
        }
    }
    // 暂停事件被触发时
    @Override
    protected void onPause() {
        super.onPause();
        // Pause the game along with the activity
        mSnakeView.setMode(SnakeView.PAUSE);
    }
    // 状态保存
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // 存储游戏状态到View里
        outState.putBundle(ICICLE_KEY, mSnakeView.saveState());
    }
}