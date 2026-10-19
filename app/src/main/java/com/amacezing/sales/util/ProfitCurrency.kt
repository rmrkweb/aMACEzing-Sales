package com.amacezing.sales.util

/** Formats a monetary amount while optionally concealing its numeric value. */
fun Double.formatCurrency(isMasked: Boolean): String =
    if (isMasked) "₱ ••••••" else Formatters.formatCurrency(this)
