package com.thesis.lumine.data.api

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Tells the UI that the session ended outside a user action (the refresh
 * token was rejected). Conflated, so the event waits for a collector instead
 * of being dropped, and repeated expiries collapse into one.
 */
object SessionEvents {

    private val expiredChannel = Channel<Unit>(Channel.CONFLATED)

    val expired: Flow<Unit> = expiredChannel.receiveAsFlow()

    fun notifyExpired() {
        expiredChannel.trySend(Unit)
    }
}
