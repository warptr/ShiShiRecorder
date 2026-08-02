package com.yepgoryo.CaptureCap

import android.os.Build
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(bundle: Bundle?, key: String?) {
        preferenceManager.setSharedPreferencesName(GlobalProperties.PREFERENCES_ALIAS)
        setPreferencesFromResource(R.xml.settings, key)
        val preferenceFindPreference: Preference = findPreference("floatingcontrols")!!
        val preferenceFindPreference2: Preference = findPreference("floatingcontrolsposition")!!
        val preferenceFindPreference3: Preference = findPreference("floatingcontrolssize")!!
        val preferenceFindPreference4: Preference = findPreference("floatingcontrolsopacity")!!
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val preferenceCategory: PreferenceCategory = findPreference("controlssettings")!!
            preferenceCategory.removePreference(preferenceFindPreference)
            preferenceCategory.removePreference(preferenceFindPreference2)
            preferenceCategory.removePreference(preferenceFindPreference3)
            preferenceCategory.removePreference(preferenceFindPreference4)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val preferenceFindPreference5: Preference = findPreference("codecvalue")!!
            val preferenceFindPreference6: Preference = findPreference("audiocodecvalue")!!
            val preferenceFindPreference7: Preference = findPreference("selectaudiosources")!!
            val preferenceFindPreference8: Preference = findPreference("drawoverlay")!!
            val preferenceFindPreference9: Preference = findPreference("drawoverlaycontents")!!
            val preferenceFindPreference10: Preference = findPreference("drawoverlayerasehorizontal")!!
            val preferenceFindPreference11: Preference = findPreference("drawoverlayerasevertical")!!
            val preferenceFindPreference12: Preference = findPreference("soundcontrolnotification")!!
            val preferenceFindPreference13: Preference = findPreference("screenorientation")!!

            val preferenceCategory2: PreferenceCategory = findPreference("capturesettings")!!
            preferenceCategory2.removePreference(preferenceFindPreference5)
            preferenceCategory2.removePreference(preferenceFindPreference6)
            preferenceCategory2.removePreference(preferenceFindPreference7)
            preferenceCategory2.removePreference(preferenceFindPreference8)
            preferenceCategory2.removePreference(preferenceFindPreference9)
            preferenceCategory2.removePreference(preferenceFindPreference10)
            preferenceCategory2.removePreference(preferenceFindPreference11)
            preferenceCategory2.removePreference(preferenceFindPreference12)
            preferenceCategory2.removePreference(preferenceFindPreference13)
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            val preferenceFindPreference14: Preference = findPreference("enablevibration")!!
            val preferenceCategory3: PreferenceCategory = findPreference("capturesettings")!!
            preferenceCategory3.removePreference(preferenceFindPreference14)
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference.key.contentEquals("qualityscale")) {
            val qualityDialogFragmentNewInstance: QualityDialogFragment = QualityDialogFragment.newInstance(preference.key)
            qualityDialogFragmentNewInstance.setTargetFragment(this, 0)
            qualityDialogFragmentNewInstance.setKeyName("qualityscale")
            qualityDialogFragmentNewInstance.show(requireFragmentManager(), null)
            return
        }
        if (preference.key.contentEquals("floatingcontrolsopacity")) {
            val panelOpacityDialogFragmentNewInstance: PanelOpacityDialogFragment = PanelOpacityDialogFragment.newInstance(preference.key)
            panelOpacityDialogFragmentNewInstance.setTargetFragment(this, 0)
            panelOpacityDialogFragmentNewInstance.setKeyName("floatingcontrolsopacity")
            panelOpacityDialogFragmentNewInstance.show(requireFragmentManager(), null)
            return
        }
        if (preference.key.contentEquals("timerseconds")) {
            val timerSecondsDialogFragmentNewInstance: TimerDialogFragment = TimerDialogFragment.newInstance(preference.key)
            timerSecondsDialogFragmentNewInstance.setTargetFragment(this, 0)
            timerSecondsDialogFragmentNewInstance.setKeyName("timerseconds")
            timerSecondsDialogFragmentNewInstance.show(requireFragmentManager(), null)
            return
        }
        super.onDisplayPreferenceDialog(preference)
    }
}
