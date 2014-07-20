package com.kenny.openimgur;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.kenny.openimgur.classes.ImgurPhoto;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;

import java.io.File;
import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by kcampagna on 6/22/14.
 */
public class ViewPhotoActivity extends BaseActivity {
    private static final long HIDE_DELAY = DateUtils.SECOND_IN_MILLIS * 3;

    private ImageView mImageView;

    private ProgressBar mProgressBar;

    public static final String KEY_IMAGE = "image";

    private ImgurPhoto photo;

    private View mDecorView;

    public static Intent createIntent(@NonNull Context context, @NonNull ImgurPhoto photo) {
        Intent intent = new Intent(context, ViewPhotoActivity.class);
        intent.putExtra(KEY_IMAGE, photo);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getIntent().hasExtra(KEY_IMAGE)) {
            //TODO Error message
            finish();
            return;
        }

        setContentView(R.layout.activity_view_photo);
        mDecorView = getWindow().getDecorView();
        photo = getIntent().getParcelableExtra(KEY_IMAGE);
        mImageView = (ImageView) findViewById(R.id.image);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        app.getImageLoader().displayImage(photo.getLink(), mImageView, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String s, View view) {
            }

            @Override
            public void onLoadingFailed(String s, View view, FailReason failReason) {
                finish();
                Toast.makeText(getApplicationContext(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                mProgressBar.setVisibility(View.GONE);
                PhotoViewAttacher photoView = new PhotoViewAttacher(mImageView);

                if (photo.isAnimated()) {
                    // The file SHOULD be in our cache if the image has successfully loaded
                    File file = DiskCacheUtils.findInCache(photo.getLink(), app.getImageLoader().getDiskCache());

                    if (file != null && file.exists()) {
                        try {
                            GifDrawable drawable = new GifDrawable(file);
                            mImageView.setImageDrawable(drawable);
                        } catch (IOException e) {
                            e.printStackTrace();
                            finish();
                            Toast.makeText(getApplicationContext(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        finish();
                        Toast.makeText(getApplicationContext(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                    }
                }

                // Setup a listener to hide the UI if the photo is tapped (only for api 19 and higher)
                if (app.SDK_VERSION >= Build.VERSION_CODES.KITKAT) {
                    photoView.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
                        @Override
                        public void onPhotoTap(View view, float x, float y) {
                            if ((mDecorView.getSystemUiVisibility()
                                    & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                                hideSystemUI();
                            }
                        }
                    });
                }
                photoView.update();
            }

            @Override
            public void onLoadingCancelled(String s, View view) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mHideHandler.removeMessages(0);
        mImageView = null;
        mProgressBar = null;
        mDecorView = null;
        super.onDestroy();
    }

    @TargetApi(19)
    private void hideSystemUI() {
        mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_photo, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.download:
                Intent intent = new Intent(getApplicationContext(), DownloaderService.class);
                intent.putExtra(DownloaderService.KEY_IMAGE, photo);
                startService(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private final Handler mHideHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            hideSystemUI();
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (app.SDK_VERSION >= Build.VERSION_CODES.KITKAT) {
            if (hasFocus) {
                mHideHandler.removeMessages(0);
                mHideHandler.sendEmptyMessageDelayed(0, HIDE_DELAY);
            } else {
                mHideHandler.removeMessages(0);
            }
        }
    }
}