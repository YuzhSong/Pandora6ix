package com.pandora6ix.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.pandora6ix.app.data.UserRepository
import com.pandora6ix.app.ui.theme.Ink
import com.pandora6ix.app.ui.theme.WarmOrange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Shared review/team avatar: real authenticated image, otherwise the actual name's initial. */
@Composable
fun RemoteAvatar(userId: String, name: String, size: Dp, fontSize: TextUnit, userRepo: UserRepository?, fallback: (@Composable () -> Unit)? = null) {
    var photo by remember(userId, userRepo) { mutableStateOf<Bitmap?>(null) }
    val revision = userRepo?.avatarRevision?.collectAsState()?.value ?: 0L
    LaunchedEffect(userId, userRepo, revision) {
        if (userId.isBlank() || userRepo == null) return@LaunchedEffect
        while (isActive) {
            try { photo = userRepo.avatar(userId) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Keep the last displayed image during transient failures. */ }
            delay(15_000)
        }
    }
    Box(Modifier.size(size).clip(CircleShape).background(WarmOrange.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
        if (photo != null) Image(photo!!.asImageBitmap(), "$name 的头像", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else if (fallback != null) fallback()
        else Text(name.take(1).ifBlank { "?" }, fontSize = fontSize, fontWeight = FontWeight.Bold, color = Ink)
    }
}
