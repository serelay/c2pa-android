/*
 * Copyright (c) 2021 Serelay Ltd. All rights reserved.
 */

package com.serelay.c2pa

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import com.serelay.c2pa.provenance.ByteArrayHelper
import java.io.*
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToInt

object ThumbnailHelper {
    init {
        System.loadLibrary("serelay-jpegturbo")
    }

    /**
     * Use a native library to create a reproducible thumbnail. Built in thumbnail methods on the OS are susceptible to OS changes and
     * hardware differences. Using this approach means we are sure that for the same input JPEG the same output thumbnail JPEG is output
     *
     * This version is used in favour of minimizing the memory footprint - by re-using the already existing byte array of the image.
     *
     * USED AT C2PA _PROCESSING_ TIME
     *
     * @param bytes Byte array of the original JPEG file
     * @return JPEG representation of thumbnail
     */
    fun getReproducibleJPEGThumbnail(context: Context, bytes: ByteArray): ByteArray {
        // val inputStream = BufferedInputStream(FileInputStream(file))
        val inputStream = ByteArrayInputStream(bytes)

        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // Decode just the info, not bitmap itself. Cannot use same inputStream as we can't rewind/reset
        BitmapFactory.decodeStream(inputStream, null, opts)
        inputStream.close()

        val input = ByteArrayInputStream(bytes)
        val rotation = ExifInterface(input).rotationDegrees
        input.close()

        val originalWidth = opts.outWidth
        val originalHeight = opts.outHeight

        // Calculate resample/scale
        // We assume it's already over 1024 so must scale down at least once
        val max = max(originalWidth, originalHeight)

        var scale = 8
        val denom = 8
        var scaledMax: Int
        do {
            scale--
            scaledMax = (max * (scale.toFloat() / denom.toFloat())).toInt()
        } while (scaledMax > 1024)
        // https://sourceforge.net/p/libjpeg-turbo/mailman/message/32806581/
        //  roundToInt: Rounds this [Float] value to the nearest integer and converts the result to [Int].
        //  * Ties are rounded towards positive infinity.
        val width = (originalWidth.toFloat() * (scale.toFloat() / denom.toFloat())).roundToInt()
        val height = (originalHeight.toFloat() * (scale.toFloat() / denom.toFloat())).roundToInt()
        val unrotatedThumbnail = getThumbnail(bytes, bytes.size, width, height)
        return rotateThumbnail(context, unrotatedThumbnail, rotation)
    }

    /**
     * Use a native library to create a reproducible thumbnail.
     * Built in thumbnail methods on the OS are susceptible to OS version/software changes and
     * hardware differences.
     * Using this approach means we are sure that for the same input JPEG gives
     * the same output thumbnail JPEG is output.
     *
     * This is possible with a full ByteArray of an image too.
     *
     * @param file An original JPEG file
     * @return JPEG representation of thumbnail
     */
    fun getReproducibleJPEGThumbnail(context: Context, file: File): ByteArray {
        val inputStream = BufferedInputStream(FileInputStream(file))
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // Decode just the info, not bitmap itself. Cannot use same inputStream as we can't rewind/reset
        BitmapFactory.decodeStream(inputStream, null, opts)
        inputStream.close()

        val rotation = ExifInterface(file).rotationDegrees

        val originalWidth = opts.outWidth
        val originalHeight = opts.outHeight

        // Calculate resample/scale
        // We assume it's already over 1024 so must scale down at least once
        val max = max(originalWidth, originalHeight)

        var scale = 8
        val denom = 8
        var scaledMax: Int
        do {
            scale--
            scaledMax = (max * (scale.toFloat() / denom.toFloat())).toInt()
        } while (scaledMax > 1024)
        val width = (originalWidth.toFloat() * (scale.toFloat() / denom.toFloat())).roundToInt()
        val height = (originalHeight.toFloat() * (scale.toFloat() / denom.toFloat())).roundToInt()

        val byteArrayOfImage = ByteArrayHelper.toByteArray(file)!!

        val unrotatedThumbnail = getThumbnail(byteArrayOfImage, byteArrayOfImage.size, width, height)
        return rotateThumbnail(context, unrotatedThumbnail, rotation)
    }

    /**
     * Apply Exif rotation to the new thumbnail, so that it renders correctly. Rotating in LibJPEG turbo is not perfect for MCU size
     * so leads to some artifacting where the incomplete MCU is left on the edges of the image (right or bottom)
     *
     * @param context The application context
     * @param original The thumbnail JPEG bytes, unrotated
     * @param rotation The rotation degrees to apply in EXIF
     * @return The final JPEG bytes which includes the rotation EXIF information
     */
    fun rotateThumbnail(context: Context, original: ByteArray, rotation: Int): ByteArray {
        val file = File(context.cacheDir, "${System.nanoTime()}.jpg") // we assume none can happen at the same nanosecond here
        file.writeBytes(original)
        val exif = ExifInterface(file)
        when (rotation) {
            90 -> exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            180 -> exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_180.toString())
            270 -> exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_270.toString())
        }
        exif.saveAttributes()
        val result = file.readBytes()
        file.delete()
        return result
    }

    /**
     * Generate a thumbnail from a source image using a scaled down representation in a power of 2.
     * The dimensions should be provided with pre-rotation and scaled down by the same factor.
     * e.g. 2 would be half the size, 4 would be a quarter of the size.
     *
     * @param source the byte buffer of the original JPEG file itself
     * @param length the length of the byte buffer
     * @param width the pre-rotated width (non EXIF application)
     * @param height the pre-rotated height (non EXIF application)
     *
     * @return The JPEG representation of the thumbnail in byte array with EXIF and other metadata.
     */
    external fun getThumbnail(
        source: ByteArray,
        length: Int,
        width: Int,
        height: Int
    ): ByteArray

}