package com.twice.whatislove.uploadhub

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class SimpleFileSystemAdapter(private val rootPath: Path) : FileSystemAdapter {
    override fun list(path: Path): List<FileEntry> {
        val p = rootPath.resolve(path).normalize()
        if (!Files.exists(p) || !Files.isDirectory(p)) return emptyList()
        return Files.list(p).use { stream ->
            stream.map { p2 ->
                FileEntry(
                    name = p2.fileName.toString(),
                    isDirectory = Files.isDirectory(p2),
                    size = Files.size(p2)
                )
            }.toList()
        }
    }

    override fun openRead(path: Path): InputStream {
        val p = rootPath.resolve(path).normalize()
        return Files.newInputStream(p, StandardOpenOption.READ)
    }

    override fun openWrite(path: Path): OutputStream {
        val p = rootPath.resolve(path).normalize()
        Files.createDirectories(p.parent)
        return Files.newOutputStream(p, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    override fun exists(path: Path): Boolean {
        return Files.exists(rootPath.resolve(path).normalize())
    }

    override fun resolve(path: Path, child: String): Path {
        return path.resolve(child).normalize()
    }

    override fun root(): Path = rootPath
}