package com.example.foodiebuddy.ui.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import coil.compose.rememberAsyncImagePainter
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.jpeg.JpegDirectory
import com.example.foodiebuddy.R
import com.example.foodiebuddy.errors.handleError
import com.example.foodiebuddy.ui.theme.MyTypography
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * Screen to edit picture with zoom, displacement and mask.
 *
 * @param picture to be edited
 * @param roundMask if true makes the mask round, if false makes it square
 * @param onCancel block to run if the user presses the Cancel button
 * @return onSave block to run if the user presses the Save button, returning the edited picture
 */
@Composable
fun SetPicture(picture: Uri, roundMask: Boolean, onCancel: () -> Unit, onSave: (Uri) -> Unit) {
    val context = LocalContext.current

    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp * density.density
    val screenHeight = LocalConfiguration.current.screenHeightDp * density.density
    val radius = minOf(screenWidth, screenHeight) / 2
    val imageInfo = computeMinScale(LocalContext.current, picture, radius, screenWidth, screenHeight)

    var scale by remember { mutableFloatStateOf(imageInfo.minScale) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var imageWidth by remember { mutableFloatStateOf(imageInfo.width * imageInfo.minScale) }
    var imageHeight by remember { mutableFloatStateOf(imageInfo.height * imageInfo.minScale) }



    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // this allows the user to zoom, move and rotate the picture
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceAtLeast(imageInfo.minScale)

                        imageWidth = imageInfo.width * scale
                        imageHeight = imageInfo.height * scale

                        val maxOffsetX = abs(imageWidth / 2 - radius)
                        val maxOffsetY = abs(imageHeight / 2 - radius)

                        offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                    }
                }
        ) {
            Image(
                painter = rememberAsyncImagePainter(picture),
                contentDescription = stringResource(R.string.desc_userPic),
                contentScale = ContentScale.None,
                modifier = Modifier
                    // ensures that the picture correctly fills the screen
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .fillMaxSize()
            )
            if (roundMask) {
                // round opacity mask that indicates to the user how the picture will look when cropped round
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val circleMask = Path().apply {
                        addOval(Rect(center, radius))
                    }
                    clipPath(circleMask, clipOp = ClipOp.Difference){
                        drawRect(color = Color.Black.copy(alpha = 0.6f), size = size)
                    }
                }
            } else {
                // square opacity mask that indicates to the user how the picture will look when cropped square
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val squareMask = Path().apply {
                        addRect(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
                    }
                    clipPath(squareMask, clipOp = ClipOp.Difference) {
                        drawRect(color = Color.Black.copy(alpha = 0.6f), size = size)
                    }
                }
            }
        }
        // top bar with Cancel and Save options
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.button_cancel),
                    style = MyTypography.bodySmall,
                    modifier = Modifier.clickable {
                        onCancel()
                    }
                )
                Text(
                    text = stringResource(R.string.button_save),
                    style = MyTypography.bodySmall,
                    modifier = Modifier.clickable {
                        val croppedBitmap = cropImage(context, picture, imageWidth, imageHeight, offsetX, offsetY, radius, imageInfo.orientation)
                        croppedBitmap?.let {
                            val croppedBitmapUri = saveBitmapToFile(context, it)
                            croppedBitmapUri?.let { uri ->
                                onSave(uri)
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Computes the minimum scale that the picture has to be automatically zoomed in so that it at least fits the round mask in a way that avoids having any "empty picture" within the mask.
 *
 * @param context to access picture metadata
 * @param picture Uri of the user-input picture
 * @param radius of the round mask
 * @param screenWidth needed to compute the mask and picture size
 * @param screenHeight needed to compute the mask and picture size
 * @return ImageInfo object that contains the required image scale, size and orientation
 */
private fun computeMinScale(context: Context, picture: Uri, radius: Float, screenWidth: Float, screenHeight: Float): ImageInfo {
    return try {
        // honestly this entire thing works in ways only God knows (I understood when I wrote it but it's way too fucking complicated)
        val inputStream = context.contentResolver.openInputStream(picture)

        val metadata = ImageMetadataReader.readMetadata(inputStream)
        val jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory::class.java)
        var imageHeight = jpegDirectory?.getString(JpegDirectory.TAG_IMAGE_HEIGHT)?.toFloat() ?: 0f
        var imageWidth = jpegDirectory?.getString(JpegDirectory.TAG_IMAGE_WIDTH)?.toFloat() ?: 0f

        val exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
        val orientation = exifDirectory?.getString(ExifIFD0Directory.TAG_ORIENTATION)?.toInt()
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            val temp = imageHeight
            imageHeight = imageWidth
            imageWidth = temp
        }
        val diameter = radius * 2

        while (imageWidth > screenWidth || imageHeight > screenHeight) {
            if (imageWidth > screenWidth) {
                val ratio = screenWidth / imageWidth
                imageWidth = screenWidth
                imageHeight *= ratio
            } else  {
                val ratio = screenHeight / imageHeight
                imageHeight = screenHeight
                imageWidth *= ratio
            }
        }

        val widthRatio = diameter / imageWidth
        val heightRatio = diameter / imageHeight
        val minScale = maxOf(widthRatio, heightRatio)
        ImageInfo(minScale, imageWidth, imageHeight, orientation)
    } catch (e: Exception) {
        handleError(context, "Failed to calculate initial scale", e)
        ImageInfo(1.5f, 0f, 0f, ExifInterface.ORIENTATION_NORMAL)
    }
}

/**
 * This gives information about an image to manipulate it for profile pictures
 *
 * @property minScale float representing the minimum size of the image so it fits the mask
 * @property width of the image
 * @property height of the image
 * @property orientation as used by ExifInterface
 */
data class ImageInfo(val minScale: Float, val width: Float, val height: Float, val orientation: Int?)

/**
 * Crops the user-input picture into a round image that matches with the opacity mask.
 *
 * @param context to access picture metadata
 * @param picture Uri of the picture to be cropped
 * @param imageWidth to scale the picture
 * @param imageHeight to scale the picture
 * @param offsetX how much the user moved the picture on the X-axis (changes the center of the round picture)
 * @param offsetY how much the user moved the picture on the Y-axis (changes the center of the round picture)
 * @param radius radius of the round mask
 * @param orientation of the image (cuz Samsung orientation metadata is messed up)
 * @return new round image as a bitmap
 */
private fun cropImage(context: Context, picture: Uri, imageWidth: Float, imageHeight: Float, offsetX: Float, offsetY: Float, radius: Float, orientation: Int?): Bitmap? {
    val inputStream = context.contentResolver.openInputStream(picture)
    val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

    // basically selling my soul to Satan, don't even start with me
    val correctedBitmap = if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
        val matrix = Matrix()
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            matrix.setRotate(90f)
        } else {
            matrix.setRotate(270f)
        }
        val orientedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
        Bitmap.createScaledBitmap(orientedBitmap, (imageWidth).toInt(), (imageHeight).toInt(), true)
    } else {
        Bitmap.createScaledBitmap(originalBitmap, (imageWidth).toInt(), (imageHeight).toInt(), true)
    }
    val centerX = correctedBitmap.width / 2 - offsetX
    val centerY = correctedBitmap.height / 2 - offsetY

    val cropLeft = (centerX - radius).toInt()
    val cropTop = (centerY - radius).toInt()
    val cropRight = (centerX + radius).toInt()
    val cropBottom = (centerY + radius).toInt()

    val cropWidth = cropRight - cropLeft
    val cropHeight = cropBottom - cropTop
    val diameter = radius * 2

    val croppedBitmap = Bitmap.createBitmap(correctedBitmap, cropLeft, cropTop, cropWidth, cropHeight)
    val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, diameter.toInt(), diameter.toInt(), true)

    return scaledBitmap
}

/**
 * Saves a new image bitmap into a file.
 *
 * @param context to create the new image file
 * @param bitmap Bitmap of the new image to be converted to a picture file
 * @return Uri of the new picture
 */
private fun saveBitmapToFile(context: Context, bitmap: Bitmap): Uri? {
    val file = File(context.cacheDir, "cropped_profile_picture.jpg")
    return try {
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
