package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.SearchFilter
import com.example.ui.theme.GeminiPurple
import com.example.ui.theme.GoogleBlue

@Composable
fun FilterChipsRow(
    selectedFilter: SearchFilter,
    onFilterSelected: (SearchFilter) -> Unit,
    isDeepThinking: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchFilter.entries.forEach { filter ->
            val isSelected = (selectedFilter == filter) || (filter == SearchFilter.DEEP_THINK && isDeepThinking)
            val icon: ImageVector = when (filter) {
                SearchFilter.ALL -> Icons.Default.FilterList
                SearchFilter.AI_OVERVIEW -> Icons.Default.AutoAwesome
                SearchFilter.NEWS -> Icons.Default.Newspaper
                SearchFilter.IMAGES -> Icons.Default.Image
                SearchFilter.WEB -> Icons.Default.Language
                SearchFilter.DEEP_THINK -> Icons.Default.Psychology
            }

            val activeColor by animateColorAsState(
                targetValue = if (filter == SearchFilter.DEEP_THINK || filter == SearchFilter.AI_OVERVIEW) {
                    GeminiPurple
                } else {
                    GoogleBlue
                },
                label = "chip_color"
            )

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = activeColor.copy(alpha = 0.15f),
                    selectedLabelColor = activeColor
                ),
                modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
            )
        }
    }
}
