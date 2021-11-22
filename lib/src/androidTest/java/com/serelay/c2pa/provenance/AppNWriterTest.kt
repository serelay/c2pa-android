/*
 * Copyright (c) 2021 Serelay Ltd. All rights reserved.
 */

package com.serelay.c2pa.provenance

import android.content.Context
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.serelay.c2pa.provenance.api.ThumbnailSegment
import com.serelay.c2pa.test.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger

/**
 * Tests with a real JUMBF block, from Mona Lisa example.
 * This is in the Android test as Android has a Base64 conversion class, but pure java does not without another
 * library.
 */
@RunWith(AndroidJUnit4::class)
class AppNWriterTest {

    lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    // Base64 encoded so the test file is not binary
    val fullJUMBFBox =
        "SlAAAQAAAAEAAALOqnVtYgAAAB1qdW1kY2FjYgARABAAAMqAONuxA2NhaQAAAAJpanVtYgAAACRqdW1kY2FzdAARABAAAMqAONuxA2NiLmFkb2JlXzEAAAAA8Kq1rWIAAAAoanVtZGNhYXMAEQAQAADKgDjbsQNjYWkuYXNzZXJ0aW9ucwAAAABdanVtYgAAACxqdW1kanNvbgARABAAAMqAONuxA2NhaS5sb2NhdGlvbi5icm9hZAAAAAApanNvbnsgImxvY2F0aW9uIjogIk1hcmdhdGUgQ2l0eSwgTkoifQAAAGNqdW1iAAAAMWp1bWRleda726JEa9KsG8K+64mRA2NhaS5jbGFpbS50aHVtYm5haWwuanBnAAAAACpqcDJjYSBKUEVHIHRodW1ibmFpbCBzaG91bGQgZ28gaGVyZS4uLgAAANeqdW1iAAAAI2p1bWRjYWNsABEAEAAAyoA427EDY2FpLmNsYWltAAAAAMyqc29uewoJCQkicmVjb3JkZXIiIDogIlBob3Rvc2hvcCIsCgkJCSJzaWduYXR1cmUiIDogInNlbGYjanVtYmY9c19hZG9iZV8xIiwKCQkJImFzc2VydGlvbnMiIDogWwoJCQkJInNlbGYjanVtYmY9YXNfYWRvYmVfMS9jYWkubG9jYXRpb24uYnJvYWQ/aGw9NzYxNDJCRDYyMzYzRiIKCQkJXQoJCX0AAAB2anVtYgAAACdqdW1kY2FzZwARABAAAMqAONuxA2NhaS5zaWduYXR1cmUAAAAAR3V1aWRjYXNnABEAEAAAyoA427F0aGlzIHdvdWxkIG5vcm1hbGx5IGJlIGJpbmFyeSBzaWduYXR1cmUgZGF0YS4uLvOhgJhFeGlmAABJSSoACAAAAAAAAAAAAAAA"

    @Test
    fun canWriteJUMBFBox() {
        val inStream = context.resources.openRawResource(R.raw.srl_test_image)
        val outFileName = "${context.cacheDir}/non_serelay_jumbf_injected.jpg"
        val outStream = FileOutputStream(outFileName)
        val content = Base64.decode(fullJUMBFBox, Base64.NO_WRAP)
        AppNWriter().insertAppNContent(inStream, outStream, listOf(MarkerContent(AppNWriter.APP11_MARKER, content)))

        File(outFileName).delete()
    }

    // This one tests that the length > 256 mechanism works.
    @Test
    fun canWriteJUMBFBox_withExtraLength() {
        val inStream = context.resources.openRawResource(R.raw.srl_test_image)
        val outFileName = "${context.cacheDir}/non_serelay_jumbf_injected_extra_length.jpg"
        val outStream = FileOutputStream(outFileName)
        val content = Base64.decode(fullJUMBFBox, Base64.NO_WRAP)
        AppNWriter().insertAppNContent(inStream, outStream, listOf(MarkerContent(AppNWriter.APP11_MARKER, content)))

        val writtenSource = FileInputStream(outFileName)
        // Skip SOI
        writtenSource.skip(2)

        val length = ByteArray(2)
        val marker = ByteArray(2)
        writtenSource.read(marker)
        writtenSource.read(length)

        var completed = false
        // If we pass App11's range we've gone too far
        while (marker[1] < 0xEC.toByte()) {
            // is app 11
            if (marker[1] == 0xEB.toByte()) {
                completed = true
                break
            }
            // Skip content and get to next marker + length
            writtenSource.skip(BigInteger(length).toLong() - 2)
            writtenSource.read(marker)
            writtenSource.read(length)
        }
        assertThat(completed).isTrue()

        val calculatedLength = BigInteger(length).toInt()
        assertThat(calculatedLength).isEqualTo(content.size + 2)

        File(outFileName).delete()
    }

