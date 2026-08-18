package com.openclassrooms.rebonnte.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.data.repository.StockErrorReason
import com.openclassrooms.rebonnte.data.repository.StockException

/**
 *
 * Message to display with the arguments.
 * Example: 10 items left in stock.
 */
@Immutable
data class UiMessage(
    @StringRes val res: Int,
    val args: List<Any> = emptyList()
)

/**
 * messages instructing the user on what to do in the event of an error
 */
fun Throwable.toUiMessage(): UiMessage {
    val failure = this as? StockException
    return when (failure?.reason) {
        StockErrorReason.PERMISSION -> UiMessage(R.string.error_permission)
        StockErrorReason.NETWORK -> UiMessage(R.string.error_network)
        StockErrorReason.UNAVAILABLE -> UiMessage(R.string.error_unavailable)

        StockErrorReason.INSUFFICIENT_STOCK -> UiMessage(
            res = R.string.error_insufficient_stock,
            args = listOf(failure.available ?: 0)
        )

        StockErrorReason.UNKNOWN, null -> UiMessage(R.string.error_generic)
    }
}


@StringRes
fun Throwable.toMessageRes(): Int = toUiMessage().res
