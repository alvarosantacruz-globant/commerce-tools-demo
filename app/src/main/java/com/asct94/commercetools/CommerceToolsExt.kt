package com.asct94.commercetools

import io.vrap.rmf.base.client.ApiHttpResponse
import io.vrap.rmf.base.client.ApiMethod
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers

suspend fun <T : ApiMethod<T, TResult>, TResult> ApiMethod<T, TResult>.makeCall(): TResult {
    val apiMethod = this
    return suspendCoroutine { cont ->
        try {
            val result = with(Dispatchers.IO) {
                val future: CompletableFuture<ApiHttpResponse<TResult>> = apiMethod.execute()
                val apiResult = future.get()
                apiResult.body
            }
            cont.resume(result)
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }
}