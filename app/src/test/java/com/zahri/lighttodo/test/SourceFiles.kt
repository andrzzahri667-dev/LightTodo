package com.zahri.lighttodo.test

import java.io.File

fun sourceFile(relativePath: String): File {
    val candidate = sourcePath(relativePath)
    if (candidate.exists()) return candidate
    error("Could not find $relativePath from ${System.getProperty("user.dir")}")
}

fun sourcePath(relativePath: String): File {
    val userDir = requireNotNull(System.getProperty("user.dir"))
    var dir = File(userDir).absoluteFile
    while (true) {
        val candidate = File(dir, relativePath)
        if (candidate.exists()) return candidate
        dir = dir.parentFile ?: break
    }
    return File(userDir, relativePath)
}
