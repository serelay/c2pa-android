/*
 * Copyright (c) 2021 Serelay Ltd. All rights reserved.
 */

package com.serelay.c2pa.provenance.api

/**
 * @param index   JUMBF segment to insert thumbnail content into
 * @param start   Where within the JUMBF to insert the content
 * @param length  In bytes; how much content to insert
 */
data class ThumbnailSegment(
    val index: Int,
    val start: Int,
    val length: Int
)
