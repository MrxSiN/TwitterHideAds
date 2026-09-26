package my.MrxSiN.twitterhideads;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

/**
 * Exact render-hook mappings retained as a zero-scan fast path. Which
 * profile applies to a version is decided by discovery.bf; the class and
 * method names stay here because they are Java-side hook targets.
 */
final class CompatibilityProfile {
    static final String TARGET_PACKAGE = "com.twitter.android";

    private static final Profile X_12_7_1 = new Profile(
            "x-12.7.1",
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
     * The version table and its prefix rule (trimmed name equal to the
     * version, or followed by '-', '.', '+' or ' ') are in discovery.bf
     * (OP_PROFILE). X 12.9.1 is intentionally absent: the adaptive resolver
     * rediscovers its renamed boundary without using s6.e.
     */
    static Profile selectExact(DetectedVersion version) {
        if (version == null || version.versionName == null) {
            return null;
        }
        PolicyFrame frame = PolicyFrame.begin(BfAbi.OP_PROFILE);
        try {
            frame.versionText(version.versionName);
            if (!frame.send(BfAbi.PROG_DISCOVERY, 1)) {
                return null;
            }
            switch (frame.out(0)) {
                case BfAbi.PROFILE_X_12_8_0:
                    return X_12_8_0;
                case BfAbi.PROFILE_X_12_7_1:
                    return X_12_7_1;
                default:
                    return null;
            }
        } finally {
            frame.release();
        }
    }

    static final class Profile {
        final String id;
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
