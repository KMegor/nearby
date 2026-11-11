package com.coursework.nearby.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.coursework.nearby.data.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject

/** Готовит выбранное фото к загрузке: квадрат 512 x 512, JPEG. */
class AvatarEncoder @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun encode(uri: String): ByteArray = withContext(io) {
        val source = decode(Uri.parse(uri)) ?: throw IOException("Не удалось прочитать изображение")
        val side = minOf(source.width, source.height)
        val square = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
        val scaled = if (side > SIZE) Bitmap.createScaledBitmap(square, SIZE, SIZE, true) else square
        ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            out.toByteArray()
        }
    }

    private fun decode(uri: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder учитывает поворот из EXIF.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val largest = maxOf(info.size.width, info.size.height)
                if (largest > MAX_DECODE_SIDE) decoder.setTargetSampleSize(largest / MAX_DECODE_SIDE)
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }

    private companion object {
        const val SIZE = 512
        const val QUALITY = 85
        const val MAX_DECODE_SIDE = 2048
    }
}
