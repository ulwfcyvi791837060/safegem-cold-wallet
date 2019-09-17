package com.bankledger.safecold.utils;

import android.os.Environment;
import com.bankledger.safecold.SafeColdApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * $desc
 *
 * @author bankledger
 * @time 2018/9/25 09:59
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss SSS", Locale.CHINA);
    private String LogDir = Environment.getExternalStorageDirectory() + File.separator + FileUtil.WALLET_LOG_SDCARD_DIR;

    // 系统默认的UncaughtException处理类
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    // CrashHandler实例
    private static CrashHandler INSTANCE = new CrashHandler();
    // 用来存储设备信息和异常信息
    private Map<String, String> infos = new HashMap<>();

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        return INSTANCE;
    }


    public void init() {
        // 获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        if (!handleException(e) && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(t, e);
        } else {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
            SafeColdApplication.mainWalletActivity.finish();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        // 保存日志文件
        saveCrashInfo2File(ex);
        return true;
    }

    private void saveCrashInfo2File(Throwable ex) {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        FileOutputStream fos = null;
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                List<File> logFiles = getCrashLogFileList();
                if (logFiles != null && logFiles.size() >= 8) {
                    logFiles.get(7).delete();
                }
                File dir = getCrashLogFile();
                fos = new FileOutputStream(dir);
                fos.write(sb.toString().getBytes("UTF-8"));
            }
        } catch (Exception e) {
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private File getCrashLogFile() {
        File logFile = new File(LogDir);
        if (!logFile.exists()) {
            logFile.mkdirs();
        }
        File walletNameFile = new File(logFile, "log_" + formatter.format(new Date()));
        if (!walletNameFile.exists()) {
            try {
                walletNameFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return walletNameFile;
    }

    public List<File> getCrashLogFileList() {
        File logFile = new File(LogDir);
        if (!logFile.exists()) {
            return null;
        }
        File[] fileList = logFile.listFiles();
        List<File> logFileList = Arrays.asList(fileList);
        Collections.sort(logFileList);
        Collections.reverse(logFileList);
        return logFileList;
    }

    public String getCrashLog(File file) {
        String encoding = "UTF-8";
        Long fileLength = file.length();
        byte[] log = new byte[fileLength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(log);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            return new String(log, encoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
