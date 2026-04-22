package com.example.semestra.logic

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

object PdfTextExtractor {

    fun extractText(context: Context, uri: Uri): String {
        PDFBoxResourceLoader.init(context)
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            PDDocument.load(stream).use { doc ->
                PDFTextStripper().getText(doc)
            }
        } ?: ""
    }
}