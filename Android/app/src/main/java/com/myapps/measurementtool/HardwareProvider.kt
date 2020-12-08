package com.myapps.measurementtool

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class HardwareProvider(serverUri: URI?) : WebSocketClient(serverUri) {

    var connectionChangedCallback: ((status: ConnectionStatus) -> Unit)? = null
    var reciveMessageCallback: ((msg: String?) -> Unit)? = null

    override fun onOpen(handshakedata: ServerHandshake?) {
        connectionChangedCallback?.invoke(ConnectionStatus.CONNECTED)
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.d("HardProvider", "Connection Closed")
        connectionChangedCallback?.invoke(ConnectionStatus.DISCONNECT)
    }

    override fun onMessage(message: String?) {
        Log.d("HardProvider", "Got Message"+message.toString())
        reciveMessageCallback?.invoke(message)
    }

    override fun onError(ex: Exception?) {
        Log.e("HardwareProvider", ex?.message.toString())
        connectionChangedCallback?.invoke(ConnectionStatus.DISCONNECT)
    }

    fun scanDistance(){
        if (this.isOpen)
            this.send("ping")
    }

}

