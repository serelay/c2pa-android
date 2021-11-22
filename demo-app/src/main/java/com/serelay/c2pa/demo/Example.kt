/*
 * Copyright (c) 2021 Serelay Ltd. All rights reserved.
 */

package com.serelay.c2pa.demo

import android.content.Context
import com.serelay.c2pa.ThumbnailHelper
import com.serelay.c2pa.provenance.ByteArrayHelper
import com.serelay.c2pa.provenance.C2PAFileHelper
import com.serelay.c2pa.provenance.api.AssetInfo
import java.io.IOException
import java.io.InputStream

object Example {

    @Throws(IOException::class)
    fun getServerC2PAInfoForImage(context: Context, originalImage: InputStream): AssetInfo {
        // Find out what byte index Jumbf data should be inserted at
        val jumbfInsertionPoint = C2PAFileHelper.getJumbfInsertionPoint(originalImage)

        // NB. Not InputStreams will support reset()

        originalImage.reset()
        // Get the full image bytes
        val fullBytes = ByteArrayHelper.toByteArray(originalImage)!!
        originalImage.close()

        // Create a reproducible thumbnail, this is the same whichever Android device/OS combination
        // This could equally be stored and inserted, but this is assumed to be on-the-fly here
        val thumbnail = ThumbnailHelper.getReproducibleJPEGThumbnail(context, fullBytes)
        // Hash the thumbnail for use in the C2PA assertion
        val thumbnailHash = ByteArrayHelper.hashOfBytes(thumbnail)

        // Hash of the full bytes. This would need to be adjusted with exclusion regions if it
        // already contains XMP, but here we do not have any.
        val assetHash = ByteArrayHelper.hashOfBytes(fullBytes)

        return AssetInfo(
            assetHash = assetHash,
            thumbnailHash = thumbnailHash,
            thumbnailAssertionLength = thumbnail.size,
            jumbfInsertionPoint = jumbfInsertionPoint,
            // Here we insert XMP at the start for ease. This is after SOI (first 2 bytes of JPEG)
            xmpInsertionPoint = 2
        )
    }

}