/*
 * Copyright (c) 2021 Serelay Ltd. All rights reserved.
 */

package com.serelay.c2pa.provenance

import android.content.Context
import android.util.Base64
import com.serelay.c2pa.ThumbnailHelper
import com.serelay.c2pa.provenance.api.CreationInfo
import com.serelay.c2pa.provenance.api.CreationInfoV2
import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object C2PAFileHelper {
    const val XMP_START = "http://ns.adobe.com/xap/1.0/"

    /**
     * With a server response and the original file create a copy with inserted C2PA data.
     * If access to the original is via InputStream this must be copied to an app-readable file first.
     * This is because multiple passes of the input stream must be done, and a stream reset operation is not guaranteed.
     *
     * @param original A file containing the bytes of the original file.
     * @param outputFile A file in a writable directory to output the completed C2PA asset.
     * @param response The response from the C2PAAcquisitionManager call.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun createC2paCompliantFileWithThumbnail(
        original: File,
        outputFile: File,
        info: CreationInfoV2,
        context: Context
    ) {
        val jumbfs = info.jumbfs
        val xmp = info.xmp
        val thumbnailSegments = info.thumbnailSegments
        val inputStream = FileInputStream(original)
        val outputStream = FileOutputStream(outputFile)
        val app11s = jumbfs.map { Pair(AppNWriter.APP11_MARKER, Base64.decode(it, Base64.NO_WRAP)) }
        val markerPairs = mutableListOf(Pair(AppNWriter.APP1_MARKER, Base64.decode(xmp, Base64.NO_WRAP)))
        markerPairs.addAll(app11s)
        val jpeg = ThumbnailHelper.getReproducibleJPEGThumbnail(context, original)
        AppNWriter().insertAppNContentWithThumbnail(inputStream, outputStream, markerPairs, jpeg, thumbnailSegments)
    }

    /**
     * This method uses server-provided thumbnail information within passed jumbfs
     */
    @JvmStatic
    @Throws(IOException::class)
    fun createC2paCompliantFile(
        original: File,
        outputFile: File,
        info: CreationInfo
    ): File {
        val inputStream = FileInputStream(original)
        val outputStream = FileOutputStream(outputFile)
        val app11s = info.jumbfs.map { Pair(AppNWriter.APP11_MARKER, Base64.decode(it, Base64.NO_WRAP)) }
        val markerPairs = mutableListOf(Pair(AppNWriter.APP1_MARKER, Base64.decode(info.xmp, Base64.NO_WRAP)))
        markerPairs.addAll(app11s)
        AppNWriter().insertAppNContent(inputStream, outputStream, markerPairs)
        return outputFile
    }

    /**
     * Opinionated
     * @param stream The stream of the JPEG file, at the start, to scan matching the server logic.
     * @return The byte index where the JUMBF boxes should be inserted.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun getJumbfInsertionPoint(stream: InputStream): Int {
        // read initial JPEG start bytes
        stream.skip(2)
        var offset = 2

        val APP11 = byteArrayOf(0xFF.toByte(), 0xEB.toByte())
        val marker = ByteArray(2)
        val segmentLength = ByteArray(2)

        stream.read(marker)
        stream.read(segmentLength)

        while (
            marker[1] >= 0xE0.toByte() &&
            marker[1] < APP11[1]
        ) {
            val hexChars = Hex.bytesToStringUppercase(segmentLength)
            val length = Integer.valueOf(hexChars, 16) - 2 // -2 for our own length marker

            var toSkip = length.toLong()
            if (marker.contentEquals(byteArrayOf(0xFF.toByte(), 0xE1.toByte()))) {
                // could be xmp

                if (length > XMP_START.length) {
                    val buffer = ByteArray(XMP_START.length)
                    stream.read(buffer)
                    if (String(buffer, Charsets.UTF_8) == XMP_START) {
                        // We're in an XMP block
                        // We discount the marker, length and content from the offset (ahead of time)
                        offset -= 4
                        offset -= length
                    }
                    toSkip -= XMP_START.length // we've already read this much
                }
            }
            // already read marker + length (either full seg length or remainder of XMP)
            stream.skip(toSkip)
            offset += 2 + 2 + length // marker(2),length(2) + content length
            // Prep next loop
            stream.read(marker)
            stream.read(segmentLength)
        }
        return offset
    }

}
