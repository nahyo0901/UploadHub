package com.twice.whatislove.uploadhub.rclone

import java.io.BufferedReader
import java.io.InputStreamReader

object RcloneModule {
    fun run(args: List<String>): String {
        val cmd = mutableListOf("rclone")
        cmd.addAll(args)
        val pb = ProcessBuilder(cmd)
        pb.redirectErrorStream(true)
        val process = pb.start()
        val out = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                out.append(line).append('\n')
            }
        }
        process.waitFor()
        return out.toString()
    }
}
