package my.MrxSiN.twitterhideads;

import de.robv.android.xposed.XposedHelpers;

/** Exact render-hook mappings validated for supported X releases. */
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

    static DetectedVersion detect(Object context) {
        if (context == null) {
            return DetectedVersion.UNKNOWN;
        }
        try {
            Object packageManager = XposedHelpers.callMethod(context, "getPackageManager");
            Object packageInfo = XposedHelpers.callMethod(
                    packageManager,
                    "getPackageInfo",
                    TARGET_PACKAGE,
                    0
            );

            Object rawName = XposedHelpers.getObjectField(packageInfo, "versionName");
            String versionName = rawName == null ? null : String.valueOf(rawName);

            long versionCode = -1L;
            try {
                Object rawCode = XposedHelpers.callMethod(packageInfo, "getLongVersionCode");
                if (rawCode instanceof Number) {
                    versionCode = ((Number) rawCode).longValue();
                }
            } catch (Throwable ignored) {
                Object rawCode = XposedHelpers.getObjectField(packageInfo, "versionCode");
                if (rawCode instanceof Number) {
                    versionCode = ((Number) rawCode).longValue();
                }
            }

            return new DetectedVersion(versionName, versionCode);
        } catch (Throwable ignored) {
            return DetectedVersion.UNKNOWN;
        }
    }

    static Profile select(DetectedVersion version) {
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
