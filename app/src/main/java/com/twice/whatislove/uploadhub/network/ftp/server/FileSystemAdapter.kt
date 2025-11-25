package com.twice.whatislove.uploadhub

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path

interface FileSystemAdapter {
    fun list(path: Path): List<FileEntry>
    fun openRead(path: Path): InputStream
    fun openWrite(path: Path): OutputStream
    fun exists(path: Path): Boolean
    fun resolve(path: Path, child: String): Path
    fun root(): Path
}

data class FileEntry(val name: String, val isDirectory: Boolean, val size: Long)