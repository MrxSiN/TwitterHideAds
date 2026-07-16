package my.MrxSiN.twitterhideads;

import de.robv.android.xposed.XposedHelpers;

/** Compatibility profile bundled for the X release validated during testing. */
final class CompatibilityProfile {
    static final String PROFILE_ID = "x-12.7.1";
    static final String TARGET_PACKAGE = "com.twitter.android";
    static final String TESTED_VERSION_PREFIX = "12.7.1";

    private CompatibilityProfile() {
    }

    static DetectedVersion detect(Object context) {
        if (context == null) {
            return DetectedVersion.UNKNOWN;
        }
        try {
            Object packageManager = XposedHelpers.callMethod(
                    context,
                    "getPackageManager"
            );
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
                Object rawCode = XposedHelpers.callMethod(
                        packageInfo,
                        "getLongVersionCode"
                );
                if (rawCode instanceof Number) {
                    versionCode = ((Number) rawCode).longValue();
                }
            } catch (Throwable ignored) {
                Object rawCode = XposedHelpers.getObjectField(
                        packageInfo,
                        "versionCode"
                );
                if (rawCode instanceof Number) {
                    versionCode = ((Number) rawCode).longValue();
                }
            }

            return new DetectedVersion(versionName, versionCode);
        } catch (Throwable ignored) {
            return DetectedVersion.UNKNOWN;
        }
    }

    static boolean supports(DetectedVersion version) {
        if (version == null || version.versionName == null) {
            return false;
        }
        String name = version.versionName.trim();
        return name.equals(TESTED_VERSION_PREFIX)
                || name.startsWith(TESTED_VERSION_PREFIX + "-")
                || name.startsWith(TESTED_VERSION_PREFIX + ".")
                || name.startsWith(TESTED_VERSION_PREFIX + "+")
                || name.startsWith(TESTED_VERSION_PREFIX + " ");
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
