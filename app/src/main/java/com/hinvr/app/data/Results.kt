package com.hinvr.app.data

import kotlinx.coroutines.CancellationException

/**
 * [runCatching] for suspend work. A cancelled coroutine (screen left, scope closed)
 * must stay cancelled, so [CancellationException] is rethrown instead of becoming a failure.
 */
inline fun <T> attempt(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
