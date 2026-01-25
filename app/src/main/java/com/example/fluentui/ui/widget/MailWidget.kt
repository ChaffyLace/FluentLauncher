package com.example.fluentui.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize

@Composable
fun MailWidget(config: TileConfig) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF005A9E)) // Bleu foncé Mail
            .padding(10.dp)
    ) {
        // Compteur de notifications en haut à droite
        Text(
            text = "3",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            Icon(Icons.Default.Email, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))

            // On affiche le détail seulement si ce n'est pas une tuile minuscule
            if (config.size != TileSize.TINY) {
                Text(
                    text = "Outlook",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Nouveaux messages reçus",
                    color = Color.White.copy(0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}