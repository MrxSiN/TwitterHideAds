package my.MrxSiN.twitterhideads;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

/** Exact render-hook mappings retained as a zero-scan fast path. */
final class CompatibilityProfile {
    static final String TARGET_PACKAGE = "com.twitter.android";

    private static final Profile X_12_7_1 = new Profile(
            "x-12.7.1",
            "12.7.1",
            "com.x.urt.items.post.a6",
            "com.x.urt.items.post.a6$a",
            "com.x.urt.items.post.c7",
            "e",
            "com.x.urt.items.post.e",
            "a",
            "com.x.urt.items.post.c7",
            "a"
    );

    private static final Profile X_12_8_0 = new Profile(
            "x-12.8.0",
            "12.8.0",
            "com.x.urt.items.post.w5",
            "com.x.urt.items.post.w5$a",
            "com.x.urt.items.post.d7",
            "e",
            "com.x.urt.items.post.e",
            "a",
            "com.x.urt.items.post.d7",
            "a"
    );

    private CompatibilityProfile() {
    }

    static DetectedVersion detect(Context context) {
        if (context == null) {
            return DetectedVersion.UNKNOWN;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(TARGET_PACKAGE, 0);

            String versionName = packageInfo.versionName;
            long versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? packageInfo.getLongVersionCode()
                    : packageInfo.versionCode;

            return new DetectedVersion(versionName, versionCode);
        } catch (Throwable ignored) {
            return DetectedVersion.UNKNOWN;
        }
    }

    /**
     * X 12.9.1 is intentionally excluded. The adaptive resolver rediscovers its
     * renamed boundary without using s6.e.
     */
    static Profile selectExact(DetectedVersion version) {
        if (version == null || version.versionName == null) {
            return null;
        }
        if (matchesPrefix(version.versionName, X_12_8_0.versionPrefix)) {
            return X_12_8_0;
        }
        if (matchesPrefix(version.versionName, X_12_7_1.versionPrefix)) {
            return X_12_7_1;
        }
        return null;
    }

    private static boolean matchesPrefix(String rawName, String prefix) {
        String name = rawName.trim();
        return name.equals(prefix)
                || name.startsWith(prefix + "-")
                || name.startsWith(prefix + ".")
                || name.startsWith(prefix + "+")
                || name.startsWith(prefix + " ");
    }

    static final class Profile {
        final String id;
        final String versionPrefix;
        final String postInterface;
        final String expectedRenderModel;
        final String primaryClass;
        final String primaryMethod;
        final String secondaryClass;
        final String secondaryMethod;
        final String tertiaryClass;
        final String tertiaryMethod;

        Profile(
                String id,
                String versionPrefix,
                String postInterface,
                String expectedRenderModel,
                String primaryClass,
                String primaryMethod,
                String secondaryClass,
                String secondaryMethod,
                String tertiaryClass,
                String tertiaryMethod
        ) {
            this.id = id;
            this.versionPrefix = versionPrefix;
            this.postInterface = postInterface;
            this.expectedRenderModel = expectedRenderModel;
            this.primaryClass = primaryClass;
            this.primaryMethod = primaryMethod;
            this.secondaryClass = secondaryClass;
            this.secondaryMethod = secondaryMethod;
            this.tertiaryClass = tertiaryClass;
            this.tertiaryMethod = tertiaryMethod;
        }
    }

    static final class DetectedVersion {
        static final DetectedVersion UNKNOWN = new DetectedVersion(null, -1L);

        final String versionName;
        final long versionCode;

        DetectedVersion(String versionName, long versionCode) {
            this.versionName = versionName;
            this.versionCode = versionCode;
        }

        String displayName() {
            return versionName == null || versionName.isEmpty()
                    ? "unknown"
                    : versionName;
        }

        String displayCode() {
            return versionCode < 0L ? "unknown" : Long.toString(versionCode);
        }
    }
}
