package com.firefly.oshe.lunli.settings

import com.firefly.oshe.lunli.GlobalInterface.settings.SettingsRegistry

private val settings get() = SettingsRegistry.get()

var LAST_APP_VERSION: Int
    get() = settings._LAST_APP_VERSION
    set(value) {
        settings._LAST_APP_VERSION = value
    }

var CACHED_APP_VERSION: Int
    get() = settings._CACHED_APP_VERSION
    set(value) {
        settings._CACHED_APP_VERSION = value
    }

var SAVED_IGNORE_APP_VERSION: Int
    get() = settings._SAVED_IGNORE_APP_VERSION
    set(value) {
        settings._SAVED_IGNORE_APP_VERSION = value
    }

var SAVED_ANN_VERSION: Int
    get() = settings._SAVED_ANN_VERSIOON
    set(value) {
        settings._SAVED_ANN_VERSIOON = value
    }
