package com.bankledger.safecold.utils;

import android.text.TextUtils;

import com.bankledger.safecoldj.qrcode.QRCodeUtil;
import com.bankledger.safecoldj.utils.Utils;
import com.ccore.jni.BlockCipherParam;
import com.ccore.jni.DevInfo;
import com.ccore.jni.SKFJni;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author zhangmiao
 * @time 2019/7/02 15:52
 */
public class EncryptionChipManagerV2 {

    public static final Logger logger = LoggerFactory.getLogger(EncryptionChipManagerV2.class);

    private final byte[] lock = new byte[0];

    private String AUTH_KEY = "C*CORE SYS @ SZ ";

    private final int PIN_RETRY = 5;//重试次数

    private long ret = 0;
    private boolean isConnectedDev = false;//是否已连接设备
    private boolean isOpenedApp = false;//是否已打开应用
    private boolean isOpenedContainer = false;//是否已打开应用

    public String APP_NAME = "SAFECOLD";//应用名
    public String FILE_NAME = "SEED";//文件名

    public String NEW_APP_NAME = "NEWSAFE";//应用名
    public String NEW_FILE_NAME = "NEWSEED";//文件名

    private String CON_NAME = "CON_1";//文件名

    private final int USER_RIGHTS = 0x00000010;// FF：所有人权限
    private final int ALL_RIGHTS = 0x000000FF;// FF：所有人权限

    private static EncryptionChipManagerV2 INSTANCE = new EncryptionChipManagerV2();

    private SKFJni skfJni;

    public enum SeedType{
        TYPE_MNEMONIC_SEED(0),
        TYPE_SEED(1);

        private int value;

        SeedType(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }


    private EncryptionChipManagerV2() {
        skfJni = new SKFJni();
    }

    public static EncryptionChipManagerV2 getInstance() {
        return INSTANCE;
    }

    public long saveMnemonicSeed(String encryptedMnemonicSeed, String encryptSeed, String userPIN) {
        StringBuilder sb = new StringBuilder(encryptedMnemonicSeed);
        sb.append(QRCodeUtil.QR_CODE_UNDERLINE);
        sb.append(encryptSeed);
        return writeNewData(sb.toString(), userPIN, NEW_APP_NAME, NEW_FILE_NAME, ALL_RIGHTS);
    }

    public boolean hasMnemonicSeed() {
        return hasNewFile() || hasOldFile();
    }

