/*
 * Copyright (c) 2021 Serelay Ltd. All rights reserved.
 */

package com.serelay.c2pa.provenance.api

/**
 * @param assetHash                  Base64 hash
 * @param thumbnailHash              Base64 hash of thumbnail JPEG
 * @param thumbnailAssertionLength   Thumbnail size (as JPEG) in bytes
 * @param jumbfInsertionPoint        Where the JUMBF will be inserted in the original image
 * @param xmpInsertionPoint          We insert XMP at the start, after SOI JPEG marker
 */
data class AssetInfo(
    val assetHash: String,
    val thumbnailHash: String,
    val thumbnailAssertionLength: Int,
    val jumbfInsertionPoint: Int,
    val xmpInsertionPoint: Int = 2,
)
