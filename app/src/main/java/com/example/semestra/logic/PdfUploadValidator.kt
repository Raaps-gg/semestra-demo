package com.example.semestra.logic

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.util.Locale

object PdfUploadValidator {
    private const val MAX_BYTES = 40L * 1024 * 1024
    private const val MIME_PDF = "application/pdf"
    private const val MIME_X_PDF = "application/x-pdf"
    private const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    fun isPdf(context: Context, uri: Uri): Boolean {
        val mime = context.contentResolver.getType(uri)?.lowercase(Locale.US).orEmpty()
        if (mime == MIME_PDF || mime == MIME_X_PDF) {
            return true
        }
        val name = queryDisplayName(context, uri)?.lowercase(Locale.US).orEmpty()
        return name.endsWith(".pdf")
    }

    fun isDocx(context: Context, uri: Uri): Boolean {
        val mime = context.contentResolver.getType(uri)?.lowercase(Locale.US).orEmpty()
        if (mime == MIME_DOCX) {
            return true
        }
        val name = queryDisplayName(context, uri)?.lowercase(Locale.US).orEmpty()
        return name.endsWith(".docx")
    }

    fun isSupportedSyllabus(context: Context, uri: Uri): Boolean {
        return isPdf(context, uri) || isDocx(context, uri)
    }

    fun sizeOrNull(context: Context, uri: Uri): Long? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
        } catch (_: Exception) {
            null
        }
    }

    fun exceedsSizeLimit(context: Context, uri: Uri): Boolean {
        val size = sizeOrNull(context, uri) ?: return false
        return size > MAX_BYTES
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
