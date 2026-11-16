package com.uade.dda2.server.config

import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest

object EntityTag {
    fun fromRepresentation(value: Any, json: JsonMapper): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(value))
        val opaqueTag = digest.joinToString("") { byte -> "%02x".format(byte) }
        return "\"$opaqueTag\""
    }

    fun matches(ifMatch: String, current: String): Boolean =
        ifMatch.split(',').any { candidate -> candidate.trim() == "*" || candidate.trim() == current }
}
