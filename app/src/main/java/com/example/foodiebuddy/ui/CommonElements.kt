package com.example.foodiebuddy.ui

import android.icu.text.UnicodeSet.SpanCondition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
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
    navExtraActions : () -> Unit,
    topBarIcons: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
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
                                GoBackButton(navigationActions, navExtraActions)
                            }
                        }
                    },
                    actions = { topBarIcons() }
                )
                Divider(color = ContrastGrey, thickness = 3.dp, modifier = Modifier.align(Alignment.BottomStart))
            }

        },
        bottomBar = { bottomBar?.invoke() },
        content = { content(it) }
    )
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
private fun BurgerMenu() {

}