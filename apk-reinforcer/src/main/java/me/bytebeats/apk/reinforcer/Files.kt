package me.bytebeats.apk.reinforcer

import java.io.File
import java.io.FileInputStream

/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created at 2022/3/28 19:57
 * @Version 1.0
 * @Description TO-DO
 */

fun getByteFromFile(file: File): ByteArray {
    val buffer = ByteArray(file.length().toInt())
    val fis = FileInputStream(file)
    fis.read(buffer)
    return buffer
}