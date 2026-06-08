package com.example.fluentui.ui.homescreen.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.fluentui.R

@Composable
fun NetflixCardTile(
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1f
) {
    val netflixRed = Color(0xFFE50914)
    val backgroundBlack = Color(0xFF000000)

    val gradientBrush = remember(netflixRed, backgroundBlack) {
        Brush.linearGradient(
            colorStops = arrayOf(
                0.0f to netflixRed,
                0.25f to backgroundBlack,
                0.75f to backgroundBlack,
                1.0f to netflixRed
            ),
            start = Offset(0f, 0f),
            end = Offset(2000f, 1000f)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp
    ) {
        // Utilisation de BoxWithConstraints pour un calcul responsive
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradientBrush),
            contentAlignment = Alignment.Center
        ) {
            // Ici, le logo s'adapte à la taille disponible (ex: 30% de la largeur du parent)
            val iconSize = maxWidth * 0.3f

            Image(
                painter = painterResource(id = R.drawable.netflix),
                contentDescription = "Netflix Logo",
                modifier = Modifier.size(iconSize),
                contentScale = ContentScale.Fit
            )
        }
    }
}