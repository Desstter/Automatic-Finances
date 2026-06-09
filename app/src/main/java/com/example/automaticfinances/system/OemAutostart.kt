package com.example.automaticfinances.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Deep-links to the OEM "auto-start / background launch" management screen.
 *
 * Aggressive Chinese-OEM skins (MIUI/HyperOS, EMUI/Magic OS, ColorOS, FuntouchOS/OriginOS, OxygenOS)
 * gate whether a sideloaded app may start itself in the background behind a vendor-specific toggle
 * that is SEPARATE from Android's battery-optimization exemption. There is no API to grant it or even
 * read its state — only the user can flip it, in the vendor's own Settings app. The best an app can do
 * (which is exactly what WhatsApp/Telegram do on these phones) is take the user straight to that
 * screen. Apps that "just work" out of the box on these devices are pre-whitelisted in the ROM by the
 * vendor; that path is not available to a sideloaded APK.
 *
 * Used by the first-run onboarding (no ADB, taps only). Every launch is wrapped so a missing/renamed
 * vendor activity never crashes the app — it just falls through to the next candidate and finally to
 * this app's system details page (from which battery/auto-start are reachable on every OEM).
 */
object OemAutostart {

    private val AGGRESSIVE_BRANDS = listOf(
        "xiaomi", "redmi", "poco", "huawei", "honor", "oppo", "realme",
        "vivo", "iqoo", "oneplus", "samsung", "meizu", "letv", "asus", "nubia",
    )

    /**
     * Candidate vendor activities, tried in order. Only the activity belonging to the device's own
     * skin is installed, so at most one launches; the rest throw and are skipped. Keep these in sync
     * with the `<queries>` package list in AndroidManifest.xml or Android 11+ visibility rules will
     * hide them and the launch will silently fall through to the app-details fallback.
     */
    private val CANDIDATES: List<ComponentName> = listOf(
        // Xiaomi / Redmi / POCO (MIUI / HyperOS)
        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        // Huawei / Honor (EMUI / Magic OS)
        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
        // Oppo / Realme (ColorOS)
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
        ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"),
        // Vivo / iQOO (FuntouchOS / OriginOS)
        ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
        ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
        ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
        // OnePlus (OxygenOS)
        ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
        // Samsung (Device care → battery; no true autostart, but the "sleeping apps" gate lives here)
        ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
    )

    /** Packages referenced by [CANDIDATES] — mirror these in the manifest `<queries>` block. */
    val MANAGED_PACKAGES = CANDIDATES.map { it.packageName }.distinct()

    /**
     * True when the device is a skin known to gate background auto-start behind a vendor toggle, so the
     * onboarding should surface the extra step. Stock Android (Pixel, Motorola, etc.) returns false —
     * there the foreground-service anchor + battery exemption are enough and no extra step is shown.
     */
    fun isRelevant(): Boolean {
        val id = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
        return AGGRESSIVE_BRANDS.any { id.contains(it) }
    }

    /**
     * Opens the most specific auto-start screen available, falling back to this app's system details
     * page. Returns true if anything launched (the fallback essentially always succeeds). Call from a
     * UI/Activity context.
     */
    fun open(context: Context): Boolean {
        for (component in CANDIDATES) {
            val intent = Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                // Not this OEM (or activity renamed) — try the next candidate.
            }
        }
        return openAppDetails(context)
    }

    private fun openAppDetails(context: Context): Boolean = try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (_: Exception) {
        false
    }
}
