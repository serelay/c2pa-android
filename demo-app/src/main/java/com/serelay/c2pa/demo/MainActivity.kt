/*
 * Copyright (c) 2021 Serelay Ltd. All rights reserved.
 */

package com.serelay.c2pa.demo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.serelay.c2pa.provenance.C2PAFileHelper
import com.serelay.c2pa.provenance.api.CreationInfo
import com.serelay.c2pa.provenance.api.CreationInfoV2
import com.serelay.c2pa.provenance.api.ThumbnailSegment
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Information required by the server to create the asset
        prepareInfoForServer()

        val originalFile = ResourceHelper.copyResourceToCache(
            this,
            R.raw.srl_test_image,
            fileName = "cacheCopy.jpg"
        )
        val c2paImage = File(cacheDir, "c2paVersion.jpg")


        // ## Either:
        // This version the JUMBF provided by the server is to include the thumbnail
        fullUploadVersion(originalFile, c2paImage)

        // ## Or:
        localThumbailVersion(originalFile, File(cacheDir, "c2paLocalThumbnailVersion.jpg"))

        // Should now be able to browse device cache files at:
        // /data/data/com.serelay.c2pa.demo/cache

        // cacheCopy.jpg - A direct copy of the raw resource srl_test_image.jpg
        // c2paVersion.jpg - Version with asset information, internal thumbnail provided by server
        // c2paLocalThumbnailVersion.jpg - Version with partial thumbnail data inserted locally

        // This can be viewed in Android Studio via: View > Tool Windows > Device File Explorer
    }

    fun fullUploadVersion(originalFile: File, c2paImage: File) {
        // NB these are placeholders, the information should be provided by the server.
        val creationInfo  = CreationInfo(
            jumbfs = listOf("TXlKdW1iZkV4YW1wbGU="), // ["MyJumbfExample"] as Base64
            xmp = "TXlFeGFtcGxlWE1Q" // MyExampleXMP as Base64
        )

        C2PAFileHelper.createC2paCompliantFile(
            original = originalFile,
            outputFile = c2paImage,
            info = creationInfo
        )
    }

    fun localThumbailVersion(originalFile: File, destination: File) {
        val creationInfo = CreationInfoV2(
            // ["MyJumbfExample"] as Base64
            jumbfs = listOf("TXlKdW1iZkV4YW1wbGU="),
            // MyExampleXMP as Base64
            xmp = "TXlFeGFtcGxlWE1Q",
            thumbnailSegments = listOf(
                // This is a trivial thumbnail example.
                // Insert 24 bytes of thumbnail in jumbfs[0], from 0 bytes in
                // e.g. FFD8FFE1 006A4578 69660000 4D4D002A 00000008 00040100
                ThumbnailSegment(0, 0, 24)
                // i.e. [First24 bytes of thumbnail, MyJumbfExample]
            )
        )

        C2PAFileHelper.createC2paCompliantFileWithThumbnail(
            original = originalFile,
            info = creationInfo,
            outputFile = destination,
            context = this
        )
    }

    /**
     * Obtain real data for use by the server when generating C2PA asset information
     * Uses srl_test_image.jpg in res/raw.
     */
    private fun prepareInfoForServer() {
        val inputStream = resources.openRawResource(R.raw.srl_test_image)
        val serverInfo = Example.getServerC2PAInfoForImage(
            this,
            inputStream
        )
        inputStream.close()

        Log.d(TAG, "serverInfo: $serverInfo")

        //AssetInfo(
        // assetHash=tDsLwbKP2Fe8ke67HGPFRWk7+OJ03atkkutPk/HFjhI=
        // thumbnailHash=bbbxtA1YlLSkl70juFoYDjr2YSWwiSTbe4uMwub3zoQ=,
        // thumbnailAssertionLength=158207,
        // jumbfInsertionPoint=854,
        // xmpInsertionPoint=2
        // )
    }

}