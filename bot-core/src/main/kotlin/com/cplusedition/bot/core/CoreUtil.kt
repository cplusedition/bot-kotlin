package com.cplusedition.bot.core

object CoreUt : CoreUtil()

open class CoreUtil {
}

/**
 * A simple monotonic serial counter that wraps on overflow.
 * By default, it count from 0 inclusive to Long.MAX_VALUE exclusive.
 */
open class Serial(
        protected val start: Long = 0L,
        protected val end: Long = Long.MAX_VALUE,
        protected var serial: Long = start
) {
    fun get(): Long {
        synchronized(this) {
            if (serial < start || serial == end) serial = start
            return serial++
        }
    }

    fun reset() {
        synchronized(this) {
            serial = start
        }
    }
}
