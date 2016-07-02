package org.easydarwin.easyplayer;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.umeng.analytics.MobclickAgent;

import org.easydarwin.video.EasyRTSPClient;
import org.easydarwin.video.RTSPClient;
import org.esaydarwin.rtsp.player.R;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.concurrent.atomic.AtomicInteger;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class PlayActivity extends AppCompatActivity {

    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra("play_url");
        if (TextUtils.isEmpty(url)) {
            finish();
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        container = (LinearLayout) findViewById(R.id.player_container);
        container.setLayoutTransition(new LayoutTransition());
        if (savedInstanceState == null) {
            addVideoSource(url);
        }

        if (isLandscape()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            container.setOrientation(LinearLayout.HORIZONTAL);
        } else {
            container.setOrientation(LinearLayout.VERTICAL);
        }
    }


    /**
     * 请求添加新播放窗口
     */
    public void onAddWindow() {
        new AlertDialog.Builder(PlayActivity.this).setTitle("新的播放窗口").setItems(new CharSequence[]{"选取历史播放记录", "手动输入视频源"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    final SharedPreferences preferences = getSharedPreferences("PlaylistActivity", MODE_PRIVATE);
                    JSONArray mArray;
                    try {
                        mArray = new JSONArray(preferences.getString("play_list", "[\"rtsp://121.41.73.249/1001_home.sdp\"]"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        mArray = new JSONArray();
                    }
                    final CharSequence[] array = new CharSequence[mArray.length()];
                    if (array.length == 0) {
                        Toast.makeText(PlayActivity.this, "没有历史播放记录", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (int i = 0; i < array.length; i++) {
                        array[i] = mArray.optString(i);
                    }
                    new AlertDialog.Builder(PlayActivity.this).setTitle("新的播放窗口").setItems(array, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            addVideoSource(String.valueOf(array[which]));
                        }
                    }).setNegativeButton(android.R.string.cancel, null).show();
                } else {
                    final EditText edit = new EditText(PlayActivity.this);
                    final int hori = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
                    final int verti = (int) getResources().getDimension(R.dimen.activity_vertical_margin);
                    edit.setPadding(hori, verti, hori, verti);
                    final AlertDialog dlg = new AlertDialog.Builder(PlayActivity.this).setView(edit).setTitle("请输入播放地址").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String url = String.valueOf(edit.getText());
                            if (TextUtils.isEmpty(url)) {
                                return;
                            }
                            final SharedPreferences preferences = getSharedPreferences("PlaylistActivity", MODE_PRIVATE);
                            JSONArray mArray;
                            try {
                                mArray = new JSONArray(preferences.getString("play_list", "[\"rtsp://121.41.73.249/1001_home.sdp\"]"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                                mArray = new JSONArray();
                            }
                            mArray.put(url);
                            preferences.edit().putString("play_list", String.valueOf(mArray)).apply();
                            addVideoSource(url);
                        }
                    }).setNegativeButton("取消", null).create();
                    dlg.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
                        }
                    });
                    dlg.show();
                }
            }
        }).show();
    }

    /**
     * 增加一个视频窗口。每一个PlayFragment表示一个播放窗口,在这里会增加一个PlayFragment。
     *
     * @param url
     */
    private void addVideoSource(String url) {
        final FrameLayout item = new FrameLayout(PlayActivity.this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.weight = 1;
        item.setLayoutParams(params);
        ViewCompat.setTransitionName(item, "video_animation");
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            item.setId(View.generateViewId());
        } else {
            item.setId(generateViewId());
        }
        container.addView(item);
        getSupportFragmentManager().beginTransaction().add(item.getId(), PlayFragment.newInstance(url, RTSPClient.TRANSTYPE_TCP)).commit();
    }

    /**
     * 删除一个播放窗口
     *
     * @param id
     */
    public void onRemoveVideoFragment(int id) {
        getSupportFragmentManager().beginTransaction().remove(getSupportFragmentManager().findFragmentById(id)).commit();
        container.removeView(container.findViewById(id));
    }

    /**
     * 播放窗口被点击,此时app会弹出一个OptionFragment,并且绑定被点击的fragment(这样就会使能删除按钮,点击删除按钮,即可把此播放窗口删除)
     *
     * @param fragment
     */
    public void onPlayFragmentClicked(PlayFragment fragment) {
        // 被绑定的窗口,呈选中状态
        fragment.setSelected(true);
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, VideoWindowOptionMenuFragment.newInstance(fragment.getId())).addToBackStack(null).commit();
    }

    /**
     * 播放窗口以外的区域被点击。此时app也弹出OptionFragment,但是不绑定播放窗口。这样的话不使能删除按钮。
     *
     * @param view
     */
    public void onOpenOptionMenu(View view) {
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, VideoWindowOptionMenuFragment.newInstance(0)).addToBackStack(null).commit();
    }

    /**
     * 删除OptionFragment
     *
     * @param optionFragment
     */
    public void onRemoveOptionMenu(VideoWindowOptionMenuFragment optionFragment) {
        getSupportFragmentManager().beginTransaction().remove(optionFragment).commit();
        getSupportFragmentManager().popBackStack();

        // 如果之前有绑定窗口,那先反选之
        int attachedPlayFragmentId = optionFragment.getAttachedPlayFragmentId();
        if (attachedPlayFragmentId != 0) {
            PlayFragment fragment = (PlayFragment) getSupportFragmentManager().findFragmentById(attachedPlayFragmentId);
            if (fragment != null) {
                fragment.setSelected(false);
            }
        }
    }


    /**
     * 切换屏幕方向
     */
    public void onToggleOrientation() {
        setRequestedOrientation(isLandscape() ?
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    /**
     * Generate a value suitable for use in View.setId(int)
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
    public static int generateViewId() {
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    private boolean isLandscape() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == ORIENTATION_LANDSCAPE;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LinearLayout container = (LinearLayout) findViewById(R.id.player_container);
        if (newConfig.orientation == ORIENTATION_LANDSCAPE) {
            // 横屏,全屏状态
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setNavVisibility(false);
            // 横屏情况下,播放窗口横着排开
            container.setOrientation(LinearLayout.HORIZONTAL);
        } else {
            // 竖屏,取消全屏状态
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setNavVisibility(true);
            // 竖屏情况下,播放窗口竖着排开
            container.setOrientation(LinearLayout.VERTICAL);
        }
    }

    public boolean multiWindows() {
        LinearLayout container = (LinearLayout) findViewById(R.id.player_container);
        return container.getChildCount() > 1;
    }

    public void setNavVisibility(boolean visible) {
        if (!ViewConfigurationCompat.hasPermanentMenuKey(ViewConfiguration.get(this))) {
            int newVis = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            if (!visible) {
                // } else {
                // newVis &= ~(View.SYSTEM_UI_FLAG_LOW_PROFILE |
                // View.SYSTEM_UI_FLAG_FULLSCREEN |
                // View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                newVis |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE;
            }
            // If we are now visible, schedule a timer for us to go invisible.
            // Set the new desired visibility.
            getWindow().getDecorView().setSystemUiVisibility(newVis);
        }
    }

    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);       //统计时长
    }

    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

}
