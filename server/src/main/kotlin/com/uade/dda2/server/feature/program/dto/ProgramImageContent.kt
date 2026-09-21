package com.uade.dda2.server.feature.program.dto

data class ProgramImageContent(
    val originalName: String,
    val contentType: String,
    val sizeBytes: Long,
    val content: ByteArray,
)
