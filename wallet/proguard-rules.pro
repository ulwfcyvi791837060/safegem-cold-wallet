-dontskipnonpubliclibraryclasses
-dontoptimize
-dontpreverify
-dontobfuscate
-verbose
-ignorewarning

-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keepattributes *Annotation*

-keep class  com.ccore.jni.** { *; }

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keep class android.**.** { *; }

-keepclassmembers,includedescriptorclasses public class * extends android.view.View {
    void set*(***);
    *** get*();
}

-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

# android-support
-dontwarn android.support.**
-dontnote android.support.**
-keep class android.support.** { *; }
-keep class android.support.v7.widget.RoundRectDrawable { *; }
-keep class android.support.v7.widget.SearchView { *; }

# android-support-design
-keep class android.support.design.widget.** { *; }
-keep interface android.support.design.widget.** { *; }
-dontwarn android.support.design.**

# Guava
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.ClassValue
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.codehaus.mojo.**
-dontnote com.google.common.reflect.**
-dontnote com.google.appengine.**
-dontnote com.google.apphosting.**
-dontnote com.google.common.cache.Striped64,com.google.common.cache.Striped64$Cell
-dontnote com.google.common.util.concurrent.AbstractFuture$UnsafeAtomicHelper

# slf4j
-dontwarn org.slf4j.MDC
-dontwarn org.slf4j.MarkerFactory

# zxing
-dontwarn com.google.zxing.common.BitMatrix

#eventbus
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

# logback-android
-dontwarn javax.mail.**
-dontnote ch.qos.logback.core.rolling.helper.FileStoreUtil

-keep class com.mcxtzhang.swipemenulib.** { *; }
-dontwarn com.mcxtzhang.**

-keep class com.andrognito.patternlockview.** { *; }
-dontwarn com.andrognito.patternlockview.**

-keep class org.web3j.** { *; }

-keep interface org.web3j.** { *; }

-keep class com.bankledger.safecold.utils.** { *; }

-keep class com.bankledger.safecold.db.** { *; }

-keep class io.eblock.eos4j.** { *; }

-keepclassmembers class com.bankledger.safecold.SafeColdApplication {
   public *;
}