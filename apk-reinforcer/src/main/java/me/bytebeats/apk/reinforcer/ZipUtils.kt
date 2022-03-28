package me.bytebeats.apk.reinforcer

import java.io.*
import java.util.*
import java.util.zip.*

/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created at 2022/3/28 18:04
 * @Version 1.0
 * @Description TO-DO
 */

private const val BUFFER: Int = 8192

@Throws(IOException::class)
fun compress(srcPath: String, dstPath: String) {
    val srcFile = File(srcPath)
    val dstFile = File(dstPath)
    if (!srcFile.exists()) {
        throw FileNotFoundException("$srcPath Not exist！")
    }
    var out: FileOutputStream? = null
    var zipOut: ZipOutputStream? = null
    try {
        out = FileOutputStream(dstFile)
        val cos = CheckedOutputStream(out, CRC32())
        zipOut = ZipOutputStream(cos)
        val baseDir = ""
        compress(srcFile, zipOut, baseDir)
    } finally {
        if (null != zipOut) {
            zipOut.close()
            out = null
        }
        out?.close()
    }
}

@Throws(IOException::class)
private fun compress(file: File, zipOut: ZipOutputStream, baseDir: String) {
    if (file.isDirectory) {
        compressDirectory(file, zipOut, baseDir)
    } else {
        compressFile(file, zipOut, baseDir)
    }
}

/** 压缩一个目录  */
@Throws(IOException::class)
private fun compressDirectory(dir: File, zipOut: ZipOutputStream, baseDir: String) {
    val files = dir.listFiles()
    files?.forEach { file -> compress(file, zipOut, baseDir + dir.name.toString() + "/") }
}

/** 压缩一个文件  */
@Throws(IOException::class)
private fun compressFile(file: File, zipOut: ZipOutputStream, baseDir: String) {
    if (!file.exists()) {
        return
    }
    var bis: BufferedInputStream? = null
    try {
        bis = BufferedInputStream(FileInputStream(file))
        val entry = ZipEntry(baseDir + file.name)
        zipOut.putNextEntry(entry)
        var count: Int
        val data = ByteArray(BUFFER)
        while (bis.read(data, 0, BUFFER).also { count = it } != -1) {
            zipOut.write(data, 0, count)
        }
    } finally {
        bis?.close()
    }
}

@Throws(IOException::class)
fun decompress(zipFile: String, dstPath: String) {
    val pathFile = File(dstPath)
    if (!pathFile.exists()) {
        pathFile.mkdirs()
    }
    val zip = ZipFile(zipFile)
    val entries: Enumeration<*> = zip.entries()
    while (entries.hasMoreElements()) {
        val entry: ZipEntry = entries.nextElement() as ZipEntry
        val zipEntryName: String = entry.name
        var `in`: InputStream? = null
        var out: OutputStream? = null
        try {
            `in` = zip.getInputStream(entry)
            val outPath = "$dstPath/$zipEntryName".replace("\\*".toRegex(), "/")
            //判断路径是否存在,不存在则创建文件路径
            val file = File(outPath.substring(0, outPath.lastIndexOf('/')))
            if (!file.exists()) {
                file.mkdirs()
            }
            //判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
            if (File(outPath).isDirectory) {
                continue
            }
            out = FileOutputStream(outPath)
            val buf = ByteArray(1024)
            var len: Int
            while (`in`.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
        } finally {
            `in`?.close()
            out?.close()
        }
    }
    zip.close()
}