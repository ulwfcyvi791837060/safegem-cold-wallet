/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bankledger.safecold.scan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bankledger.safecold.Constants;
import com.bankledger.safecold.R;
import com.bankledger.safecold.recyclerview.adapter.CommonAdapter;
import com.bankledger.safecold.recyclerview.adapter.ViewHolder;
import com.bankledger.safecold.scan.camera.CameraManager;
import com.bankledger.safecold.ui.activity.BaseActivity;
import com.bankledger.safecold.utils.DPUtils;
import com.bankledger.safecold.utils.DialogUtil;
import com.bankledger.safecold.utils.RingManager;
import com.bankledger.safecoldj.qrcode.QRCodePage;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class ScanActivity extends BaseActivity implements SurfaceHolder.Callback {

    private static final String TAG = ScanActivity.class.getSimpleName();

    private CameraManager cameraManager;
    private ScanActivityHandler handler;
    private Result savedResult;
    private SurfaceView surfaceView;
    private ViewfinderView viewfinderView;
    private TextView scanHint;

    private boolean hasSurface = false;
    private Collection<BarcodeFormat> decodeFormats;
    private String characterSet;

    private InactivityTimer inactivityTimer;
    private AmbientLightManager ambientLightManager;
    private ArrayList<QRCodePage> pageList = new ArrayList<>();
    private RecyclerView rvScanned;
    private CommonAdapter<QRCodePage> indicatorAdapter;
    private List<QRCodePage> indicatorList = new ArrayList<>();
    private LinearLayoutManager layoutManager;

    private boolean playBeep = true;
    private boolean vibrate = true;
    private static final long VIBRATE_DURATION = 300L;

    ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        ambientLightManager = new AmbientLightManager(this);
    }

    @Override
    public void initView() {
        super.initView();
        findViewById(R.id.iv_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        surfaceView = findViewById(R.id.preview_view);
        viewfinderView = findViewById(R.id.viewfinder_view);
        scanHint = findViewById(R.id.scan_hint);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvScanned = findViewById(R.id.rv_scanned);
        rvScanned.setLayoutManager(layoutManager);
        indicatorAdapter = new CommonAdapter<QRCodePage>(R.layout.listitem_scan_indicator) {

            int TYPE_NORMAL = 0;
            int TYPE_SCANNED = 1;

            @Override
            public int getItemViewType(int position) {
                if (pageList.contains(indicatorList.get(position))) {
                    return TYPE_SCANNED;
                } else {
                    return TYPE_NORMAL;
                }
            }

            @Override
            protected void convert(ViewHolder viewHolder, QRCodePage item, int position) {
                TextView tvIndicator = viewHolder.findViewById(R.id.tv_indicator);
                tvIndicator.setTextColor(getItemViewType(position) == TYPE_SCANNED ? getColor(R.color.scan_result_dots) : getColor(R.color.white));
                tvIndicator.setBackground(getItemViewType(position) == TYPE_SCANNED ? getDrawable(R.drawable.indicator_scanned_shape) : getDrawable(R.drawable.indicator_normal_shape));
                tvIndicator.setText(String.valueOf(item.getPageIndex() + 1));
            }

            @Override
            protected void onItemClick(View view, QRCodePage item, int position) {

            }
        };
        rvScanned.setAdapter(indicatorAdapter);

        CheckBox cbLight = findViewById(R.id.cb_light);
        cbLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                cameraManager.setTorch(isChecked);
            }
        });
        cbLight.setChecked(false);
    }

    @Override
    public void initData() {
        super.initData();
        String hintText = getIntent().getStringExtra(Constants.INTENT_KEY1);
        if (!TextUtils.isEmpty(hintText)) {
            scanHint.setText(hintText);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        cameraManager = new CameraManager(getApplication());

        viewfinderView.setCameraManager(cameraManager);

        handler = null;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();

        decodeFormats = null;
        characterSet = null;

        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        cameraManager.closeDriver();
        if (!hasSurface) {
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                cameraManager.setTorch(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraManager.setTorch(true);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        if (handler == null) {
            savedResult = result;
        } else {
            if (result != null) {
                savedResult = result;
            }
            if (savedResult != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, savedResult);
                handler.sendMessage(message);
            }
            savedResult = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // do nothing
        Rect frame = cameraManager.getFramingRect();
        if (frame != null) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) scanHint.getLayoutParams();
            lp.topMargin = frame.top - scanHint.getHeight() - DPUtils.dip2px(getBaseContext(), 16);
            scanHint.setVisibility(View.VISIBLE);
            scanHint.setLayoutParams(lp);
        }
    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult   The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode     A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        inactivityTimer.onActivity();
        if (playBeep) {
            RingManager.getInstance().playBeep();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
        String content = rawResult.getText();
        decodeResult(content);
    }

    private void decodeResult(String content) {
        if (QRCodePage.isQrCodePage(content)) {
            QRCodePage page = QRCodePage.formatQrCodePage(content);
            if (!pageList.contains(page)) {
                if (pageList.size() > 0 && pageList.get(0).getPageCount() != page.getPageCount()) {
                    restartPreviewAfterDelay(1000);
                    return;
                }

                if (page.getPageCount() > 999) {
                    eventResult(content);
                    return;
                }
                pageList.add(page);
                initIndicator();
                indicatorList.remove(page.getPageIndex());
                indicatorList.add(page.getPageIndex(), page);
                indicatorAdapter.notifyView();
                if (QRCodeUtil.scanIsDone(pageList)) {
                    try {
                        String result = QRCodeUtil.decodePage(pageList);
                        eventResult(result);
                    } catch (Exception e) {
                        e.printStackTrace();
                        eventResult(content);
                    }
                } else {
                    restartPreviewAfterDelay(1000);
                }
            } else {
                restartPreviewAfterDelay(1000);
            }
            layoutManager.scrollToPositionWithOffset(page.getPageIndex() - 3 > 0 ? page.getPageIndex() - 3 : 0, 0);
        } else {
            eventResult(content);
        }
    }

    private void eventResult(final String result) {
        returnResult(result);
    }


    private void returnResult(String result) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.INTENT_KEY1, result);
        setResult(Constants.RESULT_SUCCESS, resultIntent);
        finish();
    }

    private void initIndicator() {
        if (indicatorList.size() == 0) {
            int total = pageList.get(0).getPageCount();
            for (int i = 0; i < total; i++) {
                QRCodePage page = new QRCodePage();
                page.setPageCount(total);
                page.setPageIndex(i);
                page.setContent("");
                indicatorList.add(page);
            }
            indicatorAdapter.addAll(indicatorList);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new ScanActivityHandler(this, decodeFormats, characterSet, cameraManager);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.hint_camera_framework_bug));
        builder.setPositiveButton(R.string.ok, new FinishListener(this));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

}
