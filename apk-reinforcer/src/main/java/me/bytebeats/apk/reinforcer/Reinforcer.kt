package me.bytebeats.apk.reinforcer

import java.io.File
import java.io.FileOutputStream

/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created at 2022/3/28 21:08
 * @Version 1.0
 * @Description TO-DO
 */

/**
 * 1、制作只包含解密代码的dex文件
 */

fun buildDexFromAar(aarFilePath: String, unzipDirPath: String) {
    val aarFile = File(aarFilePath)
    val unzipDir = File(unzipDirPath)

    decompress(aarFilePath, unzipDirPath)

    val classesDexFile = File(unzipDir, "classes.dex")
    val classesJarFile = File(unzipDir, "classes.jar")

    /**
     * command line = "dx --dex --output out.dex in.jar"
     */
    val process =
        Runtime.getRuntime().exec("dx --dex --output ${classesDexFile.absolutePath} ${classesJarFile.absolutePath}")
    process.waitFor()
    if (process.exitValue() != 0) {
        throw RuntimeException("")
    }
}

/**
 * 2、加密apk中所有的dex文件
 */
fun encryptDex(apkFilePath: String, unzipDirPath: String) {
    val apkFile = File(apkFilePath)
    val unzipDir = File(unzipDirPath)
    decompress(apkFilePath, unzipDirPath)
    //只要dex文件拿出来加密
    val dexFiles = unzipDir.listFiles { _, name -> name.endsWith(".dex") }
    //AES加密
    for (dexFile in dexFiles) {
        val bytes = getByteFromFile(dexFile)
        val encrypt = encrypt(bytes)
        val fos = FileOutputStream(File(unzipDir, "secret-${dexFile.name}"))
        fos.write(encrypt)
        fos.flush()
        fos.close()
        dexFile.delete()
    }

    val unsignedApkPath = ""
    rebuildDexIntoApk(unzipDirPath, unsignedApkPath)
}

/**
 * 3、把dex放入apk解压目录，重新压成apk文件
 */
fun rebuildDexIntoApk(unzipDirPath: String, unsignedApkPath: String) {
//    classesDexFile.renameTo(File(unzipDirPath, "classes.dex")) from buildDexFromAar()
    val unsignedApkFile = File(unsignedApkPath)
    compress(unzipDirPath, unsignedApkPath)
}

/**
 * 4、对其和签名，最后生成签名apk
 * zipalign -v -p 4 my-app-unsigned.apk my-app-unsigned-aligned.apk
 */
fun alignUnsignedApk(unsignedApkPath: String, alignedUnsignedApkPath: String) {
    val alignedUnsignedApkFile = File(alignedUnsignedApkPath)
    val process =
        Runtime.getRuntime().exec("zipalign -v -p 4 $unsignedApkPath ${alignedUnsignedApkFile.absolutePath}")
    process.waitFor()
    if (process.exitValue() != 0) {
        throw RuntimeException("zipalign failed")
    }
}

/**
 * apksigner sign --ks my-release-key.jks --out my-app-release.apk my-app-unsigned-aligned.apk
 * apksigner sign  --ks jks文件地址 --ks-key-alias 别名 --ks-pass pass:jsk密码 --key-pass pass:别名密码 --out  out.apk in.apk
 */

fun signApk(signedApkPath: String, keyStorePath: String, alignedUnsignedApkPath: String) {
    val signedApkFile = File(signedApkPath)
    val jksFile = File(keyStorePath)
    val process =
        Runtime.getRuntime().exec("apksigner sign --ks $keyStorePath --out $signedApkFile $alignedUnsignedApkPath")
    if (process.exitValue() != 0) {
        throw RuntimeException("apksigner failed")
    }
}

/**
 * Reference: {@link https://blog.csdn.net/I123456789T/article/details/91819275}
 */
fun main() {
    val aarFilePath = "apk-reinforcer/build/outputs/aar/apk-reinforcer-debug.aar"
    val aarUnzipDir = "apk-reinforcer/aar-unzip"
    buildDexFromAar(aarFilePath, aarUnzipDir)
    val srcApkPath = "app/build/outputs/apk/debug/app-debug.apk"
    val srcUnzipPath = "app/build/outputs/apk/debug/src-apk-unzip"
    encryptDex(srcApkPath, srcUnzipPath)
    val unsignedApkPath = "app/build/outputs/apk/debug/app-unsigned.apk"
    rebuildDexIntoApk(srcUnzipPath, unsignedApkPath)
    val alignedUnsignedApkPath = "app/build/outputs/apk/debug/app-unsigned-aligned.apk"
    alignUnsignedApk(unsignedApkPath, alignedUnsignedApkPath)
    val signedApkPath = "app/build/outputs/apk/debug/app-signed-aligned.apk"
    val keyStorePath = "app/app.jks"//use your file
    signApk(alignedUnsignedApkPath, keyStorePath, alignedUnsignedApkPath)
}



