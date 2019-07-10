/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bankledger.safecold;

import android.app.Application;

import android.content.Context;
import android.os.Environment;

import com.bankledger.safecold.db.AbstractDbImpl;
import com.bankledger.safecold.db.DatabaseHelper;
import com.bankledger.safecold.mnemon.MnemonicCodeAndroid;
import com.bankledger.safecold.ui.activity.MainWalletActivity;
import com.bankledger.safecold.utils.CrashHandler;
import com.bankledger.safecold.utils.EncryptionChipManagerV2;
import com.bankledger.safecold.utils.SharedPreferencesUtil;
import com.bankledger.safecold.xrandom.LinuxSecureRandom;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.crypto.mnemonic.MnemonicCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

public class SafeColdApplication extends Application {

    public static Context mContext;
    public static DatabaseHelper dbHelper;
    public static SharedPreferencesUtil appSharedPreferenceUtil;

    public static MainWalletActivity mainWalletActivity;

    //用于蓝牙状态监听
    private static SafeColdApplication mInstance = null;

    public static synchronized SafeColdApplication getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        new LinuxSecureRandom();
        super.onCreate();

        mInstance = this;

        mContext = getApplicationContext();

        configLog();

        appSharedPreferenceUtil = SharedPreferencesUtil.getDefaultPreferences(mContext);

        dbHelper = new DatabaseHelper(mContext);

        AbstractDbImpl dbImpl = new AbstractDbImpl();
        dbImpl.construct();

        initApp();

        CrashHandler.getInstance().init();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    private void initApp() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HDAddressManager.getInstance();
                try {
                    MnemonicCode.setInstance(new MnemonicCodeAndroid());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void clearData() {
        dbHelper.deleteDB();
        if (SafeColdSettings.SEED_SAVE_TO_CHIP) {
            EncryptionChipManagerV2.getInstance().deleteApp(EncryptionChipManagerV2.getInstance().NEW_APP_NAME);
            EncryptionChipManagerV2.getInstance().deleteApp(EncryptionChipManagerV2.getInstance().APP_NAME);
        }
        appSharedPreferenceUtil.clearSP();
        if (mainWalletActivity != null && !mainWalletActivity.isFinishing() && !mainWalletActivity.isDestroyed()) {
            mainWalletActivity.finish();
        }
    }

    public void configLog()
    {
        // reset the default context (which may already have been initialized)
        // since we want to reconfigure it

        final String logDir = Environment.getExternalStorageDirectory() + File.separator + "logback";
        final String filePref = "log";

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<ILoggingEvent>();
        rollingFileAppender.setAppend(true);
        rollingFileAppender.setContext(context);

        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<ILoggingEvent>();
        rollingPolicy.setFileNamePattern(logDir + "/" + filePref + "_%d{yyyyMMdd}.txt");
        rollingPolicy.setMaxHistory(7);
        rollingPolicy.setParent(rollingFileAppender);  // parent and context required!
        rollingPolicy.setContext(context);
        rollingPolicy.start();

        rollingFileAppender.setRollingPolicy(rollingPolicy);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.setContext(context);
        encoder.start();

        rollingFileAppender.setEncoder(encoder);
        rollingFileAppender.start();

        LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(context);
        logcatAppender.setEncoder(encoder);
        logcatAppender.setName("logcat");
        logcatAppender.start();

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        root.addAppender(rollingFileAppender);
        root.addAppender(logcatAppender);

    }
}