    public String getEncryptMnemonicSeed(String userPIN, SeedType seedType) {
        String seed;
        if (hasNewFile()) {
            seed = readNewData(NEW_APP_NAME, NEW_FILE_NAME, userPIN);
        } else {
            seed = readData(APP_NAME, FILE_NAME);
        }
        if (seed != null) {
            String[] arr = seed.split(QRCodeUtil.QR_CODE_UNDERLINE);
            if (arr.length == 2) {
                return arr[seedType.getValue()];
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * 写入数据
     *
     * @param data
     * @param pin
     * @param appName
     * @param fileName
     * @param rights
     * @return
     */
    @Deprecated
    public long writeData(String data, String pin, String appName, String fileName, long rights) {
        synchronized (lock) {
            if (checkRet(skfJni.SKF_ConnectDev())) {//连接设备
                isConnectedDev = true;
                if (checkRet(devAuth())) {//认证设备
                    byte[] appNameBytes = new byte[256];
                    Arrays.fill(appNameBytes,(byte)0);
                    Integer appSize = new Integer(0);
                    if (checkRet(skfJni.SKF_EnumApplication(appNameBytes, appSize))) {//枚举应用
                        ArrayList<String> appNameList = splitByZeroToStringList(appNameBytes);
                        if (!appNameList.contains(appName)) {
                            skfJni.SKF_CreateApplication(appName.getBytes(), pin.getBytes(), PIN_RETRY, pin.getBytes(), PIN_RETRY, rights); //创建应用
                        }
                        if (checkRet(skfJni.SKF_OpenApplication(appName.getBytes()))) {//打开应用
                            isOpenedApp = true;
                            byte[] fileNameBytes = new byte[1024];
                            Arrays.fill(fileNameBytes,(byte)0);
                            Integer fileNameSize = new Integer(0);
                            if (checkRet(skfJni.SKF_EnumFiles(fileNameBytes, fileNameSize))) { //枚举文件
                                ArrayList<String> fileNameList = splitByZeroToStringList(fileNameBytes);
                                if (!fileNameList.contains(fileName)) {
                                    skfJni.SKF_CreateFile(fileName.getBytes(), 1024, rights, rights); //创建文件
                                }
                                byte[] dataArr = data.getBytes();
                                byte[] lenArr = new byte[4];
                                Utils.uint32ToByteArrayLE(dataArr.length, lenArr, 0);
                                byte[] writeData = Utils.byteMerger(lenArr, dataArr);
                                if (checkRet(skfJni.SKF_WriteFile(fileName.getBytes(), 0, writeData, writeData.length))) { //写入文件
                                    close();
                                }
                                return ret;
                            }
                        }
                    }
                }
            }
            return ret;
        }
    }

    /**
     * 读取数据
     *
     * @param appName
     * @param fileName
     * @return
     */
    public String readData(String appName, String fileName) {
        synchronized (lock) {
            if (checkRet(skfJni.SKF_ConnectDev())) {//连接设备
                isConnectedDev = true;
                if (checkRet(devAuth())) {//认证设备
                    skfJni.SKF_ClearSecureState();
                    byte[] appNameBytes = new byte[256];
                    Arrays.fill(appNameBytes,(byte)0);
                    Integer appSize = new Integer(0);
                    if (checkRet(skfJni.SKF_EnumApplication(appNameBytes, appSize))) {//枚举应用
                        ArrayList<String> appList = splitByZeroToStringList(appNameBytes);
                        if (appList.contains(appName)) {
                            if (checkRet(skfJni.SKF_OpenApplication(appName.getBytes()))) {//如果有我们的app直接打开
                                isOpenedApp = true;
                                byte[] fileNameBytes = new byte[1024];
                                Arrays.fill(fileNameBytes,(byte)0);
                                Integer fileNameSize = new Integer(0);
                                if (checkRet(skfJni.SKF_EnumFiles(fileNameBytes, fileNameSize))) {
                                    ArrayList<String> fileNameList = splitByZeroToStringList(fileNameBytes);
                                    if (fileNameList.contains(fileName)) {
                                        Integer outDataLen = new Integer(0);
                                        byte[] fileData = new byte[1024];
                                        Arrays.fill(fileData, (byte) 0);
                                        skfJni.SKF_ReadFile(fileName.getBytes(), (long) 0, (long) fileData.length, fileData, outDataLen);
                                        long len = Utils.readUint32(fileData, 0);
                                        if (len <= 0) {
                                            close();
                                            return null;
                                        }
                                        byte[] readArr = new byte[(int) len];
                                        System.arraycopy(fileData, 4, readArr, 0, (int) len);
                                        close();
                                        return new String(readArr).trim();
                                    } else {
                                        close();
                                        return null;
                                    }
                                }
                            }
                        } else {
                            close();
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }

    //认证设备
    private long devAuth() {
        synchronized (lock) {
            if (checkRet(skfJni.SKF_SetSymmKey(AUTH_KEY.getBytes(), 0x00000101))) {
                BlockCipherParam param = new BlockCipherParam();
                param.setIvLen(0);
                param.setPaddingType(0);
                param.setFeedBitLen(0);
                if (checkRet(skfJni.SKF_EncryptInit(param))) {
                    byte[] random = new byte[16];
                    Arrays.fill(random, (byte) 0);
                    if (checkRet(skfJni.SKF_GenRandom(random, 8))) {
                        byte[] encryptRandom = new byte[1024];
                        long[] decryptSize = new long[1];
                        decryptSize[0] = 1024;
                        if (checkRet(skfJni.SKF_Encrypt(random, 16, encryptRandom, decryptSize))) {
                            checkRet(skfJni.SKF_DevAuth(encryptRandom, decryptSize[0]));
                        }
                    }
                }
                skfJni.SKF_CloseHandle();
            }
            return ret;
        }
    }

    private void close() {
        if(isOpenedContainer){
            skfJni.SKF_CloseContainer();
            isOpenedContainer = false;
        }

        if (isOpenedApp) {
            skfJni.SKF_CloseApplication();
            isOpenedApp = false;
        }

        if (isConnectedDev) {
            skfJni.SKF_DisconnectDev();
            isConnectedDev = false;
        }

    }

    private boolean checkRet(long ret) {
        this.ret = ret;
        if (ret == 0) {
            return true;
        } else {
            close();
            return false;
        }
    }

    private ArrayList<String> splitByZeroToStringList(byte[] byteArr) {
        ArrayList<String> arr = new ArrayList<String>();
        byte[] temp = new byte[64];
        for (int i = 0, j = 0; i < byteArr.length; i++) {
            if (byteArr[i] == '\0') {
                String tempStr = new String(temp, 0, j);
                if (tempStr.equals("")) {
                    break;
                }
                arr.add(tempStr);
                Arrays.fill(temp, (byte) 0);
                j = 0;
                continue;
            }
            temp[j++] = byteArr[i];
        }
        return arr;
    }

    public DevInfo getDeviceInfo() {
        DevInfo devInfo = new DevInfo();
        synchronized (lock) {
            if (checkRet(skfJni.SKF_ConnectDev())) {//连接设备
                isConnectedDev = true;
                if (checkRet(devAuth())) {//认证设备
                    checkRet(skfJni.SKF_GetDevInfo(devInfo));
                }
            }
            return devInfo;
        }
    }

    /**
     * 写入数据
     *
     * @param writeData
     * @param pin
     * @param appName
     * @param fileName
     * @param rights
     * @return
     */
    public long writeNewData(String writeData, String pin, String appName, String fileName, long rights) {
        synchronized (lock) {
            if (checkRet(skfJni.SKF_ConnectDev())) {//连接设备
                isConnectedDev = true;
                if (checkRet(devAuth())) {//认证设备
                    byte[] appNameBytes = new byte[256];
                    Arrays.fill(appNameBytes,(byte)0);
                    Integer appSize = new Integer(0);
                    if (checkRet(skfJni.SKF_EnumApplication(appNameBytes, appSize))) {//枚举应用
                        ArrayList<String> appNameList = splitByZeroToStringList(appNameBytes);
                        if (!appNameList.contains(appName)) {
                            skfJni.SKF_CreateApplication(appName.getBytes(), pin.getBytes(), PIN_RETRY, pin.getBytes(), PIN_RETRY, rights); //创建应用
                        }
                        if (checkRet(skfJni.SKF_OpenApplication(appName.getBytes()))) {//打开应用
                            isOpenedApp = true;
                            byte[] containerNameBytes = new byte[256];
                            Arrays.fill(containerNameBytes,(byte)0);
                            Integer containerSize = new Integer(0);
                            if (checkRet(skfJni.SKF_EnumContainer(containerNameBytes, containerSize))) {
                                ArrayList<String> containerNameList = splitByZeroToStringList(containerNameBytes);
                                if (!containerNameList.contains(CON_NAME)) {
                                    skfJni.SKF_CreateContainer(CON_NAME.getBytes());
                                    if (checkRet(skfJni.SKF_OpenContainer(CON_NAME.getBytes()))) {
                                        isOpenedContainer = true;
                                        skfJni.SKF_GenEncrytRSAKeyPair();
                                        writeFileEncrypt(writeData.trim(), fileName, rights);
                                    }
                                } else {
                                    if (checkRet(skfJni.SKF_OpenContainer(CON_NAME.getBytes()))) {
                                        writeFileEncrypt(writeData.trim(), fileName, rights);
                                    }
                                }

                            }
                        }
                    }
                }
            }
            return ret;
        }
    }

    private void writeFileEncrypt(String writeData, String fileName, long rights){
        byte[] fileNameBytes = new byte[1024];
        Arrays.fill(fileNameBytes,(byte)0);
        Integer fileNameSize = new Integer(0);
        if (checkRet(skfJni.SKF_EnumFiles(fileNameBytes, fileNameSize))) { //枚举文件
            ArrayList<String> fileNameList = splitByZeroToStringList(fileNameBytes);
            if (!fileNameList.contains(fileName)) {
                skfJni.SKF_CreateFile(fileName.getBytes(), 1024, rights, rights); //创建文件
            }
            if (checkRet(skfJni.SKF_WriteFile_Encrypt(fileName.getBytes(), 0, writeData.getBytes(), writeData.getBytes().length))) { //写入文件
                close();
            }
        }
    }

    /**
     * 读取数据
     *
     * @param appName
     * @param fileName
     * @return
     */
    public String readNewData(String appName, String fileName, String pin) {
        synchronized (lock) {
            if (checkRet(skfJni.SKF_ConnectDev())) {//连接设备
                isConnectedDev = true;
                if (checkRet(devAuth())) {//认证设备
                    byte[] appNameBytes = new byte[256];
                    Arrays.fill(appNameBytes,(byte)0);
                    Integer appSize = new Integer(0);
                    if (checkRet(skfJni.SKF_EnumApplication(appNameBytes, appSize))) {//枚举应用
                        ArrayList<String> appNameList = splitByZeroToStringList(appNameBytes);
                        if (appNameList.contains(appName)) {
                            if (checkRet(skfJni.SKF_OpenApplication(appName.getBytes()))) {//打开应用
                                isOpenedApp = true;
                                byte[] containerNameBytes = new byte[256];
                                Arrays.fill(containerNameBytes,(byte)0);
                                Integer containerSize = new Integer(0);
                                if (checkRet(skfJni.SKF_EnumContainer(containerNameBytes, containerSize))) { //枚举容器
                                    ArrayList<String> containerNameList = splitByZeroToStringList(containerNameBytes);
                                    if (containerNameList.contains(CON_NAME)) {
                                        if (checkRet(skfJni.SKF_OpenContainer(CON_NAME.getBytes()))) { //打开容器
                                            isOpenedContainer = true;
                                            ret  = skfJni.SKF_ClearSecureState();
                                            Integer pinRetry = new Integer(0);
                                            if (checkRet(skfJni.SKF_VerifyPIN(1, pin.getBytes(), pinRetry))){ //验证PIN
                                                byte[] fileNameBytes = new byte[1024];
                                                Arrays.fill(fileNameBytes,(byte)0);
                                                Integer fileNameSize = new Integer(0);
                                                if (checkRet(skfJni.SKF_EnumFiles(fileNameBytes, fileNameSize))) { //枚举文件
                                                    ArrayList<String> fileNameList = splitByZeroToStringList(fileNameBytes);
                                                    if (fileNameList.contains(fileName)) {
                                                        Integer outDataLen = new Integer(0);
                                                        byte[] fileData = new byte[1025];
                                                        Arrays.fill(fileData, (byte) 0);
                                                        skfJni.SKF_ReadFile_Decrypt(fileName.getBytes(),(long)0, (long) fileData.length, fileData, outDataLen); //读取文件
                                                        close();
                                                        return new String(fileData).trim();
                                                    } else {
                                                        close();
                                                        return null;
                                                    }
                                                }
                                            }

                                        }
                                    } else {
                                        close();
                                        return null;
                                    }
                                }
                            }
                        } else {
                            close();
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 是否写过新文件
     * @return
     */
    public boolean hasNewFile() {
        synchronized (lock) {
            String appName = NEW_APP_NAME;
            String fileName = NEW_FILE_NAME;
            if (checkRet(skfJni.SKF_ConnectDev())) {//连接设备
                isConnectedDev = true;
                if (checkRet(devAuth())) {//认证设备
                    byte[] appNameBytes = new byte[256];
                    Arrays.fill(appNameBytes,(byte)0);
                    Integer appSize = new Integer(0);
                    if (checkRet(skfJni.SKF_EnumApplication(appNameBytes, appSize))) {//枚举应用
                        ArrayList<String> appNameList = splitByZeroToStringList(appNameBytes);
                        if (appNameList.contains(appName)) {
                            if (checkRet(skfJni.SKF_OpenApplication(appName.getBytes()))) {//打开应用
                                isOpenedApp = true;
                                byte[] containerNameBytes = new byte[256];
                                Arrays.fill(containerNameBytes,(byte)0);
                                Integer containerSize = new Integer(0);
                                if (checkRet(skfJni.SKF_EnumContainer(containerNameBytes, containerSize))) { //枚举容器
                                    ArrayList<String> containerNameList = splitByZeroToStringList(containerNameBytes);
                                    if (containerNameList.contains(CON_NAME)) {
                                        if (checkRet(skfJni.SKF_OpenContainer(CON_NAME.getBytes()))) { //打开容器
                                            isOpenedContainer = true;
                                            ret  = skfJni.SKF_ClearSecureState();
                                            byte[] fileNameBytes = new byte[1024];
                                            Arrays.fill(fileNameBytes,(byte)0);
                                            Integer fileNameSize = new Integer(0);
                                            if (checkRet(skfJni.SKF_EnumFiles(fileNameBytes, fileNameSize))) { //枚举文件
                                                ArrayList<String> fileNameList = splitByZeroToStringList(fileNameBytes);
                                                if (fileNameList.contains(fileName)) {
                                                    close();
                                                    return true;
                                                } else {
                                                    close();
                                                    return false;
                                                }
                                            }
                                        }
                                    } else {
                                        close();
                                        return false;
                                    }
                                }
                            }
                        } else {
                            close();
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 是否写过老文件
     * @return
     */
    public boolean hasOldFile() {
        synchronized (lock) {
            String appName = APP_NAME;
            String fileName = FILE_NAME;
            if (checkRet(skfJni.SKF_ConnectDev())) {//连接设备
                isConnectedDev = true;
                if (checkRet(devAuth())) {//认证设备
                    byte[] appNameBytes = new byte[256];
                    Arrays.fill(appNameBytes,(byte)0);
                    Integer appSize = new Integer(0);
                    if (checkRet(skfJni.SKF_EnumApplication(appNameBytes, appSize))) {//枚举应用
                        ArrayList<String> appNameList = splitByZeroToStringList(appNameBytes);
                        if (appNameList.contains(appName)) {
                            if (checkRet(skfJni.SKF_OpenApplication(appName.getBytes()))) {//打开应用
                                isOpenedApp = true;
                                byte[] fileNameBytes = new byte[1024];
                                Arrays.fill(fileNameBytes,(byte)0);
                                Integer fileNameSize = new Integer(0);
                                if (checkRet(skfJni.SKF_EnumFiles(fileNameBytes, fileNameSize))) { //枚举文件
                                    ArrayList<String> fileNameList = splitByZeroToStringList(fileNameBytes);
                                    if (fileNameList.contains(fileName)) {
                                        close();
                                        return true;
                                    } else {
                                        close();
                                        return false;
                                    }
                                }
                            }
                        } else {
                            close();
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void fromOldSeedToNew(String userPIN){
        String seed = readData(APP_NAME, FILE_NAME);
        if (!TextUtils.isEmpty(seed)) {
            deleteApp(NEW_APP_NAME);
            deleteApp(APP_NAME);
            writeNewData(seed, userPIN, NEW_APP_NAME, NEW_FILE_NAME, ALL_RIGHTS);

        }
    }

    public long deleteApp(String appName) {
        synchronized (lock) {
            if (checkRet(skfJni.SKF_ConnectDev())) { //连接设备
                isConnectedDev = true;
                if (checkRet(devAuth())) { //认证设备
                    byte[] appNameBytes = new byte[256];
                    Arrays.fill(appNameBytes,(byte)0);
                    Integer size = new Integer(0);
                    if (checkRet(skfJni.SKF_EnumApplication(appNameBytes, size))) { //枚举应用
                        ArrayList<String> appNameList = splitByZeroToStringList(appNameBytes);
                        if (appNameList.contains(appName)) {
                            skfJni.SKF_DeleteApplication(appName.getBytes()); //删除应用
                            close();
                        } else {
                            close();
                        }
                    } else {
                        close();
                    }
                }
            }
        }
        return ret;
    }

}
