package org.easydarwin.easyplayer;


import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.easydarwin.video.EasyRTSPClient;
import org.easydarwin.video.RTSPClient;
import org.esaydarwin.rtsp.player.R;


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

    // TODO: Rename and change types of parameters
    private String mUrl;
    private int mType;

    private EasyRTSPClient mStreamRender;
    private ResultReceiver mResultReceiver;
    private int mWidth;
    private int mHeight;

    public PlayFragment() {
        // Required empty public constructor
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_play, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final TextureView surfaceView = (TextureView) view.findViewById(R.id.surface_view);
        surfaceView.setSurfaceTextureListener(this);
        mResultReceiver = new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == EasyRTSPClient.RESULT_VIDEO_DISPLAYED) {
//                    Toast.makeText(PlayActivity.this, "视频正在播放了", Toast.LENGTH_SHORT).show();
                    view.findViewById(android.R.id.progress).setVisibility(View.GONE);
                } else if (resultCode == EasyRTSPClient.RESULT_VIDEO_SIZE) {
                    mWidth = resultData.getInt(EasyRTSPClient.EXTRA_VIDEO_WIDTH);
                    mHeight = resultData.getInt(EasyRTSPClient.EXTRA_VIDEO_HEIGHT);
                    if (!isLandscape()) {

                        fixPlayerRatio(surfaceView, getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
                    }
                } else if (resultCode == EasyRTSPClient.RESULT_TIMEOUT) {
                    Toast.makeText(getActivity(), "试播时间到", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }


    private boolean isLandscape() {
        return getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    }

    private void fixPlayerRatio(View renderView, int maxWidth, int maxHeight) {

        int widthSize = maxWidth;
        int heightSize = maxHeight;
        int width = mWidth, height = mHeight;
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

    }
}
