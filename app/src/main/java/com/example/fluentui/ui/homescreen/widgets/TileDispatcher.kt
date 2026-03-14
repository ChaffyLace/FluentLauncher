package com.example.fluentui.ui.homescreen.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileType

@Composable
fun TileContentDispatcher(config: TileConfig, contentColor: Color, accentColor: Color) {
    when (config.type) {
        TileType.CLOCK -> ClockWidget(config, contentColor, accentColor)
        TileType.TIMER -> TimerWidget(config, contentColor, accentColor)
        TileType.STOPWATCH -> StopwatchWidget(config, contentColor, accentColor)
        TileType.CALENDAR -> CalendarWidget(config, contentColor)
        else -> StandardAppTile(config, contentColor)
    }
}