package com.example.foodiebuddy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.NavigationButton
import com.example.foodiebuddy.ui.theme.ContrastGrey
import com.example.foodiebuddy.ui.theme.MyTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenStructure(
    navigationActions: NavigationActions,
    title: String,
    navButton: NavigationButton,
    topBarIcons: @Composable () -> Unit,
    content:  LazyListScope.() -> Unit,
    bottomBar: @Composable() (() -> Unit) ?= null,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MyTypography.titleMedium
                        )
                    },
                    navigationIcon = {
                        when (navButton) {
                            NavigationButton.BURGER_MENU -> {
                                BurgerMenu()
                            }
                            NavigationButton.GO_BACK -> {
                                GoBackButton(navigationActions)
                            }
                        }
                    },
                    actions = { topBarIcons() }
                )
                Divider(color = ContrastGrey, thickness = 3.dp, modifier = Modifier.align(Alignment.BottomStart))
            }

        },
        bottomBar = { bottomBar?.invoke() },
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                content = { content() }
            )
        }
    )
}

@Composable
private fun GoBackButton(navigationActions: NavigationActions) {
    IconButton(
        onClick = { navigationActions.goBack() }
    ) {
        Icon(
            painter = painterResource(R.drawable.go_back),
            contentDescription = stringResource(R.string.desc_goBack)
        )
    }
}

@Composable
private fun BurgerMenu() {

}