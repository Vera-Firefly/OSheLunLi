package com.firefly.oshe.lunli.GlobalInterface

interface BackListening {
    fun onBackPressed(): Boolean
}

object BackEventPublisher {

    private val listeners = mutableListOf<BackListening>()

    fun registerListener(listener: BackListening) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun unregisterListener(listener: BackListening) {
        listeners.remove(listener)
    }

    fun publishBackEvent(): Boolean {
        for (i in listeners.size - 1 downTo 0) {
            if (listeners[i].onBackPressed()) {
                return true
            }
        }
        return false
    }

    fun clear() {
        listeners.clear()
    }
}