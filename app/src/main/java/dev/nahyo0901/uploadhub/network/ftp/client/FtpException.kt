package dev.nahyo0901.uploadhub.network.ftp.client

class FtpException(message: String, val response: FtpResponse? = null) : Exception(message)