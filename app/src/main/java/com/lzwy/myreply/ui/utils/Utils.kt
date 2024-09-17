package com.lzwy.myreply.ui.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.destination
import id.zelory.compressor.constraint.quality
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

fun createFileFromUri(context: Context, uri: Uri): File? {
    // Check if the Uri uses the "File" scheme
    if ("file" == uri.scheme) {
        return File(uri.path!!)
    }

    val contentResolver: ContentResolver = context.contentResolver

    // Check if the Uri uses the "Content" scheme
    if ("content" == uri.scheme) {
        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                val inputStream = contentResolver.openInputStream(uri)

                if (inputStream != null) {
                    val outputFile = createTempFile(context)
                    copyInputStreamToFile(inputStream, outputFile)
                    return outputFile
                }
            }
        }
    }

    // Handle special cases like SAF (Storage Access Framework) Uris
    // TODO: i don't know why we need Documents?
/*    if (DocumentsContract.isDocumentUri(context, uri)) {
        val documentFile = DocumentFile.fromSingleUri(context, uri)

        if (documentFile != null) {
            val displayName = documentFile.name
            val inputStream = contentResolver.openInputStream(uri)

            if (inputStream != null) {
                val outputFile = createTempFile(context)
                copyInputStreamToFile(inputStream, outputFile)
                return outputFile
            }
        }
    }*/

    return null
}

@Throws(IOException::class)
private fun copyInputStreamToFile(inputStream: InputStream, file: File) {
    FileOutputStream(file).use { outputStream ->
        val buffer = ByteArray(4 * 1024) // buffer size
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            outputStream.write(buffer, 0, read)
        }
        outputStream.flush()
    }
}

private fun createTempFile(context: Context): File {
    val timestamp: String = System.currentTimeMillis().toString()
    val fileName = "temp_image_$timestamp"
    val directory: File = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        ?: context.filesDir

    return File.createTempFile(fileName, ".jpg", directory)
}

suspend fun createAndCompressImage(context: Context, uri: Uri): File? {
    val contentResolver: ContentResolver = context.contentResolver
    val inputStream = contentResolver.openInputStream(uri)
    val originalFile = createFileFromUri(context, uri)
    val timestamp: String = System.currentTimeMillis().toString()

    // 压缩Bitmap
    val compressedBitmap = originalFile?.let {
        Compressor.compress(context, it) {
            quality(70) // adjust bitmap quality, range：0-100
            destination(File(context.cacheDir, "temp_image_$timestamp.jpg"))
        }
    }
    return compressedBitmap
}

private fun saveBitmapToFile(bitmap: Bitmap, directory: File): File {
    val file = File(directory, "compressed_image.jpg")
    try {
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        stream.flush()
        stream.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return file
}