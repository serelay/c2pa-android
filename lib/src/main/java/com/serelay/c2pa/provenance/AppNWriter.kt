/*
 * Copyright (c) 2021 Serelay Ltd. All rights reserved.
 */

package com.serelay.c2pa.provenance

import com.serelay.c2pa.provenance.api.ThumbnailSegment
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.jvm.Throws

typealias Marker = ByteArray // 2
typealias Content = ByteArray // N
typealias MarkerContent = Pair<Marker, Content> // 2, N

/**
 * Writer for APPN contents within a JPEG. Order must be specified correctly prior to writing in this way.
 * APP1 and APP11 contents are treated specially here. Depending on the method called and circumstances certain sections may be skipped.
 * Streams must be independent. A fresh output stream is required for the image as the original is read.
 */
class AppNWriter {
    companion object {
        val APP1_MARKER = byteArrayOf(0xFF.toByte(), 0xE1.toByte())
        val APP11_MARKER = byteArrayOf(0xFF.toByte(), 0xEB.toByte())
    }

    @Throws(IOException::class)
    @Deprecated("Should use insertAppNContentWithThumbnail() instead")
    fun insertAppNContent(
        original: InputStream,
        destination: OutputStream,
        content: List<MarkerContent>
    ) {
        val marker = ByteArray(2)
        val segmentLength = ByteArray(2)
        original.read(marker)
        destination.write(marker)

        // get first marker and length (APP0 is mandatory in JPEGs)
        original.read(marker)
        original.read(segmentLength)

        for (markerContent in content) {
            while (
                marker[1] < markerContent.first[1] &&
                marker[1] >= 0xE0.toByte() &&
                marker[1] <= 0xEF.toByte()
            ) {
                val hexChars = Hex.bytesToStringUppercase(segmentLength)
                val length = Integer.valueOf(hexChars, 16) - 2
                val segmentContents = ByteArray(length)
                original.read(segmentContents)
                if (!String(segmentContents, Charsets.UTF_8).startsWith("http://ns.adobe.com/xap/1.0/")) {
                    destination.write(marker)
                    destination.write(segmentLength)
                    destination.write(segmentContents)
                }
                original.read(marker)
                original.read(segmentLength)
            }

            destination.write(markerContent.first)

            val dataToInsert = markerContent.second
            val length = dataToInsert.size + 2
            val lengthBytes = if (length < 256) {
                byteArrayOf(0x00.toByte(), length.toByte())
            } else {
                val stringBuilder = StringBuilder()
                stringBuilder.append(Integer.toHexString(length))
                // Enforce an even length with leading 0 as padding e.g. ffb -> 0ffb
                if (stringBuilder.length % 2 != 0) {
                    stringBuilder.insert(0, '0')
                }
                Hex.stringToBytes(stringBuilder.toString())
            }
            destination.write(lengthBytes)
            destination.write(dataToInsert)
        }

        // Now write the remaining APP11 or APP12 marker, length and all following content
        destination.write(marker)
        destination.write(segmentLength)

        // Docs here do not say, but I believe this continues the state of 'original' buffer. This is in experience and practice, just not
        // docs. Also makes sense that the stream would be in a given position already.
        original.copyTo(destination)
        original.close()
        destination.close()
    }

    // TODO should we convert for them. Taking the response as an argument here instead? (i.e. do we hide this within C2PAAcquisitionManager?)
    /**
     * Taking the original input stream, create a copy, inserting the corresponding C2PA content and the locally
     * created thumbnail.
     * <b>NB</b>. If the original or the thumbnail is different from creation time, this will result in an invalid
     * C2PA asset. While it will contain the C2PA information for the file it was originally requested for an edited
     * or different source file will produce validation errors when the C2PA asset is examined.
     *
     * @param original An input stream of the original source file.
     * @param destination A writable output stream of the destination file.
     * @param content The converted response for app insertion.
     * @param includeLength Whether or not to include the length during insertion.
     * @param thumbnailJpeg The thumbnail JPEG data as raw bytes.
     * @param thumbnailSegments The converted response for thumbnail insertion into the provided content segments.
     */
    fun insertAppNContentWithThumbnail(
        original: InputStream,
        destination: OutputStream,
        content: List<MarkerContent>,
        thumbnailJpeg: ByteArray,
        thumbnailSegments: List<ThumbnailSegment>
    ) {
        // Must not reorder here because the indexes must be preserved for thumbnail insertion
        val marker = ByteArray(2)
        val segmentLength = ByteArray(2)
        original.read(marker)
        destination.write(marker)

        // get first marker and length (APP0 is mandatory in JPEGs)
        original.read(marker)
        original.read(segmentLength)

        // var minimum = 0xE0.toByte()
        var offset = 0

        content.forEachIndexed { index, markerContent ->

            while (
                marker[1] < markerContent.first[1] &&
                marker[1] >= 0xE0.toByte() &&
                marker[1] <= 0xEF.toByte()
            ) {
                val hexChars = Hex.bytesToStringUppercase(segmentLength)
                val length = Integer.valueOf(hexChars, 16) - 2
                val segmentContents = ByteArray(length)
                original.read(segmentContents)
                if (!String(segmentContents, Charsets.UTF_8).startsWith("http://ns.adobe.com/xap/1.0/")) {
                    destination.write(marker)
                    destination.write(segmentLength)
                    destination.write(segmentContents)
                }
                original.read(marker)
                original.read(segmentLength)
            }

            destination.write(markerContent.first)
            var second: Content? = null
            if (markerContent.first.contentEquals(APP11_MARKER)) {
                // index discounting the first for the preceding APP1 marker
                thumbnailSegments.firstOrNull { it.index == index - 1 }?.let { segment ->
                    second = augmentContents(markerContent.second, segment, thumbnailJpeg, offset)
                    offset += segment.length
                }
            }

            val dataToInsert = second ?: markerContent.second
            val length = dataToInsert.size + 2
            val lengthBytes = if (length < 256) {
                byteArrayOf(0x00.toByte(), length.toByte())
            } else {
                val stringBuilder = StringBuilder()
                stringBuilder.append(Integer.toHexString(length))
                // Enforce an even length with leading 0 as padding e.g. ffb -> 0ffb
                if (stringBuilder.length % 2 != 0) {
                    stringBuilder.insert(0, '0')
                }
                Hex.stringToBytes(stringBuilder.toString())
            }
            destination.write(lengthBytes)
            destination.write(dataToInsert)
        }

        // Now write the remaining APP11 or APP12 marker, length and all following content
        destination.write(marker)
        destination.write(segmentLength)

        // Docs here do not say, but I believe this continues the state of 'original' buffer. This is in experience and practice, just not
        // docs. Also makes sense that the stream would be in a given position already.
        original.copyTo(destination)
        original.close()
        destination.close()
    }

    fun augmentContents(
        original: Content,
        segment: ThumbnailSegment,
        thumbnailJpeg: ByteArray,
        offset: Int
    ): Content {
        return original.copyOfRange(0, segment.start) +
            thumbnailJpeg.copyOfRange(offset, offset + segment.length) +
            original.copyOfRange(segment.start, original.size)
    }
}
