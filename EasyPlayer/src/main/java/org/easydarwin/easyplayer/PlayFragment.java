package org.easydarwin.easyplayer;


import android.animation.Animator;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.signature.StringSignature;

import org.easydarwin.video.EasyRTSPClient;
import org.easydarwin.video.RTSPClient;
import org.esaydarwin.rtsp.player.BuildConfig;
import org.esaydarwin.rtsp.player.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlayFragment extends Fragment implements TextureView.SurfaceTextureListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "PlayFragment";

    // TODO: Rename and change types of parameters
    private String mUrl;
    private int mType;

    private EasyRTSPClient mStreamRender;
    private ResultReceiver mResultReceiver;
    private int mWidth;
    private int mHeight;
    private View.OnLayoutChangeListener listener;
    private TextureView surfaceView;
    private ImageView cover;

    public void setSelected(boolean selected) {
        surfaceView.animate().scaleX(selected ? 0.9f : 1.0f);
        surfaceView.animate().scaleY(selected ? 0.9f : 1.0f);
        surfaceView.animate().alpha(selected ? 0.7f : 1.0f);
    }

    public static class ReverseInterpolator extends AccelerateDecelerateInterpolator {
        @Override
        public float getInterpolation(float paramFloat) {
            return super.getInterpolation(1.0f - paramFloat);
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param url  Parameter 1.
     * @param type Parameter 2.
     * @return A new instance of fragment PlayFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PlayFragment newInstance(String url, int type) {
        PlayFragment fragment = new PlayFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, url);
        args.putInt(ARG_PARAM2, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUrl = getArguments().getString(ARG_PARAM1);
            mType = getArguments().getInt(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_play, container, false);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayActivity activity = (PlayActivity) getActivity();
                activity.onPlayFragmentClicked(PlayFragment.this);
            }
        });

        cover = (ImageView) view.findViewById(R.id.surface_cover);
//        Glide.with(this).load(PlaylistActivity.url2localPosterFile(getActivity(), mUrl)).diskCacheStrategy(DiskCacheStrategy.NONE).placeholder(R.drawable.placeholder).into(new ImageViewTarget<GlideDrawable>(cover) {
//            @Override
//            protected void setResource(GlideDrawable resource) {
//                int width = resource.getIntrinsicWidth();
//                int height = resource.getIntrinsicHeight();
//                fixPlayerRatio(view, container.getWidth(), container.getHeight(), width, height);
//                cover.setImageDrawable(resource);
//            }
//        });

        Glide.with(this).load(PlaylistActivity.url2localPosterFile(getActivity(), mUrl)).signature(new StringSignature(UUID.randomUUID().toString())).fitCenter().into(cover);
        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        surfaceView = (TextureView) view.findViewById(R.id.surface_view);
        surfaceView.setOpaque(false);
        surfaceView.setSurfaceTextureListener(this);
        mResultReceiver = new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == EasyRTSPClient.RESULT_VIDEO_DISPLAYED) {
//                    Toast.makeText(PlayActivity.this, "视频正在播放了", Toast.LENGTH_SHORT).show();
                    view.findViewById(android.R.id.progress).setVisibility(View.GONE);
                    surfaceView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mWidth != 0 && mHeight != 0) {
                                Bitmap e = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                                surfaceView.getBitmap(e);
                                File f = PlaylistActivity.url2localPosterFile(surfaceView.getContext(), mUrl);
                                saveBitmapInFile(f.getPath(), e);
                                e.recycle();
                            }
                        }
                    });
                    cover.setVisibility(View.GONE);
                } else if (resultCode == EasyRTSPClient.RESULT_VIDEO_SIZE) {
                    mWidth = resultData.getInt(EasyRTSPClient.EXTRA_VIDEO_WIDTH);
                    mHeight = resultData.getInt(EasyRTSPClient.EXTRA_VIDEO_HEIGHT);
                    if (!isLandscape()) {

                        ViewGroup parent = (ViewGroup) view.getParent();
                        parent.addOnLayoutChangeListener(listener);
                        fixPlayerRatio(view, parent.getWidth(), parent.getHeight());
                    }
                } else if (resultCode == EasyRTSPClient.RESULT_TIMEOUT) {
                    Toast.makeText(getActivity(), "试播时间到", Toast.LENGTH_SHORT).show();
                }
            }
        };

        listener = new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                Log.d(TAG, String.format("onLayoutChange left:%d,top:%d,right:%d,bottom:%d->oldLeft:%d,oldTop:%d,oldRight:%d,oldBottom:%d", left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom));
                if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                    if (!isLandscape()) {
                        fixPlayerRatio(view, right - left, bottom - top);
                    } else {
                        PlayActivity activity = (PlayActivity) getActivity();
                        if (!activity.multiWindows()) {
                            view.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
                            view.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                            view.requestLayout();
                        } else {
                            fixPlayerRatio(view, right - left, bottom - top);
                        }
                    }
                }
            }
        };
        ViewGroup parent = (ViewGroup) view.getParent();
        parent.addOnLayoutChangeListener(listener);
    }

    private static void saveBitmapInFile(final String path, Bitmap bitmap) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        } catch (IOException var18) {
            var18.printStackTrace();
        } catch (OutOfMemoryError var19) {
            var19.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException var16) {
                    var16.printStackTrace();
                }
            }

        }

    }

    @Override
    public void onDestroyView() {
        ViewGroup parent = (ViewGroup) getView().getParent();
        parent.removeOnLayoutChangeListener(listener);
        super.onDestroyView();
    }

    private boolean isLandscape() {
        return getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    }

    private void fixPlayerRatio(View renderView, int maxWidth, int maxHeight, int width, int height) {
        if (width == 0 || height == 0) {
            return;
        }
        int widthSize = maxWidth;
        int heightSize = maxHeight;
        float aspectRatio = width * 1.0f / height;


        if (widthSize > heightSize * aspectRatio) {
            height = heightSize;
            width = (int) (height * aspectRatio);
        } else {
            width = widthSize;
            height = (int) (width / aspectRatio);
        }
        renderView.getLayoutParams().width = width;
        renderView.getLayoutParams().height = height;
        renderView.requestLayout();
    }

    private void fixPlayerRatio(View renderView, int maxWidth, int maxHeight) {
        fixPlayerRatio(renderView, maxWidth, maxHeight, mWidth, mHeight);
    }

    private void startRending(Surface surface) {
        mStreamRender = new EasyRTSPClient(getContext(), "F94CAB947C2786773C95DC05244DF8CA", surface, mResultReceiver);
        mStreamRender.start(mUrl, mType, RTSPClient.EASY_SDK_VIDEO_FRAME_FLAG | RTSPClient.EASY_SDK_AUDIO_FRAME_FLAG, "admin", "admin");
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        startRending(new Surface(surface));
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mStreamRender != null) {
            mStreamRender.stop();
            mStreamRender = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, String.format("onSurfaceTextureUpdated [%s]", surface));
    }
}
