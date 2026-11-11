package com.uade.dda2.server.feature.program.validator

import com.uade.dda2.server.error.ApiException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import org.mockito.Mockito.*
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProgramImageBranchTest {
    private val validator = ProgramImageValidator()
    private val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte())
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
    private fun file(name: String?, mime: String? = "image/jpeg", content: ByteArray = jpeg) =
        MockMultipartFile("image", name, mime, content)
    private fun rejects(code: String, input: MultipartFile) {
        assertEquals("PROGRAM_IMAGE_$code", assertFailsWith<ApiException> { validator.validate(input) }.code)
    }

    @TestFactory
    fun `invalid filenames are rejected`() = listOf(null, "", "   ", "bad\n.jpg", "/", ".", "..", "a".repeat(252) + ".jpg").map { name ->
        dynamicTest("filename $name") { rejects("INVALID_FILE_NAME", file(name)) }
    }

    @TestFactory
    fun `extension mime and signature must agree`() = listOf(
        file("photo.gif"), file("photo"), file("photo.jpg", null),
        file("photo.jpg", "image/png"), file("photo.png", "image/png", jpeg),
        file("photo.jpg", content = png), file("photo.jpg", content = byteArrayOf(0xff.toByte())),
    ).mapIndexed { index, input -> dynamicTest("mismatch $index") { rejects("INVALID_FILE_TYPE", input) } }

    @TestFactory
    fun `valid image names and media types are normalized`() = listOf(
        Triple(" C:\\photos\\PHOTO.JPG ", " Image/JPEG ", jpeg),
        Triple("/photos/photo.jpeg", "image/jpeg", jpeg),
        Triple("/photos/photo.png", "image/png", png),
    ).map { (name, mime, bytes) -> dynamicTest(name) {
        val result = validator.validate(file(name, mime, bytes))
        assertEquals(name.trim().substringAfterLast('/').substringAfterLast('\\'), result.originalName)
        assertEquals(mime.trim().lowercase(), result.contentType)
        assertContentEquals(bytes, result.content)
    } }

    @Test
    fun `empty and oversized uploads fail while exact size limit succeeds`() {
        rejects("EMPTY_FILE", file("photo.jpg", content = byteArrayOf()))
        val maximum = ByteArray(ProgramImageValidator.MAX_FILE_SIZE.toInt())
        jpeg.copyInto(maximum)
        assertEquals(maximum.size, validator.validate(file("photo.jpg", content = maximum)).content.size)
        rejects("FILE_TOO_LARGE", file("photo.jpg", content = maximum.copyOf(maximum.size + 1)))
    }

    @Test
    fun `actual content is checked even when multipart metadata reports a different size`() {
        val input = mock(MultipartFile::class.java)
        `when`(input.isEmpty).thenReturn(false)
        `when`(input.size).thenReturn(0L)
        rejects("EMPTY_FILE", input)
        `when`(input.size).thenReturn(3L)
        `when`(input.originalFilename).thenReturn("photo.jpg")
        `when`(input.contentType).thenReturn("image/jpeg")
        `when`(input.bytes).thenReturn(byteArrayOf())
        rejects("EMPTY_FILE", input)
        `when`(input.bytes).thenReturn(ByteArray(ProgramImageValidator.MAX_FILE_SIZE.toInt() + 1))
        rejects("FILE_TOO_LARGE", input)
    }
}
