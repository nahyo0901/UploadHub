package com.twice.whatislove.uploadhub.network.ftp.client

data class FtpResponse(val code: Int, val message: String) {
    override fun toString(): String = "$code $message"
}