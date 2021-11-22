/*
 * Copyright (c) 2021 Serelay Ltd. All rights reserved.
 */

package com.serelay.c2pa.provenance

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets

/**
 * This tests the App11 writing in JVM, see the AppNWriterTest in androidTest for real JUMBF.
 * Due to Base64 support constraints it they are not in regular Java unit tests.
 */
@RunWith(JUnit4::class)
@Suppress("DEPRECATION")
class AppNWriterUnitTest {

    private fun getFile(name: String): File {
        return File(javaClass.classLoader!!.getResource(name).file)
    }

    @Test
    fun canWriteContent_withCalculatedLength() {
        val contentToWrite = "Ian Rocks"
        val file = getFile("non_serelay_image.jpg")
        val outFileName = "customInjected.jpg"
        val inStream = BufferedInputStream(FileInputStream(file))
        val outStream = FileOutputStream(outFileName)
        val raw = contentToWrite.toByteArray(StandardCharsets.UTF_8)

        val content = MarkerContent(AppNWriter.APP11_MARKER, raw)
        AppNWriter().insertAppNContent(inStream, outStream, listOf(content))

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
        assertThat(calculatedLength).isEqualTo(content.second.size + 2)

        val insertedContent = ByteArray(content.second.size)
        writtenSource.read(insertedContent)
        val contentString = String(insertedContent, StandardCharsets.UTF_8)
        assertThat(contentString).isEqualTo(contentToWrite)

        File(outFileName).delete()
    }

    @Test
    fun canWriteOddLengthHex() {
        // 258 would be 102, which needs to be 0102 so here we check the padding we add
        val file = getFile("non_serelay_image.jpg")
        val inStream = BufferedInputStream(FileInputStream(file))
        val outFileName = "non_serelay_borderline_length.jpg"
        val outStream = FileOutputStream(outFileName)

        val writtenValue = "S".repeat(258).toByteArray(StandardCharsets.UTF_8)

        AppNWriter().insertAppNContent(inStream, outStream, listOf(MarkerContent(AppNWriter.APP11_MARKER, writtenValue)))

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
            writtenSource.skip(BigInteger(length).toLong() - 2)
            writtenSource.read(marker)
            writtenSource.read(length)
        }
        assertThat(completed).isTrue()

        val calculatedLength = BigInteger(length).toInt()
        assertThat(calculatedLength).isEqualTo(writtenValue.size + 2)

        File(outFileName).delete()
    }

    @Test
    fun canWriteWithinExistingAppSegments() {
        val file = getFile("lena_with_APP0_APP1_APP10_APP11_APP12.jpg")
        val inStream = BufferedInputStream(FileInputStream(file))
        val outFileName = "lena_with_injected_segment.jpg"
        val outStream = FileOutputStream(outFileName)

        val writtenValue = "S".repeat(230).toByteArray(StandardCharsets.UTF_8)
        AppNWriter().insertAppNContent(inStream, outStream, listOf(Pair(AppNWriter.APP11_MARKER, writtenValue)))

        // Check it was inserted before 12 after 11
        val checkStream = BufferedInputStream(FileInputStream(outFileName))
        val marker = ByteArray(2)
        val segmentLength = ByteArray(2)
        // read SOI
        checkStream.read(marker)
        assertThat(marker).isEqualTo(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))

        // APP: 0, 1, 10, 11, 11, 12
        val expectedSegmentMarkers = byteArrayOf(0xE0.toByte(), 0xE1.toByte(), 0xEA.toByte(), 0xEB.toByte(), 0xEB.toByte(), 0xEC.toByte())

        // Read marker
        for (i in expectedSegmentMarkers.indices) {
            checkStream.read(marker)
            checkStream.read(segmentLength)

            assertThat(marker[0]).isEqualTo(0xFF.toByte())
            assertThat(marker[1]).isEqualTo(expectedSegmentMarkers[i])

            val length = lengthFromBytes(segmentLength)
            checkStream.skip(length.toLong())
        }

        checkStream.close()
        File(outFileName).delete()
    }

    @Test
    fun canInsertMultipleSegments() {
        val file = getFile("lena_with_APP0_APP1_APP10_APP11_APP12.jpg")
        val inStream = BufferedInputStream(FileInputStream(file))
        val outFileName = "lena_with_injected_segments.jpg"
        val outStream = FileOutputStream(outFileName)

        val writtenValue = "S".repeat(230).toByteArray(StandardCharsets.UTF_8)
        AppNWriter().insertAppNContent(
            inStream,
            outStream,
            listOf(
                MarkerContent(AppNWriter.APP1_MARKER, "Whatsup".toByteArray(StandardCharsets.UTF_8)),
                MarkerContent(AppNWriter.APP11_MARKER, writtenValue)
            )
        )

        // Check it was inserted before 12 after 11
        val checkStream = BufferedInputStream(FileInputStream(outFileName))
        val marker = ByteArray(2)
        val segmentLength = ByteArray(2)
        // read SOI
        checkStream.read(marker)
        assertThat(marker).isEqualTo(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))

        // APP: 0, 1, 10, 11, 11, 12
        val expectedSegmentMarkers = byteArrayOf(0xE0.toByte(), 0xE1.toByte(), 0xE1.toByte(), 0xEA.toByte(), 0xEB.toByte(), 0xEB.toByte(), 0xEC.toByte())

        // Read marker
        for (i in expectedSegmentMarkers.indices) {
            checkStream.read(marker)
            checkStream.read(segmentLength)

            assertThat(marker[0]).isEqualTo(0xFF.toByte())
            assertThat(marker[1]).isEqualTo(expectedSegmentMarkers[i])

            val length = lengthFromBytes(segmentLength)
            checkStream.skip(length.toLong())
        }

        checkStream.close()
        File(outFileName).delete()
    }

    private fun lengthFromBytes(byteArray: ByteArray): Int {
        val hexChars = Hex.bytesToStringUppercase(byteArray)
        return Integer.valueOf(hexChars, 16) - 2
    }
}
