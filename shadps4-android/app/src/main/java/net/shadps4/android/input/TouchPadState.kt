package net.shadps4.android.input

data class TouchPadState(
    var buttons: Int = 0,
    var leftStickX: Float = 0f,
    var leftStickY: Float = 0f,
    var rightStickX: Float = 0f,
    var rightStickY: Float = 0f,
    var l2Trigger: Float = 0f,
    var r2Trigger: Float = 0f
) {
    companion object {
        const val BTN_CROSS = 1 shl 0
        const val BTN_CIRCLE = 1 shl 1
        const val BTN_SQUARE = 1 shl 2
        const val BTN_TRIANGLE = 1 shl 3
        const val BTN_L1 = 1 shl 4
        const val BTN_R1 = 1 shl 5
        const val BTN_L3 = 1 shl 6
        const val BTN_R3 = 1 shl 7
        const val BTN_OPTIONS = 1 shl 8
        const val BTN_TOUCHPAD = 1 shl 9
        const val BTN_DPAD_UP = 1 shl 10
        const val BTN_DPAD_DOWN = 1 shl 11
        const val BTN_DPAD_LEFT = 1 shl 12
        const val BTN_DPAD_RIGHT = 1 shl 13
    }
}
