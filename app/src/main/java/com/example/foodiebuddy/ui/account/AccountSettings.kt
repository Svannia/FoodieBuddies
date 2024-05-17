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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import coil.compose.rememberImagePainter
import com.example.foodiebuddy.R
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.NavigationButton
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.system.checkPermission
import com.example.foodiebuddy.system.imagePermissionVersion
import com.example.foodiebuddy.ui.ScreenStructure
import com.example.foodiebuddy.viewModels.UserViewModel
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

    val density = LocalDensity.current
    val screenWidth = with(density) { LocalConfiguration.current.screenWidthDp * density.density }
    val screenHeight = with(density) { LocalConfiguration.current.screenHeightDp * density.density }
    val radius = minOf(screenWidth, screenHeight) / 2
    val minScale = computeMinScale(LocalContext.current, picture, radius, screenWidth, screenHeight)

    var scale by remember { mutableStateOf(minScale) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var imageWidth by remember { mutableStateOf(0f) }
    var imageHeight by remember { mutableStateOf(0f) }



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
                        scale = (scale * zoom).coerceAtLeast(minScale)

                        val scaledWidth = imageWidth * scale
                        val scaledHeight = imageHeight * scale

                        val maxOffsetX = abs(scaledWidth / 2 - radius)
                        val maxOffsetY = abs(scaledHeight / 2 - radius)

                        Log.d(
                            "Debug",
                            "scale is: $scale and offset x: $maxOffsetX and offset y: $maxOffsetY"
                        )
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
                    .onGloballyPositioned { layoutCoordinates ->
                        val size = layoutCoordinates.size
                        imageWidth = size.width.toFloat()
                        imageHeight = size.height.toFloat()
                    }
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
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxWidth().height(24.dp).background(Color.Red)
        ) {

        }
    }
}

private fun computeMinScale(context: Context, picture: Uri, radius: Float, screenWidth: Float, screenHeight: Float): Float {
    return try {
        context.contentResolver.openInputStream(picture)?.use {inputStream ->
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            var imageWidth = options.outWidth.toFloat()
            var imageHeight = options.outHeight.toFloat()
            val diameter = radius * 2
            if (imageWidth > imageHeight && imageWidth >= screenWidth) {
                val ratio = screenWidth / imageWidth
                imageWidth = screenWidth
                imageHeight *= ratio
            } else if (imageHeight > imageWidth && imageHeight >= screenHeight)  {
                val ratio = screenHeight / imageHeight
                imageHeight = diameter
                imageWidth *= ratio
            }
            val widthRatio = diameter / imageWidth
            val heightRatio = diameter / imageHeight
            val minScale = maxOf(widthRatio, heightRatio)
            minScale
        } ?: 1.5f
    } catch (e: Exception) {
        Log.d("Error", "Failed to calculate initial scale with $e")
        1.5f
    }

}
