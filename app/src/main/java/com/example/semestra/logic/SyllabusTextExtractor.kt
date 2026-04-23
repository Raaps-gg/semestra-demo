package com.example.semestra.logic

import android.content.Context
import android.net.Uri

object SyllabusTextExtractor {
    fun extractText(context: Context, uri: Uri): String {
        return if (PdfUploadValidator.isPdf(context, uri)) {
            PdfTextExtractor.extractText(context, uri)
        } else {
            ""
        }
    }
}
