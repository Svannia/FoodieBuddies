package com.example.foodiebuddy.ui.account

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import coil.compose.rememberImagePainter
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.jpeg.JpegDirectory
import com.example.foodiebuddy.R
import com.example.foodiebuddy.database.DatabaseConnection
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.NavigationButton
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.system.checkPermission
import com.example.foodiebuddy.system.imagePermissionVersion
import com.example.foodiebuddy.ui.ScreenStructure
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.ui.theme.SystemColor
import com.example.foodiebuddy.viewModels.UserViewModel
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun NewAccount(context: Context, userViewModel: UserViewModel, navigationActions: NavigationActions, picture: MutableState<Uri>, name: MutableState<String>, onEditPicture: () -> Unit) {
    // getting image and image permissions
    Log.d("Debug", "picture is: ${picture.value}")
    val imageInput = "image/*"
    val getPicture = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {pictureUri ->
            picture.value = pictureUri
            onEditPicture()
        }
    }
    val imagePermission = imagePermissionVersion()
    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getPicture.launch(imageInput)
            }
        }

    ScreenStructure(
        navigationActions = navigationActions,
        title = stringResource(R.string.title_createAccount),
        navButton = NavigationButton.GO_BACK,
        topBarIcons = {},
        content = {
            item {
                Text("ummm hello????")
                Log.d("Debug", "Uri: ${picture.value}")
                Image(
                    modifier = Modifier
                        .height(100.dp)
                        .width(100.dp)
                        .clickable { },
                    painter = rememberImagePainter(picture.value),
                    contentDescription = stringResource(R.string.desc_profilePic),
                    contentScale = ContentScale.FillBounds
                )
                OutlinedTextField(
                    value = name.value,
                    onValueChange = {name.value = it})
                Button(
                    onClick = {
                        checkPermission(context, imagePermission, requestPermissionLauncher) {
                            getPicture.launch(imageInput)
                        }
                    }
                ) {
                    Text("gotta try stuff")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetProfilePicture(picture: Uri, onCancel: () -> Unit, onSave: (Uri) -> Unit) {
    val context = LocalContext.current

    val density = LocalDensity.current
    val screenWidth = with(density) { LocalConfiguration.current.screenWidthDp * density.density }
    val screenHeight = with(density) { LocalConfiguration.current.screenHeightDp * density.density }
    val radius = minOf(screenWidth, screenHeight) / 2
    val imageInfo = computeMinScale(LocalContext.current, picture, radius, screenWidth, screenHeight)

    Log.d("Debug", "image info: scale ${imageInfo.minScale} width ${imageInfo.width} height ${imageInfo.height}")
    var scale by remember { mutableStateOf(imageInfo.minScale) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var imageWidth by remember { mutableStateOf(imageInfo.width) }
    var imageHeight by remember { mutableStateOf(imageInfo.height) }



    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
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
                painter = rememberImagePainter(picture),
                contentDescription = stringResource(R.string.desc_profilePic),
                contentScale = ContentScale.None,
                modifier = Modifier
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .fillMaxSize()
            )
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
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(SystemColor)
        ) {
            Row(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Annuler",
                    style = MyTypography.bodySmall,
                    modifier = Modifier.clickable {
                        onCancel()
                    }
                )
                Text(
                    text = "Sauvegarder",
                    style = MyTypography.bodySmall,
                    modifier = Modifier.clickable {
                        val croppedBitmap = cropImage(context, picture, scale, offsetX, offsetY, radius*2)
                        croppedBitmap?.let {
                            val croppedBitmapUri = saveBitmapToFile(context, it, "cropped_profile_picture.jpg")
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

private fun rotateBitmap(originalBitmap: Bitmap, degrees: Float): Bitmap {
    val width = originalBitmap.width
    val height = originalBitmap.height

    val rotatedBitmap = Bitmap.createBitmap(height, width, originalBitmap.config)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val newX = height - 1 - y
            val newY = x
            rotatedBitmap.setPixel(newX, newY, originalBitmap.getPixel(x, y))
        }
    }

    return rotatedBitmap
}
private fun getCorrectedBitmap(context: Context, picture: Uri): Bitmap? {
    val inputStream = context.contentResolver.openInputStream(picture) ?: return null
    val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
    val exif = ExifInterface(context.contentResolver.openInputStream(picture)!!)
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(originalBitmap, 90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(originalBitmap, 180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(originalBitmap, 270f)
        else -> originalBitmap // No rotation needed
    }
}

private fun computeMinScale(context: Context, picture: Uri, radius: Float, screenWidth: Float, screenHeight: Float): ImageInfo {
    return try {
        val inputStream = context.contentResolver.openInputStream(picture)


            val metadata = ImageMetadataReader.readMetadata(inputStream)
            val jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory::class.java)
            var imageHeight = jpegDirectory?.getString(JpegDirectory.TAG_IMAGE_HEIGHT)?.toFloat() ?: 0f
            var imageWidth = jpegDirectory?.getString(JpegDirectory.TAG_IMAGE_WIDTH)?.toFloat() ?: 0f
            Log.d("Debug", "metadata height is $imageHeight")
        // Iterate through metadata directories and tags
        for (directory in metadata.directories) {
            for (tag in directory.tags) {
                Log.d("Debug", "Metadata Tag: $tag")
            }
        }
        val exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
            val orientation = exifDirectory?.getString(ExifIFD0Directory.TAG_ORIENTATION)?.toInt()
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            val temp = imageHeight
            imageHeight = imageWidth
            imageWidth = temp
        }
            Log.d("Debug", "orientation: $orientation")
            Log.d("Debug", "raw width $imageWidth height $imageHeight")
            val diameter = radius * 2

        while (imageWidth > screenWidth || imageHeight > screenHeight) {
            Log.d("Debug", "screen height : $screenHeight screen width: $screenWidth")
            Log.d("Debug", "image height : $imageHeight image width: $imageWidth")
            if (imageWidth > screenWidth) {
                Log.d("Debug", "too wide")
                val ratio = screenWidth / imageWidth
                imageWidth = screenWidth
                imageHeight *= ratio
            } else  {
                Log.d("Debug", "too high")
                val ratio = screenHeight / imageHeight
                imageHeight = screenHeight
                imageWidth *= ratio
            }
        }

            val widthRatio = diameter / imageWidth
            val heightRatio = diameter / imageHeight
            val minScale = maxOf(widthRatio, heightRatio)
            Log.d("Debug", "width $imageWidth height $imageHeight")
            Log.d("Debug", " ratios width $widthRatio height $heightRatio")
            ImageInfo(minScale, imageWidth, imageHeight)
    } catch (e: Exception) {
        Log.d("Error", "Failed to calculate initial scale with $e")
        ImageInfo(1.5f, 0f, 0f)
    }
}

data class ImageInfo(val minScale: Float, val width: Float, val height: Float)

private fun cropImage(context: Context, picture: Uri, scale: Float, offsetX: Float, offsetY: Float, diameter: Float): Bitmap? {
    val inputStream = context.contentResolver.openInputStream(picture)
    val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

    val centerX = originalBitmap.width / 2 - offsetX
    val centerY = originalBitmap.height / 2 - offsetY
    val cropRadius = diameter / 2 / scale

    val cropLeft = (centerX - cropRadius).toInt()
    val cropTop = (centerY - cropRadius).toInt()
    val cropRight = (centerX + cropRadius).toInt()
    val cropBottom = (centerY + cropRadius).toInt()

    val cropWidth = cropRight - cropLeft
    val cropHeight = cropBottom - cropTop
    val croppedBitmap = Bitmap.createBitmap(originalBitmap, cropLeft, cropTop, cropWidth, cropHeight)
    val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, diameter.toInt(), diameter.toInt(), true)

    return scaledBitmap
}

private fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): Uri? {
    val file = File(context.cacheDir, fileName)
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
