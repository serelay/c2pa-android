/*
 * Copyright (c) 2021 Serelay Ltd. All rights reserved.
 */

package com.serelay.c2pa.provenance

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.serelay.c2pa.ThumbnailHelper
import com.serelay.c2pa.test.R
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ThumbnailHelperTest {

    @Test
    fun createThumbnailReproducibleJPEGTURBO_knownValues() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.resources.openRawResource(R.raw.srl_test_image)

        val opts = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inSampleSize = 4
        }
        // Decode just the info, not bitmap itself. Cannot use same inputStream as we can't rewind/reset
        BitmapFactory.decodeResource(context.resources, R.raw.srl_test_image, opts)
        val byteArrayOfImage = ThumbnailHelper.toByteArray(inputStream)!!
        val thumbnailJpeg = ThumbnailHelper.getThumbnail(byteArrayOfImage, byteArrayOfImage.size, opts.outWidth, opts.outHeight)
        val base64 = ThumbnailHelper.hashOfBytes(thumbnailJpeg)
        assertThat(base64).isEqualTo("CCBmuzZtrr5363AFXXA5z+Tf38Y9U4Y2Bm/CHXJWaW4=")
    }

    @Test
    fun getReproducibleJPEGThumbnail_automatedValues() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.resources.openRawResource(R.raw.srl_test_image)
        val destination = File(context.cacheDir, "original.jpg")
        val output = FileOutputStream(destination)
        // Copy the bits from instream to outstream
        val buffer = ByteArray(32 * 1024) // 32kb https://stackoverflow.com/questions/10143731/android-optimal-buffer-size
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } >= 0) {
            output.write(buffer, 0, bytesRead)
        }
        val jpeg = ThumbnailHelper.getReproducibleJPEGThumbnail(context, destination)
        val base64 = ThumbnailHelper.hashOfBytes(jpeg)
        assertThat(base64).isEqualTo("bbbxtA1YlLSkl70juFoYDjr2YSWwiSTbe4uMwub3zoQ=")
    }

    @Test
    fun getReproducibleJPEGThumbnailViaByteArray() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.resources.openRawResource(R.raw.srl_test_image)
        val bytes = ThumbnailHelper.toByteArray(inputStream)!!
        val jpeg = ThumbnailHelper.getReproducibleJPEGThumbnail(context, bytes)
        val base64 = ThumbnailHelper.hashOfBytes(jpeg)
        // File(context.cacheDir, "srl_test_image-viabytes.jpeg").writeBytes(jpeg)
        assertThat(base64).isEqualTo("bbbxtA1YlLSkl70juFoYDjr2YSWwiSTbe4uMwub3zoQ=")
    }

    @Test
    fun getReproducibleJPEGThumbnailViaByteArray_a70() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.resources.openRawResource(R.raw.cap_a70)
        val bytes = ThumbnailHelper.toByteArray(inputStream)!!
        val jpeg = ThumbnailHelper.getReproducibleJPEGThumbnail(context, bytes)
        val base64 = ThumbnailHelper.hashOfBytes(jpeg)
        // File(context.cacheDir, "caiA70-viabytes.jpeg").writeBytes(jpeg)
        // This is after exif is re-written too
        assertThat(base64).isEqualTo("jLdw1+3R5pk/rFhWChdtrvKpovNxDvd8kk0wZgLeabU=")
    }
}
