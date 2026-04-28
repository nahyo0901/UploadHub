package com.twice.whatislove.uploadhub.network.ftp.client.engine

data class FtpResponse(
    val code: Int,
    val message: String
) {
    fun isPositive(): Boolean = code in 200..399
}