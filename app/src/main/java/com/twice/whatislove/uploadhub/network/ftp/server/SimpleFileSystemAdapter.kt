package com.twice.whatislove.uploadhub.network.ftp.server

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class SimpleFileSystemAdapter(private val rootPath: File) : FileSystemAdapter {
    private fun resolveToRoot(file: File): File {
        val resolved = if (file.isAbsolute) file else File(rootPath, file.path)
        return try {
            // canonicalFile helps avoid simple ../ escapes
            resolved.canonicalFile
        } catch (_: Exception) {
            resolved.absoluteFile
        }
    }

    override fun list(dir: File): List<FileEntry> {
        val target = resolveToRoot(dir)
        if (!target.exists() || !target.isDirectory) return emptyList()
        val files = target.listFiles() ?: return emptyList()
        return files.map { f ->
            FileEntry(
                name = f.name,
                isDirectory = f.isDirectory,
                size = if (f.isFile) f.length() else 0L
            )
        }
    }

    override fun openRead(file: File): InputStream {
        val target = resolveToRoot(file)
        if (!target.exists() || !target.isFile) {
            throw java.io.FileNotFoundException("File not found: ${target.path}")
        }
        return FileInputStream(target)
    }

    override fun openWrite(file: File): OutputStream {
        val target = resolveToRoot(file)
        target.parentFile?.let { parent ->
            if (!parent.exists()) parent.mkdirs()
        }
        return FileOutputStream(target, false) // overwrite
    }

    override fun exists(file: File): Boolean {
        val target = resolveToRoot(file)
        return target.exists()
    }

    override fun resolve(dir: File, child: String): File {
        val base = resolveToRoot(dir)
        return resolveToRoot(File(base, child))
    }

    override fun root(): File = try {
        rootPath.canonicalFile
    } catch (_: Exception) {
        rootPath.absoluteFile
    }
}