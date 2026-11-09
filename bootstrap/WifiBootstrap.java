// SPDX-License-Identifier: GPL-3.0-or-later
package studio.astratale.xingyuhelper.bootstrap;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WifiBootstrap {
    private static final Pattern BSSID = Pattern.compile("(?:[0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}");

    public static void main(String[] args) {
        try {
            System.out.println(initialize());
        } catch (ReflectiveOperationException | PackageManager.NameNotFoundException | RuntimeException error) {
            System.out.println("CHECK_FAILED");
        }
        // ActivityThread starts a looper for the system context.
        System.exit(0);
    }

    @SuppressWarnings("deprecation")
    private static String initialize() throws ReflectiveOperationException, PackageManager.NameNotFoundException {
        Resources resources = Resources.getSystem();
        int confirmationId = resources.getIdentifier("config_customAdbWifiNetworkConfirmationComponent", "string", "android");
        if (confirmationId == 0) return "SYSTEM";
        ComponentName confirmation = ComponentName.unflattenFromString(resources.getString(confirmationId));
        if (confirmation == null) return "SYSTEM";

        if (Looper.myLooper() == null) Looper.prepareMainLooper();
        Class<?> activityThread = Class.forName("android.app.ActivityThread");
        Object thread = activityThread.getMethod("systemMain").invoke(null);
        Context system = (Context) activityThread.getMethod("getSystemContext").invoke(thread);
        Context shell = system.createPackageContext("com.android.shell", 0);
        PackageManager packages = shell.getPackageManager();
        Intent intent = new Intent().setComponent(confirmation);
        if (packages.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null ||
                packages.resolveService(intent, 0) != null) return "SYSTEM";
        // The shell context above belongs to the owner. Leave other profiles to their system flow.
        if ((int) Class.forName("android.app.ActivityManager").getMethod("getCurrentUser").invoke(null) != 0) return "SYSTEM";

        try {
            return allowOnCurrentWifi(shell);
        } catch (ReflectiveOperationException | IOException | RuntimeException error) {
            return "UNAVAILABLE";
        }
    }

    @SuppressWarnings("deprecation")
    private static String allowOnCurrentWifi(Context shell) throws ReflectiveOperationException, IOException {
        IBinder binder = (IBinder) Class.forName("android.os.ServiceManager").getMethod("getService", String.class).invoke(null, "adb");
        Class<?> managerType = Class.forName("android.debug.IAdbManager");
        Object manager = Class.forName("android.debug.IAdbManager$Stub").getMethod("asInterface", IBinder.class).invoke(null, binder);
        if (manager == null) return "UNAVAILABLE";
        Method port = managerType.getMethod("getAdbWirelessPort");
        if ((int) port.invoke(manager) > 0) return "READY";

        WifiInfo wifi = shell.getSystemService(WifiManager.class).getConnectionInfo();
        if (wifi == null || wifi.getSupplicantState() != SupplicantState.COMPLETED) return "NO_WIFI";
        String accessPoint = wifi.getBSSID();
        if (!usableBssid(accessPoint)) {
            // Some ROMs redact WifiInfo. Only use a dump record that matches both the primary connection and its SSID.
            String dump = readWifiCommand("dumpsys", "wifi");
            String masked = null;
            boolean primary = false;
            for (String rawLine : dump.split("\n")) {
                String line = rawLine.trim();
                if (line.startsWith("mWifiInfo ")) {
                    primary = line.contains("Supplicant state: COMPLETED,") && line.matches(".*, IsPrimary: 1(?:,.*)?");
                } else if (primary && line.startsWith("mLastBssid ")) {
                    masked = line.substring("mLastBssid ".length()).trim();
                    break;
                }
            }
            if (usableBssid(masked)) {
                accessPoint = masked;
            } else {
                if (masked == null) return "UNKNOWN_AP";
                Matcher parts = Pattern.compile("(?i)^([0-9a-f]{2}:[0-9a-f]{2}:)\\*\\*:\\*\\*(:[0-9a-f]{2}:[0-9a-f]{2})$").matcher(masked);
                Matcher network = Pattern.compile("(?m)^Wifi is connected to (\"[^\\r\\n]+\")$").matcher(readWifiCommand("cmd", "wifi", "status"));
                if (!parts.matches() || !network.find()) return "UNKNOWN_AP";
                Matcher candidates = Pattern.compile("(?im)^\\s*" + Pattern.quote(network.group(1)) + "("
                        + Pattern.quote(parts.group(1)) + "[0-9a-f]{2}:[0-9a-f]{2}" + Pattern.quote(parts.group(2))
                        + "): -?\\d+\\s*$").matcher(dump);
                Set<String> addresses = new HashSet<>();
                while (candidates.find()) addresses.add(candidates.group(1).toLowerCase(Locale.ROOT));
                if (addresses.size() != 1) return "UNKNOWN_AP";
                accessPoint = addresses.iterator().next();
            }
        }

        managerType.getMethod("allowWirelessDebugging", boolean.class, String.class).invoke(manager, true, accessPoint);
        long deadline = SystemClock.elapsedRealtime() + 6000;
        do {
            if ((int) port.invoke(manager) > 0) return "READY";
            SystemClock.sleep(200);
        } while (SystemClock.elapsedRealtime() < deadline);
        return "NO_LISTENER";
    }

    private static boolean usableBssid(String value) {
        return value != null && BSSID.matcher(value).matches() &&
                !value.equals("02:00:00:00:00:00") && !value.equals("00:00:00:00:00:00");
    }

    private static String readWifiCommand(String... arguments) throws IOException {
        Process process = new ProcessBuilder(arguments).redirectErrorStream(true).start();
        try (BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder text = new StringBuilder();
            for (String line; (line = output.readLine()) != null; ) text.append(line).append('\n');
            try {
                if (process.waitFor() != 0) throw new IOException(arguments[0] + " failed");
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IOException(arguments[0] + " interrupted", error);
            }
            return text.toString();
        } finally {
            process.destroy();
        }
    }
}
