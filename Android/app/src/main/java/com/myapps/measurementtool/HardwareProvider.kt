package com.myapps.measurementtool

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI


// Class untuk berkomunikasi dengan hardware melalui protokol websocket
class HardwareProvider(serverUri: URI?) : WebSocketClient(serverUri) {

    var connectionChangedCallback: ((status: ConnectionStatus) -> Unit)? = null
    var reciveMessageCallback: ((msg: String?) -> Unit)? = null
    
    // Fungsi untuk menghugkan dengan hardware 
    override fun connect() {
        connectionChangedCallback?.invoke(ConnectionStatus.CONNECTING)
        Log.d("HardProvider", "Connecting")
        super.connect()
    }

    // Event saat koneksi telah dibuka 
    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.d("HardProvider", "Connection Opened")
        connectionChangedCallback?.invoke(ConnectionStatus.CONNECTED)
    }

    // Event saat koneksi telah ditutup
    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.d("HardProvider", "Connection Closed")
        connectionChangedCallback?.invoke(ConnectionStatus.DISCONNECT)
    }

    // Event saat mendapat pesan dari hardware
    override fun onMessage(message: String?) {
        Log.d("HardProvider", "Got Message: "+message.toString())
        reciveMessageCallback?.invoke(message)
    }

    // Event saat terjadi error
    override fun onError(ex: Exception?) {
        Log.e("HardProvider", ex?.message.toString())
        connectionChangedCallback?.invoke(ConnectionStatus.DISCONNECT)
    }

    // Fungsi meminta data jarak 
    fun scanDistance(){
        // Apabila koneksi telah dibuka 
        if (this.isOpen)
            // Kirimkan pesan ping
            this.send("ping")
    }

    // Fungsi saat membersihkan/menghentikan koneksi 
    fun onDestroy(){
        // Apabila koneksi telah dibuka 
        if (this.isOpen)
            // Tutup koneksi 
            this.close()
    }

}

