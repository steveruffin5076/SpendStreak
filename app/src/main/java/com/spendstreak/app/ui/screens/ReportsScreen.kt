package com.spendstreak.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendstreak.app.ads.BannerAdView
import com.spendstreak.app.data.Category
import com.spendstreak.app.data.Expense
import com.spendstreak.app.data.Income
import com.spendstreak.app.ui.components.DateRangeSection
import com.spendstreak.app.ui.components.MILLIS_PER_DAY
import com.spendstreak.app.ui.components.RetroPanel
import com.spendstreak.app.util.formatCurrency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class ReportRange { WEEK, MONTH, CUSTOM }

private data class TrendBucket(val label: String, val income: Double, val expense: Double)

private val WEEK_LABEL_FORMATTER = DateTimeFormatter.ofPattern("EEE")
private val BUCKET_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM d")

// Transfers are deliberately excluded everywhere in this screen — moving money between
// your own accounts is neither income nor expense.
@Composable
fun ReportsScreen(
    expenses: List<Expense>,
    income: List<Income>,
    categories: List<Category>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var range by rememberSaveable { mutableStateOf(ReportRange.WEEK) }
    var startDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var endDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }

    // Copied to locals so the null-checks below can smart-cast (rememberSaveable-delegated
    // vars can't be smart-cast directly) — same pattern BudgetScreen's submit() uses.
    val start = startDateMillis
    val end = endDateMillis
    val customRangeInvalid = range == ReportRange.CUSTOM && start != null && end != null && end < start

    val (periodStart, periodEnd) = periodBounds(range, startDateMillis, endDateMillis)

    val periodExpenses = remember(expenses, periodStart, periodEnd) {
        expenses.filter { it.timestampMillis in periodStart until periodEnd }
    }
    val periodIncome = remember(income, periodStart, periodEnd) {
        income.filter { it.timestampMillis in periodStart until periodEnd }
    }
    val categoryNameById = remember(categories) { categories.associate { it.id to it.name } }
    val categoryBreakdown = remember(periodExpenses, categoryNameById) {
        periodExpenses.groupBy { it.categoryId }
            .map { (categoryId, list) -> (categoryNameById[categoryId] ?: "Other") to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
    }
    val sourceBreakdown = remember(periodIncome, categoryNameById) {
        periodIncome.groupBy { it.categoryId }
            .map { (categoryId, list) -> (categoryNameById[categoryId] ?: "Other") to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
    }
    val trend = remember(range, expenses, income, periodStart, periodEnd) {
        computeTrend(range, expenses, income, periodStart, periodEnd)
    }

    Column(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(text = "REPORTS", style = MaterialTheme.typography.headlineMedium)
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ReportRange.entries) { option ->
                FilterChip(
                    selected = range == option,
                    onClick = { range = option },
                    label = { Text(option.name) }
                )
            }
        }

        if (range == ReportRange.CUSTOM) {
            DateRangeSection(
                startMillis = startDateMillis,
                endMillis = endDateMillis,
                onStartChange = { startDateMillis = it },
                onEndChange = { endDateMillis = it }
            )
        }

        if (customRangeInvalid) {
            Text(
                text = "End date must be on or after the start date.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else if (periodExpenses.isEmpty() && periodIncome.isEmpty()) {
            Text(
                text = "No transactions in this period.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            RetroPanel(modifier = Modifier.fillMaxWidth()) {
                Text(text = "BREAKDOWN", style = MaterialTheme.typography.labelLarge)

                Text(
                    text = "EXPENSES BY CATEGORY",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
                if (categoryBreakdown.isEmpty()) {
                    Text(text = "None logged.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val maxCategoryAmount = categoryBreakdown.first().second
                    categoryBreakdown.forEach { (category, amount) ->
                        BreakdownRow(
                            label = category,
                            amount = amount,
                            maxAmount = maxCategoryAmount,
                            barColor = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Text(
                    text = "INCOME BY SOURCE",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
                if (sourceBreakdown.isEmpty()) {
                    Text(text = "None logged.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val maxSourceAmount = sourceBreakdown.first().second
                    sourceBreakdown.forEach { (source, amount) ->
                        BreakdownRow(
                            label = source,
                            amount = amount,
                            maxAmount = maxSourceAmount,
                            barColor = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            RetroPanel(modifier = Modifier.fillMaxWidth()) {
                Text(text = "TREND", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "Income (green) vs expense (red) per period",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
                val maxTrendValue = trend.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0.0
                if (range == ReportRange.WEEK) {
                    // WEEK is always exactly 7 buckets (see computeTrend) — lay them out with
                    // equal weight so all 7 always fit on screen, instead of a LazyRow that can
                    // silently scroll a couple of days off the visible edge with no scroll cue.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        trend.forEach { bucket ->
                            TrendBucketBars(
                                bucket = bucket,
                                maxValue = maxTrendValue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    // MONTH/CUSTOM bucket by week and can have many buckets (a long custom
                    // range) — scrolling is the right behavior here, unlike the fixed 7-item
                    // WEEK case above.
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(trend) { bucket ->
                            TrendBucketBars(bucket, maxTrendValue)
                        }
                    }
                }
            }
        }
    }
    BannerAdView(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun BreakdownRow(label: String, amount: Double, maxAmount: Double, barColor: Color) {
    Column(modifier = Modifier.padding(top = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Text(text = formatCurrency(amount), style = MaterialTheme.typography.bodyMedium)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .padding(top = 4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val fraction = if (maxAmount > 0) (amount / maxAmount).toFloat().coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun TrendBucketBars(bucket: TrendBucket, maxValue: Double, modifier: Modifier = Modifier.width(56.dp)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(80.dp)
        ) {
            val incomeFraction = if (maxValue > 0) (bucket.income / maxValue).toFloat().coerceIn(0f, 1f) else 0f
            val expenseFraction = if (maxValue > 0) (bucket.expense / maxValue).toFloat().coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight(incomeFraction)
                    .background(MaterialTheme.colorScheme.tertiary)
            )
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight(expenseFraction)
                    .background(MaterialTheme.colorScheme.error)
            )
        }
        Text(
            text = bucket.label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun periodBounds(range: ReportRange, startDateMillis: Long?, endDateMillis: Long?): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    return when (range) {
        ReportRange.WEEK -> {
            val end = System.currentTimeMillis()
            (end - 7 * MILLIS_PER_DAY) to end
        }
        ReportRange.MONTH -> {
            val start = LocalDate.now(zone).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
            start to System.currentTimeMillis()
        }
        ReportRange.CUSTOM -> {
            val start = startDateMillis
                ?.let { LocalDate.ofEpochDay(it / MILLIS_PER_DAY).atStartOfDay(zone).toInstant().toEpochMilli() }
                ?: 0L
            val end = endDateMillis
                ?.let { LocalDate.ofEpochDay(it / MILLIS_PER_DAY).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() }
                ?: System.currentTimeMillis()
            start to end
        }
    }
}

// WEEK buckets by day; MONTH/CUSTOM bucket by week — re-filtering the full lists per
// bucket is fine at this screen's scale (a handful of buckets, local to Reports only).
private fun computeTrend(
    range: ReportRange,
    expenses: List<Expense>,
    income: List<Income>,
    periodStart: Long,
    periodEnd: Long
): List<TrendBucket> {
    val zone = ZoneId.systemDefault()
    val bucketMillis = if (range == ReportRange.WEEK) MILLIS_PER_DAY else MILLIS_PER_DAY * 7
    val labelFormatter = if (range == ReportRange.WEEK) WEEK_LABEL_FORMATTER else BUCKET_LABEL_FORMATTER

    val buckets = mutableListOf<TrendBucket>()
    var bucketStart = periodStart
    while (bucketStart < periodEnd) {
        val bucketEnd = minOf(bucketStart + bucketMillis, periodEnd)
        val bucketExpense = expenses
            .filter { it.timestampMillis in bucketStart until bucketEnd }
            .sumOf { it.amount }
        val bucketIncome = income
            .filter { it.timestampMillis in bucketStart until bucketEnd }
            .sumOf { it.amount }
        val label = Instant.ofEpochMilli(bucketStart).atZone(zone).toLocalDate().format(labelFormatter)
        buckets.add(TrendBucket(label, bucketIncome, bucketExpense))
        bucketStart = bucketEnd
    }
    return buckets
}
