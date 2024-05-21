package com.example.foodiebuddy.ui


import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.foodiebuddy.R
import com.example.foodiebuddy.navigation.BURGER_DESTINATIONS
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.ui.theme.ContrastGrey
import com.example.foodiebuddy.ui.theme.MyPurple
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.UserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryScreen(
    title: String,
    navigationActions: NavigationActions,
    navExtraActions : () -> Unit,
    topBarIcons: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box {
                CenterAlignedTopAppBar(
                    title = { Text(text = title, style = MyTypography.titleMedium)},
                    navigationIcon = {
                        GoBackButton(navigationActions, navExtraActions)
                    },
                    actions = {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            topBarIcons()
                        }
                    }
                )}
        },
        content = { content(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimaryScreen(
    navigationActions: NavigationActions,
    title: String,
    topBarIcons: @Composable () -> Unit,
    userViewModel: UserViewModel,
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val userData by userViewModel.userData.collectAsState()
    val nameState = remember { mutableStateOf(userData.username) }
    val pictureState = remember { mutableStateOf(userData.picture) }

    LaunchedEffect(userData) {
        nameState.value = userData.username
        pictureState.value = userData.picture
    }

    ModalNavigationDrawer(
        modifier = Modifier.fillMaxSize(),
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.requiredWidth(200.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.size(16.dp))
                    RoundImage(64.dp, pictureState.value, stringResource(R.string.desc_profilePic))
                    Text(text = nameState.value, style = MyTypography.bodyMedium)
                    Divider(color = ContrastGrey, thickness = 3.dp)
                }
                BURGER_DESTINATIONS.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(text = stringResource(item.text), style = MyTypography.bodyMedium) },
                        selected = false,
                        onClick = {
                            navigationActions.navigateTo(item.route)
                            scope.launch { drawerState.close() }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(item.icon),
                                contentDescription = stringResource(R.string.desc_dstIcon)
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        // screen content
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Box {
                    CenterAlignedTopAppBar(
                        title = { Text(text = title, style = MyTypography.titleMedium) },
                        navigationIcon = { BurgerMenu(scope, drawerState) },
                        actions = { topBarIcons() }
                    )
                    Divider(color = ContrastGrey, thickness = 3.dp, modifier = Modifier.align(Alignment.BottomStart))
                }

            },
            bottomBar = {

            },
            content = { content(it) }
        )
    }

}
@Composable
fun LoadingPage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = stringResource(R.string.desc_loading))
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing)
            ), label = stringResource(R.string.desc_loading)
        )

        Canvas(modifier = Modifier.size((100f).dp)) {
            drawArc(
                color = MyPurple,
                startAngle = angle,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )
        }
    }
}
@Composable
fun RoundImage(size: Dp, picture: Uri, contentDescription: String) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Transparent)
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = rememberAsyncImagePainter(picture),
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds
        )
    }
}
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    icon: Int, placeHolder: String,
    singleLine: Boolean,
    maxLength: Int
) {
    TextField(
        modifier = if (singleLine) {Modifier.width(300.dp)} else {
            Modifier
                .width(300.dp)
                .height(120.dp)},
        value = value,
        onValueChange = {
            if (it.length <= maxLength) {
                onValueChange(it)
            }
        },
        textStyle = MyTypography.bodyMedium,
        prefix = {
            Row{
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = stringResource(R.string.desc_textFieldIcon),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(16.dp))
            }
        },
        placeholder = {
            Text(text = placeHolder, style = MyTypography.bodySmall)
        },
        singleLine = singleLine,
        supportingText = {
            Text(text = stringResource(R.string.field_maxChar, maxLength), style = MyTypography.labelSmall)
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = MyPurple,
            focusedIndicatorColor = MyPurple
        )
    )
}

@Composable
private fun GoBackButton(navigationActions: NavigationActions, navExtraActions: () -> Unit) {
    IconButton(
        onClick = {
            navigationActions.goBack()
            navExtraActions()
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.go_back),
            contentDescription = stringResource(R.string.desc_goBack)
        )
    }
}

@Composable
private fun BurgerMenu(scope: CoroutineScope, drawerState: DrawerState) {
    IconButton(
        onClick = { scope.launch { drawerState.open() }}
    ) {
        Icon(
            painter = painterResource(R.drawable.burger_menu),
            contentDescription = stringResource(R.string.desc_burgerMenu),
            modifier = Modifier.size(28.dp)
        )
    }
}