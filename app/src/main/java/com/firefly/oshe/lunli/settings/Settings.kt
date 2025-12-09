package com.firefly.oshe.lunli.settings

import com.firefly.oshe.lunli.settings.interfaces.SettingsRegistry

private val settings get() = SettingsRegistry.get()

var ANNOUNCEMENT_DONE: Boolean
    get() = settings._ANNOUNCEMENT_DONE
    set(value) {
        settings._ANNOUNCEMENT_DONE = value
    }
