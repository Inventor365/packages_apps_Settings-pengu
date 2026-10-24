/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-FileCopyrightText: AxionOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.display

import android.app.WallpaperManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.framework.theme.surfaceTone
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Dedicated Display page for window blur enablement and intensity, with a live QS-style preview.
 */
class BlurSettings : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SettingsTheme {
                    BlurSettingsScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.window_blurs)
    }

    @Composable
    private fun BlurSettingsScreen() {
        val context = LocalContext.current
        val cr = context.contentResolver

        var blursEnabled by remember {
            mutableStateOf(
                Settings.Global.getInt(cr, Settings.Global.DISABLE_WINDOW_BLURS, 0) == 0
            )
        }

        var blurIntensity by remember {
            mutableFloatStateOf(
                Settings.System.getInt(cr, Settings.System.BLUR_INTENSITY, DEFAULT_BLUR_INTENSITY)
                    .coerceIn(MIN_BLUR_INTENSITY, MAX_BLUR_INTENSITY)
                    .toFloat()
            )
        }

        var blurStyle by remember {
            val lgOn = Settings.Secure.getInt(cr, KEY_LIQUID_GLASS, 0) != 0
            val savedStyle =
                Settings.Secure.getInt(
                    cr,
                    KEY_BLUR_STYLE,
                    if (lgOn) BLUR_STYLE_LIQUID_GLASS else BLUR_STYLE_IOS_FROSTED,
                )
            mutableIntStateOf(if (lgOn) BLUR_STYLE_LIQUID_GLASS else savedStyle.coerceIn(0, 2))
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            ) {
                item {
                    BlurIllustration(blursEnabled, blurIntensity, blurStyle)
                }

                item {
                    val windowBlursTitle = stringResource(R.string.window_blurs)
                    SwitchPreference(
                        object : SwitchPreferenceModel {
                            override val title = windowBlursTitle
                            override val checked = { blursEnabled }
                            override val onCheckedChange: (Boolean) -> Unit = { newValue ->
                                blursEnabled = newValue
                                Settings.Global.putInt(
                                    cr,
                                    Settings.Global.DISABLE_WINDOW_BLURS,
                                    if (newValue) 0 else 1,
                                )
                            }
                        }
                    )
                }

                item {
                    BlurStyleSelectorCard(
                        selectedStyle = blurStyle,
                        enabled = blursEnabled,
                        onStyleSelected = { newStyle ->
                            blurStyle = newStyle
                            Settings.Secure.putInt(cr, KEY_BLUR_STYLE, newStyle)
                            Settings.Secure.putInt(
                                cr,
                                KEY_LIQUID_GLASS,
                                if (newStyle == BLUR_STYLE_LIQUID_GLASS) 1 else 0,
                            )
                        },
                    )
                }

                item {
                    BlurIntensitySlider(
                        value = blurIntensity,
                        enabled = blursEnabled,
                        onValueChange = { blurIntensity = it },
                        onValueChangeFinished = {
                            val clamped =
                                blurIntensity.roundToInt()
                                    .coerceIn(MIN_BLUR_INTENSITY, MAX_BLUR_INTENSITY)
                            Settings.System.putInt(
                                cr,
                                Settings.System.BLUR_INTENSITY,
                                clamped,
                            )
                            Settings.Secure.putInt(
                                cr,
                                KEY_SYSTEM_BLUR_RADIUS,
                                clamped,
                            )
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun BlurStyleSelectorCard(
        selectedStyle: Int,
        enabled: Boolean,
        onStyleSelected: (Int) -> Unit,
    ) {
        val options = listOf(
            Triple(
                BLUR_STYLE_LIQUID_GLASS,
                stringResource(R.string.blur_style_liquid_glass),
                stringResource(R.string.blur_style_liquid_glass_summary),
            ),
            Triple(
                BLUR_STYLE_IOS_FROSTED,
                stringResource(R.string.blur_style_ios_frosted),
                stringResource(R.string.blur_style_ios_frosted_summary),
            ),
            Triple(
                BLUR_STYLE_STANDARD,
                stringResource(R.string.blur_style_standard),
                stringResource(R.string.blur_style_standard_summary),
            ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.blur_style_title),
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier.padding(bottom = 4.dp),
            )
            options.forEach { (styleValue, title, summary) ->
                val isSelected = selectedStyle == styleValue
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected && enabled) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable(enabled = enabled) { onStyleSelected(styleValue) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = if (enabled) ({ onStyleSelected(styleValue) }) else null,
                        enabled = enabled,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun BlurIntensitySlider(
        value: Float,
        enabled: Boolean,
        onValueChange: (Float) -> Unit,
        onValueChangeFinished: () -> Unit,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.blur_intensity_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
                Text(
                    text = stringResource(
                        R.string.blur_intensity_value,
                        value.roundToInt(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                valueRange = MIN_BLUR_INTENSITY.toFloat()..MAX_BLUR_INTENSITY.toFloat(),
                onValueChangeFinished = onValueChangeFinished,
                colors = SliderDefaults.colors(
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceTone,
                ),
            )
        }
    }

    @Composable
    private fun BlurIllustration(enabled: Boolean, intensityPct: Float, blurStyle: Int) {
        val context = LocalContext.current

        val layerFg = Color(context.getColor(com.android.internal.R.color.shade_panel_fg))
        val layerBg = Color(context.getColor(com.android.internal.R.color.shade_panel_bg))
        val compositeColor = ColorUtils.compositeColors(layerFg.toArgb(), layerBg.toArgb())
        val shadeColor = when (blurStyle) {
            BLUR_STYLE_LIQUID_GLASS -> Color(0x400A0A10)
            BLUR_STYLE_IOS_FROSTED -> Color(0x52121218)
            else -> Color(compositeColor)
        }

        val previewBlurDp =
            (MAX_PREVIEW_BLUR_RADIUS_DP * intensityPct / DEFAULT_BLUR_INTENSITY.toFloat())
                .coerceAtLeast(0f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    RoundedCornerShape(32.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(136.dp)
                    .height(280.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(4.dp, Color.Black.copy(alpha = 0.9f), RoundedCornerShape(32.dp))
                    .background(Color.Black)
                    .padding(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(26.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(26.dp)),
                    ) {
                        WallpaperImage(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (enabled && previewBlurDp > 0f) {
                                        Modifier.blur(previewBlurDp.dp)
                                    } else {
                                        Modifier
                                    },
                                ),
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (enabled) {
                                        shadeColor
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    },
                                ),
                        )

                        QSContent(enabled = enabled, blurStyle = blurStyle)
                    }

                    StatusBar()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 7.dp)
                        .width(34.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            if (enabled && blurStyle == BLUR_STYLE_LIQUID_GLASS) {
                                Color(0xCC18181E)
                            } else if (enabled && blurStyle == BLUR_STYLE_IOS_FROSTED) {
                                Color(0xD916161C)
                            } else {
                                Color.Black
                            }
                        )
                        .border(
                            0.6.dp,
                            if (enabled && blurStyle != BLUR_STYLE_STANDARD) {
                                Color.White.copy(alpha = if (blurStyle == BLUR_STYLE_LIQUID_GLASS) 0.45f else 0.22f)
                            } else {
                                Color.Transparent
                            },
                            RoundedCornerShape(5.dp),
                        ),
                )
            }
        }
    }

    @Composable
    private fun WallpaperImage(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val wallpaperManager = remember { WallpaperManager.getInstance(context) }
        val drawable = remember {
            try {
                wallpaperManager.drawable
            } catch (_: Exception) {
                null
            }
        }

        if (drawable != null) {
            val painter = rememberDrawablePainter(drawable)
            Image(
                painter = painter,
                contentDescription = null,
                modifier = modifier,
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = modifier.background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ),
                ),
            )
        }
    }

    @Composable
    private fun rememberDrawablePainter(drawable: Drawable?): Painter {
        return remember(drawable) {
            object : Painter() {
                override val intrinsicSize: Size
                    get() = drawable?.let {
                        Size(it.intrinsicWidth.toFloat(), it.intrinsicHeight.toFloat())
                    } ?: Size.Unspecified

                override fun DrawScope.onDraw() {
                    drawable?.let {
                        it.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                        it.draw(drawContext.canvas.nativeCanvas)
                    }
                }
            }
        }
    }

    @Composable
    private fun StatusBar() {
        val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
        var time by remember { mutableStateOf(LocalTime.now().format(formatter)) }

        LaunchedEffect(Unit) {
            while (true) {
                time = LocalTime.now().format(formatter)
                delay(30_000)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                time,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp, 6.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)),
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)),
                )
            }
        }
    }

    @Composable
    private fun QSContent(enabled: Boolean = true, blurStyle: Int = BLUR_STYLE_IOS_FROSTED) {
        val icons = listOf(
            Icons.Filled.Wifi,
            Icons.Filled.Bluetooth,
            Icons.Filled.FlashlightOn,
            Icons.Filled.Settings,
            Icons.Filled.AirplanemodeActive,
            Icons.Filled.Nfc,
            Icons.Filled.Adb,
            Icons.Filled.NotificationsActive,
        )

        val tileSurface = when {
            !enabled -> MaterialTheme.colorScheme.surfaceBright
            blurStyle == BLUR_STYLE_LIQUID_GLASS -> Color.White.copy(alpha = 0.22f)
            blurStyle == BLUR_STYLE_IOS_FROSTED -> Color.White.copy(alpha = 0.16f)
            else -> MaterialTheme.colorScheme.surfaceBright
        }
        val tileBorder = when {
            !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            blurStyle == BLUR_STYLE_LIQUID_GLASS -> Color.White.copy(alpha = 0.55f)
            blurStyle == BLUR_STYLE_IOS_FROSTED -> Color.White.copy(alpha = 0.28f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            repeat(4) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(2) { colIndex ->
                        val iconIndex = (rowIndex * 2 + colIndex) % icons.size
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(tileSurface)
                                .border(
                                    if (enabled && blurStyle == BLUR_STYLE_LIQUID_GLASS) 0.8.dp else 0.5.dp,
                                    tileBorder,
                                    RoundedCornerShape(12.dp),
                                ),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.Transparent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = icons[iconIndex],
                                        contentDescription = null,
                                        modifier = Modifier.size(8.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .height(2.dp)
                                        .weight(1f)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        ),
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceBright),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Icon(
                    imageVector = Icons.Filled.Brightness4,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp).size(7.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceBright),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    companion object {
        private const val MIN_BLUR_INTENSITY = 0
        private const val MAX_BLUR_INTENSITY = 200
        private const val DEFAULT_BLUR_INTENSITY = 100
        private const val MAX_PREVIEW_BLUR_RADIUS_DP = 34f

        private const val KEY_LIQUID_GLASS = "penguin_liquid_glass"
        private const val KEY_BLUR_STYLE = "penguin_blur_style"
        private const val KEY_SYSTEM_BLUR_RADIUS = "system_blur_radius"

        const val BLUR_STYLE_STANDARD = 0
        const val BLUR_STYLE_IOS_FROSTED = 1
        const val BLUR_STYLE_LIQUID_GLASS = 2
    }
}
