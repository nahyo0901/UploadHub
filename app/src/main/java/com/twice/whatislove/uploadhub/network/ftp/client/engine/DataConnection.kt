package com.twice.whatislove.uploadhub.network.ftp.client.engine

import java.net.InetSocketAddress
import java.net.Socket

object DataConnection {

    fun openPassiveSocket(response: String): Socket {
        // Parse (h1,h2,h3,h4,p1,p2)
        val regex = Regex("""\((.*?)\)""")
        val match = regex.find(response) ?: throw FtpException("Invalid PASV response")

        val parts = match.groupValues[1].split(",")
        val host = parts.take(4).joinToString(".")
        val port = parts[4].toInt() * 256 + parts[5].toInt()

        val socket = Socket()
        socket.connect(InetSocketAddress(host, port), 10000)
        return socket
    }
}