package com.technest.smartled.data.transport

import android.content.Context

private var applicationContext: Context? = null

fun initializeAndroidBleTransport(context: Context) {
    applicationContext = context.applicationContext
}

actual fun createDefaultTransportConnection(): TransportConnection {
    val context = applicationContext
    val transport = if (context != null) {
        AndroidBleTransport(context)
    } else {
        UnsupportedTransport("Android without initialized Context")
    }
    return TransportConnection(transport = transport, scanner = transport)
}
