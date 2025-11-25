package com.twice.whatislove.uploadhub.network.ftp.server

import java.io.File
import java.io.InputStream
import java.io.OutputStream

interface FileSystemAdapter {
    fun list(dir: File): List<FileEntry>
    fun openRead(file: File): InputStream
    fun openWrite(file: File): OutputStream
    fun exists(file: File): Boolean
    fun resolve(dir: File, child: String): File
    fun root(): File
}

data class FileEntry(val name: String, val isDirectory: Boolean, val size: Long)