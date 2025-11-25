package com.twice.whatislove.uploadhub

fun main() {
    val server = Server(
        socketProvider = DefaultSocketProvider(port = 2121),
        fileSystem = SimpleFileSystemAdapter(rootPath = java.nio.file.Paths.get("."))
    )
    Runtime.getRuntime().addShutdownHook(Thread { server.stop() })
    server.start()
}