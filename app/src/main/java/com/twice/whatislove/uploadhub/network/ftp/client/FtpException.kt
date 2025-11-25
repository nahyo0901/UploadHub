package com.twice.whatislove.uploadhub.network.ftp.client

class FtpException(message: String, val response: FtpResponse? = null) : Exception(message)