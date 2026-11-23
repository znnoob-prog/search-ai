package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GeminiAuroraMiddle
import com.example.ui.theme.GeminiAuroraStart
import com.example.ui.theme.GeminiPurple
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import com.example.ui.theme.ThemeMode
import com.google.firebase.auth.FirebaseUser

@Composable
fun GoogleHeader(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    bookmarkCount: Int,
    onOpenBookmarks: () -> Unit,
    onOpenAssistant: () -> Unit,
    currentUser: FirebaseUser?,
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var themeMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Multi-colored Google Logo + AI Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("google_logo_header")
        ) {
            Text(
                text = "G",
                color = GoogleBlue,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "o",
                color = GoogleRed,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "o",
                color = GoogleYellow,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "g",
                color = GoogleBlue,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "l",
                color = GoogleGreen,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "e",
                color = GoogleRed,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.width(6.dp))

            // AI Badge in gradient pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(GeminiAuroraStart, GeminiAuroraMiddle, GeminiPurple)
                        )
                    )
                    .clickable { onOpenAssistant() }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .testTag("ai_sparkle_badge")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Powered",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "AI",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Action Buttons: Theme Toggle, Bookmarks, and Google Account
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Theme Mode Selector
            Box {
                IconButton(
                    onClick = { themeMenuExpanded = true },
                    modifier = Modifier.testTag("theme_toggle_button")
                ) {
                    val icon = when (currentThemeMode) {
                        ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                        ThemeMode.LIGHT -> Icons.Default.LightMode
                        ThemeMode.DARK -> Icons.Default.DarkMode
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Change Theme Mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = themeMenuExpanded,
                    onDismissRequest = { themeMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("System Default") },
                        leadingIcon = { Icon(Icons.Default.SettingsBrightness, null) },
                        onClick = {
                            onThemeModeChange(ThemeMode.SYSTEM)
                            themeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Light Mode") },
                        leadingIcon = { Icon(Icons.Default.LightMode, null) },
                        onClick = {
                            onThemeModeChange(ThemeMode.LIGHT)
                            themeMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Dark Mode") },
                        leadingIcon = { Icon(Icons.Default.DarkMode, null) },
                        onClick = {
                            onThemeModeChange(ThemeMode.DARK)
                            themeMenuExpanded = false
                        }
                    )
                }
            }

            // Bookmarks Icon
            IconButton(
                onClick = onOpenBookmarks,
                modifier = Modifier.testTag("bookmarks_button")
            ) {
                if (bookmarkCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge { Text("$bookmarkCount") }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Saved Results",
                            tint = GoogleBlue
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Saved Results",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Google Profile / Sign In Button
            if (currentUser != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(GoogleBlue)
                        .clickable { onOpenAccount() }
                        .testTag("user_avatar_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (currentUser.displayName?.take(1) ?: currentUser.email?.take(1) ?: "U").uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenAccount() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("sign_in_header_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sign in",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
