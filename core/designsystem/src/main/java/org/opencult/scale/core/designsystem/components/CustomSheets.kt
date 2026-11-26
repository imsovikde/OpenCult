package org.opencult.scale.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import org.opencult.scale.core.designsystem.theme.OpenCultMotion
import org.opencult.scale.core.designsystem.theme.OpenCultTheme

/**
 * Custom Squircle Calendar Modal Sheet (`28.dp` corner radius) replacing stock modal date dialogs (`INV-18`).
 * Shows colored dots under days containing weigh-in records.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenCultCalendarSheet(
    selectedDate: LocalDate,
    datesWithMeasurements: ImmutableSet<LocalDate>,
    onSelectDate: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OpenCultTheme.colors
    var displayedMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.elevatedSurface,
        shape = OpenCultTheme.shapes.sheetSquircle,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.cardSurface)
                        .openCultMicroPress(role = Role.Button) {
                            displayedMonth = displayedMonth.minusMonths(1)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = colors.textPrimary,
                    )
                }

                Text(
                    text = "${displayedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${displayedMonth.year}",
                    style = OpenCultTheme.typography.sectionTitle,
                    color = colors.textPrimary,
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.cardSurface)
                        .openCultMicroPress(role = Role.Button) {
                            displayedMonth = displayedMonth.plusMonths(1)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Next month",
                        tint = colors.textPrimary,
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                weekdays.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = OpenCultTheme.typography.microLabel,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            val firstDay = displayedMonth.atDay(1)
            val leadingEmpty = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
            val totalDays = displayedMonth.lengthOfMonth()
            val totalCells = ((leadingEmpty + totalDays + 6) / 7) * 7

            for (weekStart in 0 until totalCells step 7) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    for (offset in 0 until 7) {
                        val cellIndex = weekStart + offset
                        val dayNum = cellIndex - leadingEmpty + 1
                        if (dayNum in 1..totalDays) {
                            val date = displayedMonth.atDay(dayNum)
                            val isSelected = date == selectedDate
                            val hasEntry = date in datesWithMeasurements

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(OpenCultTheme.shapes.chipSquircle)
                                    .background(
                                        color = if (isSelected) colors.brandCobalt else colors.elevatedSurface,
                                    )
                                    .openCultMicroPress(
                                        pressedScale = OpenCultMotion.RowPressScale,
                                        role = Role.Button,
                                    ) {
                                        onSelectDate(date)
                                        onDismiss()
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    style = OpenCultTheme.typography.numericInline,
                                    color = if (isSelected) colors.dockActiveIcon else colors.textPrimary,
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                !hasEntry -> colors.elevatedSurface.copy(alpha = 0f)
                                                isSelected -> colors.dockActiveIcon
                                                else -> colors.brandCobalt
                                            },
                                        ),
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).height(48.dp))
                        }
                    }
                }
            }
        }
    }
}

@Immutable
data class PillOption<T>(
    val value: T,
    val label: String,
    val subtitle: String? = null,
)

/**
 * Custom animated pill selector row replacing stock [Spinner] / [DropdownMenu] (`INV-18`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> OpenCultPillSelectorRow(
    options: ImmutableList<PillOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenCultTheme.colors
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(OpenCultTheme.shapes.pill)
                    .background(if (isSelected) colors.dockBackground else colors.cardSurface)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) colors.dockBackground else colors.borderSubtle,
                        shape = OpenCultTheme.shapes.pill,
                    )
                    .openCultMicroPress(
                        pressedScale = OpenCultMotion.RowPressScale,
                        role = Role.RadioButton,
                    ) {
                        onSelect(option.value)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.label,
                    style = OpenCultTheme.typography.buttonLabel,
                    color = if (isSelected) colors.dockActiveIcon else colors.textPrimary,
                )
            }
        }
    }
}

/** Custom modal pill chooser sheet replacing stock [DropdownMenu] dialogs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OpenCultPillSelectorSheet(
    title: String,
    options: ImmutableList<PillOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OpenCultTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.elevatedSurface,
        shape = OpenCultTheme.shapes.sheetSquircle,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = OpenCultTheme.typography.sectionTitle,
                color = colors.textPrimary,
            )
            options.forEach { opt ->
                val isSelected = opt.value == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clip(OpenCultTheme.shapes.bentoSquircle)
                        .background(if (isSelected) colors.cobaltSubtle else colors.cardSurface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) colors.brandCobalt else colors.borderSubtle,
                            shape = OpenCultTheme.shapes.bentoSquircle,
                        )
                        .openCultMicroPress(
                            pressedScale = OpenCultMotion.RowPressScale,
                            role = Role.RadioButton,
                        ) {
                            onSelect(opt.value)
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = opt.label,
                            style = OpenCultTheme.typography.buttonLabel,
                            color = colors.textPrimary,
                        )
                        if (!opt.subtitle.isNullOrBlank()) {
                            Text(
                                text = opt.subtitle,
                                style = OpenCultTheme.typography.body,
                                color = colors.textSecondary,
                            )
                        }
                    }
                    if (isSelected) {
                        OpenCultStatusChip(label = "Selected")
                    }
                }
            }
        }
    }
}
