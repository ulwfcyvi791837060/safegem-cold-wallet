package com.bankledger.safecold.utils;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * $desc
 *
 * @author bankledger
 * @time 2018/10/24 14:33
 */
public class RingManager {

    private final SoundPool soundPool;

    private static RingManager INSTANCE;
    private int beepId;
    private int crystalId;
    private int startId;
    private boolean beepIsComplete = false;
    private boolean crystalIsComplete = false;
    private boolean startIsComplete = false;


    private SoundPool.OnLoadCompleteListener loadCompleteListener = new SoundPool.OnLoadCompleteListener() {
        @Override
        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            soundPool.play(sampleId, 1, 1, 0, 0, 1);
            if (sampleId == beepId) {
                beepIsComplete = true;
            } else if (sampleId == crystalId) {
                crystalIsComplete = true;
            } else if (sampleId == startId) {
                startIsComplete = true;
            } else {
            }
        }
    };

    private RingManager() {
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(1);
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
        builder.setAudioAttributes(attrBuilder.build());
        soundPool = builder.build();
    }

    public static RingManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RingManager();
        }
        return INSTANCE;
    }

    public void playBeep() {
        if (beepIsComplete) {
            soundPool.play(beepId, 1, 1, 0, 0, 1);
        } else {
            beepId = soundPool.load(SafeColdApplication.mContext, R.raw.beep, 1);
            soundPool.setOnLoadCompleteListener(loadCompleteListener);
        }
    }

    public void playCrystal() {
        if (crystalIsComplete) {
            soundPool.play(crystalId, 1, 1, 0, 0, 1);
        } else {
            crystalId = soundPool.load(SafeColdApplication.mContext, R.raw.crystal_ring, 1);
            soundPool.setOnLoadCompleteListener(loadCompleteListener);
        }
    }

    public void playStart() {
        if (startIsComplete) {
            soundPool.play(startId, 1, 1, 0, 0, 1);
        } else {
            startId = soundPool.load(SafeColdApplication.mContext, R.raw.searchlight, 1);
            soundPool.setOnLoadCompleteListener(loadCompleteListener);
        }
    }
}
