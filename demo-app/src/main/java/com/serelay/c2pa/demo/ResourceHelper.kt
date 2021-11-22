/*
 * Copyright (c) 2021 Serelay Ltd. All rights reserved.
 */

package com.serelay.c2pa.demo

import android.content.Context
import android.content.res.Resources
import androidx.annotation.RawRes
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.jvm.Throws

object ResourceHelper {
    /**
     * Copies a raw resource file into this app's cache directory
     * It's unlikely that a raw resource would be used outside of an example.
     */
    @Throws(IOException::class)
    fun copyResourceToCache(context: Context, @RawRes rawId: Int, fileName: String): File {
        val inputStream = context.resources.openRawResource(rawId)
        val destination = File(context.cacheDir, fileName)
        val output = FileOutputStream(destination)
        // Copy the bits from instream to outstream
        val buffer = ByteArray(32 * 1024)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } >= 0) {
            output.write(buffer, 0, bytesRead)
        }
        inputStream.close()
        output.close()
        return destination
    }
}