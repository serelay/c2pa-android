/*
 * Copyright (c) 2021 Serelay Ltd. All rights reserved.
 */

package com.serelay.c2pa.provenance.api

/**
 * @param jumbfs  A list of JPEG JUMBF segments. These are APP11 blocks, they are in-order when sent
 *                by the server. These require augmenting with thumbnail data prior to being written
 *                to the file in order to be valid.
 * @param xmp     A single JPEG XMP (APP1) block C2PA information.
 */
data class CreationInfo(
    val jumbfs: List<String>,
    val xmp: String
)
