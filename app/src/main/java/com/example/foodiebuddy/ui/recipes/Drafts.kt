package com.example.foodiebuddy.ui.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodiebuddy.R
import com.example.foodiebuddy.navigation.NavigationActions
import com.example.foodiebuddy.navigation.Route
import com.example.foodiebuddy.ui.SecondaryScreen
import com.example.foodiebuddy.ui.theme.MyTypography
import com.example.foodiebuddy.viewModels.OfflineDataViewModel

@Composable
fun Drafts(offDataVM: OfflineDataViewModel, navigationActions: NavigationActions) {

    val drafts by offDataVM.drafts.collectAsState()

    SecondaryScreen(
        title = stringResource(R.string.button_drafts),
        navigationActions = navigationActions,
        navExtraActions = {},
        topBarIcons = {}) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(start = 16.dp, end = 16.dp)
            ) {
                // case where there are no drafts
                if (drafts.isEmpty()) {
                    item {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            text = stringResource(R.string.txt_noDrafts),
                            style = MyTypography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(drafts, key = {it.id}) { draft ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clickable { navigationActions.navigateTo("${Route.EDIT_DRAFT}/${draft.id}") },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = draft.name.ifBlank { stringResource(R.string.txt_unnamed) },
                                    style = MyTypography.bodyMedium,
                                    fontStyle = if (draft.name.isEmpty()) FontStyle.Italic else FontStyle.Normal
                                )
                                IconButton(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .width(40.dp)
                                        .padding(end = 16.dp),
                                    onClick = { offDataVM.deleteDraft(draft.id) }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.bin),
                                        contentDescription = stringResource(R.string.desc_delete),
                                    )
                                }
                            }
                        }
                    }
                }
            }
    }
}