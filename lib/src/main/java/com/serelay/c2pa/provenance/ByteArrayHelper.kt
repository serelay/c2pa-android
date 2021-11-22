/*
 * Copyright (c) 2021 Serelay Ltd. All rights reserved.
 */

package com.serelay.c2pa.provenance

import android.util.Base64
import java.io.*
import java.security.MessageDigest

object ByteArrayHelper {

    @JvmStatic
    @Throws(IOException::class)
    fun toByteArray(file: File): ByteArray? {
        val inputStream = FileInputStream(file)
        val result = toByteArray(inputStream)
        inputStream.close()
        return result
    }

    @JvmStatic
    @Throws(IOException::class)
    fun toByteArray(inputStream: InputStream): ByteArray? {
        val os = ByteArrayOutputStream()
        val buffer = ByteArray(1024 * 32) // Considered optimal buffer size for Android
        var len: Int

        // read bytes from the input stream and store them in buffer
        while (inputStream.read(buffer).also { len = it } != -1) {
            // write bytes from the buffer into output stream
            os.write(buffer, 0, len)
        }
        val result = os.toByteArray()
        os.close()
        return result
    }

    /**
     * Create the Base64 representation of the SHA-256 of the provided array.
     *
     * @param bytes The Thumbnail JPEG file byte representation
     * @return Base64 String representation of the SHA-256 digest of the message
     */
    fun hashOfBytes(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(bytes)
        val digest = md.digest()
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }
}