    @Test
    fun stripsExistingXMP() {
        val inStream = context.resources.openRawResource(R.raw.srl_test_image) // 1,799,561 bytes
        val outFileName = "${context.cacheDir}/test_without_xmp.jpg"
        val outStream = FileOutputStream(outFileName)
        val content = Base64.decode(fullJUMBFBox, Base64.NO_WRAP)
        AppNWriter().insertAppNContent(inStream, outStream, listOf(MarkerContent(AppNWriter.APP11_MARKER, content)))
        val originalLength = 1_799_561
        // XMP size marker is: 028F, 655
        val expectedLength = originalLength - 655 + (content.size + AppNWriter.APP11_MARKER.size)
        val output = File(outFileName)
        assertThat(output.length()).isEqualTo(expectedLength)
        output.delete()
    }

    @Test
    fun stripsNothingIfNoXMP() {
        val content = Base64.decode(fullJUMBFBox, Base64.NO_WRAP)
        val inStream = context.resources.openRawResource(R.raw.capture_s8) // 3,655,699 bytes
        val outFileName = "${context.cacheDir}/capture_s8_without_xmp.jpg"
        val outStream = FileOutputStream(outFileName)
        AppNWriter().insertAppNContent(inStream, outStream, listOf(MarkerContent(AppNWriter.APP11_MARKER, content)))
        val originalLength = 3_655_699
        // XMP size marker is: 028F, 655

        // 3,656,384
        val expectedLength = originalLength + 2 + (content.size + AppNWriter.APP11_MARKER.size)
        val output = File(outFileName)
        assertThat(output.length()).isEqualTo(expectedLength)
        output.delete()
    }

    // TODO need a test with some real data to check thumbnail insertion
    @Test
    fun canInsertThumbnailData() {
        val inStream = context.resources.openRawResource(R.raw.srl_test_image) // 3,655,699 bytes
        val outFileName = "${context.cacheDir}/cai_with_thumb.jpg"
        val outStream = FileOutputStream(outFileName)

        val markers = listOf(
            MarkerContent(AppNWriter.APP1_MARKER, "Whatsup".toByteArray(Charsets.UTF_8)),
            MarkerContent(AppNWriter.APP11_MARKER, "IM_A_CAIBLOCK".toByteArray(Charsets.UTF_8))
        )

        val segments = listOf(
            ThumbnailSegment(0, 3, 44)
        )

        val jpeg = "ThumbnailPlaceholderThumbnailPlaceholderThumbnailPlaceholder".toByteArray(Charsets.UTF_8)

        AppNWriter().insertAppNContentWithThumbnail(inStream, outStream, markers, jpeg, segments)

        val output = File(outFileName)
        assertThat(output.length()).isGreaterThan(1_799_561 - 657) // original size - existing xmp size (inc marker)
        var extra = 0
        markers.forEach { extra += 2 + it.second.size + 2 }
        segments.forEach { extra += it.length }
        assertThat(output.length()).isEqualTo(1_799_561 + extra - 657) // original size + extra inserted - existing xmp size (inc marker)
        output.delete()
    }

    @Test
    fun canInsertThumbnailDataMultiSegment() {
        val inStream = context.resources.openRawResource(R.raw.srl_test_image) // 3,655,699 bytes
        val outFileName = "${context.cacheDir}/cai_with_thumb.jpg"
        val outStream = FileOutputStream(outFileName)

        val markers = listOf(
            MarkerContent(AppNWriter.APP1_MARKER, "Whatsup".toByteArray(Charsets.UTF_8)),
            MarkerContent(AppNWriter.APP11_MARKER, "IM_A_CAIBLOCK".toByteArray(Charsets.UTF_8)),
            MarkerContent(AppNWriter.APP11_MARKER, "IM_ALSO_A_CAIBLOCK".toByteArray(Charsets.UTF_8))
        )

        val segments = listOf(
            ThumbnailSegment(0, 3, 44),
            ThumbnailSegment(1, 4, 3)
        )

        val jpeg = "ThumbnailPlaceholderThumbnailPlaceholderThumbnailPlaceholder".toByteArray(Charsets.UTF_8)

        AppNWriter().insertAppNContentWithThumbnail(inStream, outStream, markers, jpeg, segments)

        val output = File(outFileName)
        assertThat(output.length()).isGreaterThan(1_799_561 - 657) // original size - existing xmp size (inc marker)
        var extra = 0
        markers.forEach { extra += 2 + it.second.size + 2 }
        segments.forEach { extra += it.length }
        assertThat(output.length()).isEqualTo(1_799_561 + extra - 657) // original size + extra inserted - existing xmp size (inc marker)
        output.delete()
    }

    @Test
    fun augmentContents_boundryTest() {
        val jpeg = "A".repeat(30).toByteArray(Charsets.UTF_8)
        val original = "0".repeat(10).toByteArray(Charsets.UTF_8)

        val segment = ThumbnailSegment(0, 10, 20)

        val result = AppNWriter().augmentContents(
            original,
            segment,
            jpeg,
            0
        )

        val expectation = "0".repeat(10).toByteArray(Charsets.UTF_8) + "A".repeat(20).toByteArray(Charsets.UTF_8)
        assertThat(result).isEqualTo(expectation)

        val array = byteArrayOf()
        val array2 = array.copyOfRange(0, 0)
        assertThat(array.contentEquals(array2)).isTrue()
    }
}
