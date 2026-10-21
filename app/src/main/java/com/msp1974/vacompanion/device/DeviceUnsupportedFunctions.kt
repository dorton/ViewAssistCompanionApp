package com.msp1974.vacompanion.device

import android.os.Build

enum class FunctionClasses {
    AUDIO_ENHANCEMENTS,
    // Built-in mic HAL ignores the requested format and always delivers 48kHz stereo
    MIC_NATIVE_48K_STEREO,
}

class IssueDevice(
    var make: String,
    var model: String,
    var issues: List<FunctionClasses>,

)

class UnsupportedFunctionsDevice {

    companion object {
        private val issueDevices = listOf(
            IssueDevice(
                make = "lenovo",
                model = "tb-8505fs",
                issues = listOf(FunctionClasses.AUDIO_ENHANCEMENTS)
            ),
            // Portal+ has own very good audio handling
            IssueDevice(
                make = "facebook",
                model = "portal+",
                issues = listOf(FunctionClasses.AUDIO_ENHANCEMENTS)
            ),
            // Lenovo Smart Clock 2 - built-in mic returns 48kHz stereo whatever is requested,
            // so 16kHz mono capture comes back 6x slowed down
            IssueDevice(
                make = "lenovo",
                model = "lenovocd-24502f",
                issues = listOf(FunctionClasses.MIC_NATIVE_48K_STEREO)
            ),
        )


        fun isIssueDevice(functionClass: FunctionClasses): Boolean {
            val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
            val model = Build.MODEL.orEmpty().lowercase()

            for (device in issueDevices) {
                if (functionClass in device.issues) {
                    if (manufacturer == device.make && model == device.model) {
                        return true
                    }
                }
            }
            return false
        }
    }
}