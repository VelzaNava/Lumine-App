package com.thesis.lumine.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Prepares a gallery photo for upload: applies its EXIF rotation, scales it so
 * the longest side is at most [MAX_DIMENSION] px and re-encodes it as JPEG.
  * A 12 MP photo (4-8 MB) ends up a few hundred KB, well under the server's 5 MB limit.
 */
object ImageCompressor {

    private const val MAX_DIMENSION = 1280
    private const val JPEG_QUALITY = 85

    /**
     * Builds the multipart "file" part for an upload. Blocking: call it from a
     * background dispatcher. Returns null if the picked file can't be read.
     */
    fun toUploadPart(context: Context, uri: Uri, fileName: String): MultipartBody.Part? {
        val jpeg = toJpeg(context, uri)
        if (jpeg != null) {
            return MultipartBody.Part.createFormData(
                "file", "$fileName.jpg", jpeg.toRequestBody("image/jpeg".toMediaType())
            )
        }

        // Undecodable here (e.g. HEIC on Android 8): send the original and let the server validate it.
        val original = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val type = context.contentResolver.getType(uri) ?: "application/octet-stream"
        return MultipartBody.Part.createFormData("file", fileName, original.toRequestBody(type.toMediaType()))
    }

    private fun toJpeg(context: Context, uri: Uri): ByteArray? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Decode at the coarsest power-of-two step that still keeps MAX_DIMENSION of detail.
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_DIMENSION) {
            sampleSize *= 2
        }

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } ?: return null

        val scaled = scaleDown(decoded)
        if (scaled !== decoded) decoded.recycle()

        val upright = rotateUpright(context, uri, scaled)
        if (upright !== scaled) scaled.recycle()

        return ByteArrayOutputStream().use { output ->
            upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            upright.recycle()
            output.toByteArray()
        }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= MAX_DIMENSION) return bitmap

        val ratio = MAX_DIMENSION.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * ratio).roundToInt(), (bitmap.height * ratio).roundToInt(), true
        )
    }

    /** Phone cameras store portrait photos sideways plus an EXIF rotation flag. */
    private fun rotateUpright(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        } catch (e: IOException) {
            null
        }

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }

        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
