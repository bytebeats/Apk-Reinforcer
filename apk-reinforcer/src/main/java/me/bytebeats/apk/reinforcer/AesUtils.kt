package me.bytebeats.apk.reinforcer

import java.io.UnsupportedEncodingException
import java.security.InvalidKeyException
import java.security.Key
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec

/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created at 2022/3/28 18:04
 * @Version 1.0
 * @Description TO-DO
 */

private const val DEFAULT_PASSWORD = "bytebeats6668888"
private const val ALGORITHM_AES = "AES"
private const val ALGORITHM_AES_ECB_PKCS5PADDING = "AES/ECB/PKCS5Padding"

/**
 * 生成key，作为加密和解密密钥且只有密钥相同解密加密才会成功
 * @param password 相同的密码才能生成相同的 Key
 */
fun createKey(password: String?): Key? = try {
    // 创建AES的Key生产者
    val keyGenerator = KeyGenerator.getInstance(ALGORITHM_AES)
    //利用用户密码作为随机数初始化出128位的key生产者
    if (password.isNullOrEmpty()) {
        keyGenerator.init(128)
    } else {
        keyGenerator.init(128, SecureRandom(password.toByteArray(Charsets.UTF_8)))
    }
    //根据用户密码，生成一个密钥
    val secretKey = keyGenerator.generateKey()
    //返回基本编码格式的密钥，如果此密钥不支持编码，则返回null
    val keyInByte = secretKey.encoded
    //转换为AES专用密钥
    SecretKeySpec(keyInByte, ALGORITHM_AES)
} catch (ignore: NoSuchAlgorithmException) {
    null
} catch (ignore: UnsupportedEncodingException) {
    null
}


@JvmOverloads
fun encrypt(content: String?, key: Key? = createKey(DEFAULT_PASSWORD)): ByteArray? =
    encrypt(content?.toByteArray(Charsets.UTF_8), key)

@JvmOverloads
fun encrypt(content: ByteArray?, key: Key? = createKey(DEFAULT_PASSWORD)): ByteArray? = try {
    if (content == null || key == null) {
        null
    } else {
        val cipher = Cipher.getInstance(ALGORITHM_AES_ECB_PKCS5PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        //将加密并编码后的内容解码成字节数组
        val encrypted = cipher.doFinal(content)
        encrypted
    }
} catch (ignore: NoSuchPaddingException) {
    null
} catch (ignore: NoSuchAlgorithmException) {
    null
} catch (ignore: UnsupportedEncodingException) {
    null
} catch (ignore: InvalidKeyException) {
    null
} catch (ignore: IllegalBlockSizeException) {
    null
} catch (ignore: BadPaddingException) {
    null
}

@JvmOverloads
fun decrypt(decrypted: ByteArray?, key: Key? = createKey(DEFAULT_PASSWORD)): ByteArray? = try {
    if (decrypted == null || key == null) {
        null
    } else {
        val cipher = Cipher.getInstance(ALGORITHM_AES_ECB_PKCS5PADDING)
        cipher.init(Cipher.DECRYPT_MODE, key)
        cipher.doFinal(decrypted)
    }
} catch (ignore: NoSuchPaddingException) {
    null
} catch (ignore: NoSuchAlgorithmException) {
    null
} catch (ignore: UnsupportedEncodingException) {
    null
} catch (ignore: InvalidKeyException) {
    null
} catch (ignore: IllegalBlockSizeException) {
    null
} catch (ignore: BadPaddingException) {
    null
}

@JvmOverloads
fun decrypt(decrypted: String?, key: Key? = createKey(DEFAULT_PASSWORD)): ByteArray? = try {
    if (decrypted.isNullOrEmpty() || key == null) {
        null
    } else {
        val decryptedInByte = decrypted.toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(ALGORITHM_AES_ECB_PKCS5PADDING)
        cipher.init(Cipher.DECRYPT_MODE, key)
        cipher.doFinal(decryptedInByte)
    }
} catch (ignore: NoSuchPaddingException) {
    null
} catch (ignore: NoSuchAlgorithmException) {
    null
} catch (ignore: UnsupportedEncodingException) {
    null
} catch (ignore: InvalidKeyException) {
    null
} catch (ignore: IllegalBlockSizeException) {
    null
} catch (ignore: BadPaddingException) {
    null
}

fun main() {
    val content = "bytebeats Go!"
    val key = createKey(DEFAULT_PASSWORD)
//    val key1 = createKey(DEFAULT_PASSWORD)
    println("before: $content")
    val encrypted = encrypt(content, key)
    val encryptedStr = String(encrypted!!)
    println("encrypted: $encryptedStr")
    val decrypted = decrypt(encryptedStr, key)
    val decryptedStr = String(decrypted ?: byteArrayOf())
    println("decrypted: $decryptedStr")
}

