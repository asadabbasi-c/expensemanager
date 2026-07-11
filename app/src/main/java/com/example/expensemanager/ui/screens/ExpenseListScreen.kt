package com.example.expensemanager.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensemanager.data.model.Category
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.monetization.BannerAd
import com.example.expensemanager.monetization.InterstitialAdManager
import com.example.expensemanager.monetization.ProManager
import com.example.expensemanager.ui.theme.*
import com.example.expensemanager.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.util.*

// ── Subscription brand data ────────────────────────────────────────────────────

data class SubscriptionBrand(
    val name      : String,
    val bg        : Color,
    val fg        : Color,
    val letter    : String,
    val amount    : Double,
    val categoryId: Long
)

val SUBSCRIPTION_BRANDS = listOf(
    SubscriptionBrand("Netflix",   Color(0xFFE50914), Color.White, "N",  1100.0, 5),
    SubscriptionBrand("YouTube",   Color(0xFFFF0000), Color.White, "▶", 499.0,  5),
    SubscriptionBrand("Spotify",   Color(0xFF1DB954), Color.White, "♪",  299.0,  5),
    SubscriptionBrand("ChatGPT",   Color(0xFF10A37F), Color.White, "G",  2500.0, 8),
    SubscriptionBrand("Claude",    Color(0xFFCC785C), Color.White, "C",  2500.0, 8),
    SubscriptionBrand("Amazon",    Color(0xFFFF9900), Color(0xFF232F3E), "a", 999.0, 3),
    SubscriptionBrand("Disney+",   Color(0xFF113CCF), Color.White, "D+", 799.0,  5),
    SubscriptionBrand("Apple TV+", Color(0xFF1C1C1E), Color.White, "⊕",  900.0,  5),
    SubscriptionBrand("HBO Max",   Color(0xFF5822E0), Color.White, "H",  1499.0, 5),
    SubscriptionBrand("Canva",     Color(0xFF00C4CC), Color.White, "C",  1500.0, 8),
    SubscriptionBrand("Figma",     Color(0xFFF24E1E), Color.White, "F",  1800.0, 8),
    SubscriptionBrand("GitHub",    Color(0xFF24292E), Color.White, "⌥",  1200.0, 8),
    SubscriptionBrand("LinkedIn",  Color(0xFF0A66C2), Color.White, "in", 2999.0, 8),
    SubscriptionBrand("Notion",    Color.White,       Color(0xFF191919), "N",  1600.0, 8),
    SubscriptionBrand("Dropbox",   Color(0xFF0061FF), Color.White, "⬡",  1100.0, 8),
    SubscriptionBrand("Duolingo",  Color(0xFF58CC02), Color.White, "D",  799.0,  8),
)

fun findBrandByDescription(description: String): SubscriptionBrand? {
    val lower = description.lowercase()
    return SUBSCRIPTION_BRANDS.find { lower.contains(it.name.lowercase()) }
}

// ── Brand icon composable ──────────────────────────────────────────────────────

@Composable
fun BrandBadge(brand: SubscriptionBrand, size: Int = 40) {
    val hasBorder = brand.bg == Color.White
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.28).dp))
            .background(brand.bg)
            .then(
                if (hasBorder) Modifier.border(1.dp, SurfaceVar, RoundedCornerShape((size * 0.28).dp))
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = brand.letter,
            fontSize   = (size * 0.35).sp,
            fontWeight = FontWeight.ExtraBold,
            color      = brand.fg,
            letterSpacing = (-0.02).sp
        )
    }
}

// ── Category icon composable ───────────────────────────────────────────────────

@Composable
fun CategoryBadge(category: Category?, size: Int = 40) {
    val color = remember(category?.color) {
        category?.color?.let {
            runCatching { Color(AndroidColor.parseColor(it)) }.getOrDefault(Color(0xFF888888))
        } ?: Color(0xFF888888)
    }
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Text(category?.icon ?: "💰", fontSize = (size * 0.5).sp)
    }
}

// ── Source tag ─────────────────────────────────────────────────────────────────

@Composable
fun SourceTag(source: String) {
    val (bg, fg, label) = when (source) {
        "receipt"   -> Triple(Color(0xFF1E293B), AccentBlue,   "📷 Receipt")
        "voice"     -> Triple(Color(0xFF1A2E1A), Brand400,     "🎙 Voice")
        "recurring" -> Triple(Color(0xFF1E1A2E), AccentPurple, "🔄 Auto")
        else        -> return
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bg
    ) {
        Text(
            text     = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color    = fg,
            letterSpacing = 0.02.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    viewModel            : ExpenseViewModel,
    proManager           : ProManager? = null,
    interstitialAdManager: InterstitialAdManager? = null,
    onNavigateToReceipt  : () -> Unit = {},
    onNavigateToVoice    : () -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    onSaveRecurring      : ((String, Double, Long, String, String, String) -> Unit)? = null,
    projects             : List<com.example.expensemanager.data.model.SideProject> = emptyList(),
    onCreateProject      : ((name: String, type: String, budget: Double) -> Unit)? = null,
    onUpgrade            : () -> Unit = {}
) {
    val expenses   by viewModel.expenses.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isPro      by (proManager?.isPro ?: kotlinx.coroutines.flow.MutableStateFlow(false))
        .collectAsStateWithLifecycle()
    val activity   = LocalContext.current as Activity

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val projectMap  = remember(projects) { projects.associateBy { it.id } }

    val groupedExpenses = remember(expenses) {
        expenses.groupBy { e ->
            if (e.date.length >= 7) e.date.substring(0, 7) else "Unknown"
        }.toSortedMap(compareByDescending { it })
    }

    // This month summary
    val currentMonth = remember {
        val cal = Calendar.getInstance()
        "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}"
    }
    // Summary counts only main-budget expenses (project-only ones are excluded)
    val thisMonthExpenses = remember(expenses, currentMonth) {
        expenses.filter { it.date.startsWith(currentMonth) && it.includeInMain }
    }
    val thisMonthTotal = remember(thisMonthExpenses) { thisMonthExpenses.sumOf { it.amount } }
    val lastMonthKey = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}"
    }
    val lastMonthTotal = remember(expenses, lastMonthKey) {
        expenses.filter { it.date.startsWith(lastMonthKey) && it.includeInMain }.sumOf { it.amount }
    }

    var showAddSheet  by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }

    val listState = rememberLazyListState()
    val scrolled  = remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 50 } }

    // Collapsing FAB width
    val fabWidth by animateDpAsState(
        targetValue   = if (scrolled.value) 56.dp else 160.dp,
        animationSpec = tween(300),
        label         = "fabWidth"
    )

    Scaffold(
        containerColor = Bg,
        bottomBar      = { if (!isPro) BannerAd() }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Custom top bar ─────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "My Expenses",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    val displayMonth = remember(currentMonth) {
                        val parts = currentMonth.split("-")
                        if (parts.size == 2) {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, parts[0].toInt())
                                set(Calendar.MONTH, parts[1].toInt() - 1)
                            }
                            cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + " " + parts[0]
                        } else currentMonth
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Brand400.copy(alpha = 0.09f)
                    ) {
                        Text(
                            text     = displayMonth,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color    = Brand400
                        )
                    }
                }

                if (expenses.isEmpty()) {
                    EmptyExpensesState()
                } else {
                    LazyColumn(
                        state          = listState,
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        // ── Summary header ────────────────────────────────────
                        item {
                            SummaryHeader(
                                thisMonthTotal  = thisMonthTotal,
                                lastMonthTotal  = lastMonthTotal,
                                transactionCount = thisMonthExpenses.size,
                                dayOfMonth      = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                            )
                        }

                        groupedExpenses.forEach { (month, monthExpenses) ->
                            item(key = "header_$month") {
                                MonthDivider(
                                    month = month,
                                    total = monthExpenses.sumOf { it.amount },
                                    count = monthExpenses.size
                                )
                            }
                            items(monthExpenses, key = { it.id }) { expense ->
                                ExpenseCard(
                                    expense  = expense,
                                    category = categoryMap[expense.categoryId],
                                    onDelete = { viewModel.deleteExpense(expense) },
                                    onEdit   = { expenseToEdit = expense },
                                    project  = expense.projectId?.let { projectMap[it] }
                                )
                            }
                        }
                    }
                }
            }

            // ── Collapsing FAB ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(fabWidth)
                        .height(56.dp)
                        .clip(CircleShape)
                        .background(Brand400)
                        .clickable { showAddSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text("+", fontSize = 22.sp, color = Color(0xFF0A0A0A), fontWeight = FontWeight.Bold)
                        if (!scrolled.value) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Add Expense",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color(0xFF0A0A0A),
                                maxLines   = 1
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Add expense sheet ──────────────────────────────────────────────────────
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = Surface,
            dragHandle       = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(SurfaceVar)
                    )
                }
            }
        ) {
            NewAddExpenseSheet(
                viewModel       = viewModel,
                categories      = viewModel.categories.collectAsStateWithLifecycle().value,
                onSaved         = { showAddSheet = false },
                onScanReceipt   = { showAddSheet = false; onNavigateToReceipt() },
                onVoice         = { showAddSheet = false; onNavigateToVoice() },
                onSaveRecurring = onSaveRecurring,
                projects        = projects,
                onBeforeSave    = { doSave ->
                    if (isPro || interstitialAdManager == null) {
                        doSave()
                    } else {
                        interstitialAdManager.showAdEveryOtherThenRun(activity, "expense_entry", doSave)
                    }
                },
                isPro           = isPro,
                onCreateProject = onCreateProject,
                onUpgrade       = onUpgrade
            )
        }
    }

    // ── Edit expense sheet ─────────────────────────────────────────────────────
    expenseToEdit?.let { expense ->
        ModalBottomSheet(
            onDismissRequest = { expenseToEdit = null },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = Surface,
            dragHandle       = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(SurfaceVar)
                    )
                }
            }
        ) {
            AddExpenseSheetContent(
                viewModel       = viewModel,
                onSaved         = { expenseToEdit = null },
                existingExpense = expense,
                projects        = projects,
                onBeforeSave    = { doSave ->
                    if (isPro || interstitialAdManager == null) {
                        doSave()
                    } else {
                        interstitialAdManager.showAdEveryOtherThenRun(activity, "expense_entry", doSave)
                    }
                }
            )
        }
    }
}

// ── Summary header ─────────────────────────────────────────────────────────────

@Composable
private fun SummaryHeader(
    thisMonthTotal   : Double,
    lastMonthTotal   : Double,
    transactionCount : Int,
    dayOfMonth       : Int
) {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0; minimumFractionDigits = 0
    }
    val trend = if (lastMonthTotal > 0) (thisMonthTotal - lastMonthTotal) / lastMonthTotal * 100 else 0.0
    val up    = trend > 0
    val dailyAvg = if (dayOfMonth > 0) thisMonthTotal / dayOfMonth else 0.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, SurfaceVar, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column {
            Text("This Month", fontSize = 11.sp, color = TextMuted)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "PKR ${formatter.format(thisMonthTotal)}",
                    fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = ErrorRed
                )
                if (lastMonthTotal > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (up) WarnAmber.copy(alpha = 0.1f) else Brand400.copy(alpha = 0.1f)
                    ) {
                        Text(
                            "${if (up) "▲" else "▼"} ${String.format("%.0f", kotlin.math.abs(trend))}% vs last mo",
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (up) WarnAmber else Brand400
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Divider))
            Spacer(Modifier.height(10.dp))
            Text(
                "$transactionCount transactions · Daily avg PKR ${formatter.format(dailyAvg)}",
                fontSize = 11.sp, color = TextMuted
            )
        }
    }
}

// ── Month divider ──────────────────────────────────────────────────────────────

@Composable
fun MonthDivider(month: String, total: Double, count: Int) {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0; minimumFractionDigits = 0
    }
    val displayMonth = remember(month) {
        try {
            val parts = month.split("-")
            if (parts.size == 2) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, parts[0].toInt())
                    set(Calendar.MONTH, parts[1].toInt() - 1)
                }
                "${cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())} ${parts[0]}"
            } else month
        } catch (_: Exception) { month }
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(displayMonth, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Brand400, letterSpacing = 0.02.sp)
                Text("$count transaction${if (count != 1) "s" else ""}", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("PKR", fontSize = 10.sp, color = TextMuted)
                Text(formatter.format(total), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Divider))
    }
}

// ── Expense card ───────────────────────────────────────────────────────────────

@Composable
fun ExpenseCard(
    expense: Expense,
    category: Category?,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    project: com.example.expensemanager.data.model.SideProject? = null
) {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0; minimumFractionDigits = 0
    }
    val brand     = remember(expense.description) { findBrandByDescription(expense.description) }
    val catColor  = remember(category?.color) {
        category?.color?.let {
            runCatching { Color(AndroidColor.parseColor(it)) }.getOrDefault(Color(0xFF888888))
        } ?: Color(0xFF888888)
    }
    val projectColor = remember(project?.color) {
        project?.color?.let {
            runCatching { Color(AndroidColor.parseColor(it)) }.getOrDefault(AccentBlue)
        } ?: AccentBlue
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .border(1.dp, ErrorRed.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Delete this expense?", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceEl,
                    modifier = Modifier.clickable { showDeleteConfirm = false }
                ) {
                    Text("Cancel", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, color = TextSecondary)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ErrorRed,
                    modifier = Modifier.clickable { onDelete(); showDeleteConfirm = false }
                ) {
                    Text("Delete", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, SurfaceVar, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            if (brand != null) BrandBadge(brand = brand, size = 40)
            else CategoryBadge(category = category, size = 40)

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        category?.name ?: "Other",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = catColor
                    )
                    Text(
                        formatter.format(expense.amount),
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color      = ErrorRed
                    )
                }
                if (expense.description.isNotBlank()) {
                    Text(
                        expense.description,
                        fontSize  = 12.sp,
                        color     = TextSecondary,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                        modifier  = Modifier.padding(top = 2.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dateLabel = buildString {
                        if (expense.date.isNotBlank()) append(expense.date)
                        if (expense.time.isNotBlank()) append(" · ${expense.time}")
                    }
                    Text(dateLabel, fontSize = 11.sp, color = TextMuted, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (project != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(projectColor.copy(alpha = 0.12f))
                                .border(0.5.dp, projectColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                "${project.icon} ${project.name}" + if (expense.includeInMain) "" else " · only",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = projectColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (expense.source.isNotBlank() && expense.source != "manual") {
                        SourceTag(expense.source)
                    }
                    Text(
                        "✏",
                        fontSize = 14.sp,
                        color    = TextMuted,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clickable { onEdit() }
                    )
                    Text(
                        "🗑",
                        fontSize = 14.sp,
                        color    = TextMuted,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clickable { showDeleteConfirm = true }
                    )
                }
            }
        }
    }
}

// ── Empty state ────────────────────────────────────────────────────────────────

@Composable
private fun EmptyExpensesState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            Text("💸", fontSize = 48.sp)
            Text("No expenses yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Tap + to record your first expense", fontSize = 13.sp, color = TextMuted)
        }
    }
}

// ── New add expense sheet with subscription mode ───────────────────────────────

@Composable
fun NewAddExpenseSheet(
    viewModel       : ExpenseViewModel,
    categories      : List<Category>,
    onSaved         : () -> Unit,
    onScanReceipt   : () -> Unit = {},
    onVoice         : () -> Unit = {},
    onSaveRecurring : ((String, Double, Long, String, String, String) -> Unit)? = null,
    projects        : List<com.example.expensemanager.data.model.SideProject> = emptyList(),
    onBeforeSave    : ((() -> Unit) -> Unit)? = null,
    isPro           : Boolean = true,
    onCreateProject : ((name: String, type: String, budget: Double) -> Unit)? = null,
    onUpgrade       : () -> Unit = {}
) {
    var mode              by remember { mutableStateOf("manual") }   // "manual" | "subscription"
    var amountText        by remember { mutableStateOf("") }
    var selectedCatId     by remember { mutableStateOf<Long?>(categories.firstOrNull()?.id) }
    var description       by remember { mutableStateOf("") }
    var isRecurring       by remember { mutableStateOf(false) }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    // When a project is selected: false = project-only (default), true = also in main
    var alsoCountInMain   by remember { mutableStateOf(false) }
    var selectedBrand     by remember { mutableStateOf<SubscriptionBrand?>(null) }
    var saving            by remember { mutableStateOf(false) }
    var saved             by remember { mutableStateOf(false) }

    // Inline project creation
    var showCreateProject     by remember { mutableStateOf(false) }
    var showProjectProLimit   by remember { mutableStateOf(false) }
    var pendingProjectSelect  by remember { mutableStateOf(false) }

    LaunchedEffect(categories) {
        if (selectedCatId == null && categories.isNotEmpty()) selectedCatId = categories.first().id
    }

    LaunchedEffect(projects) {
        // Auto-select a project that was just created from this sheet
        if (pendingProjectSelect && projects.isNotEmpty()) {
            selectedProjectId = projects.maxByOrNull { it.id }?.id
            pendingProjectSelect = false
        }
    }

    fun pickSubscription(brand: SubscriptionBrand) {
        selectedBrand = brand
        amountText    = brand.amount.toLong().toString()
        description   = brand.name
        // match category if exists (find by position index heuristic)
        selectedCatId = categories.getOrNull((brand.categoryId - 1).toInt().coerceIn(0, categories.size - 1))?.id
            ?: categories.firstOrNull()?.id
        isRecurring   = true
    }

    fun doSave() {
        val amt = amountText.toDoubleOrNull() ?: return
        if (amt <= 0) return
        val catId = selectedCatId ?: return
        val now   = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
        val tm    = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())
        val source = if (isRecurring) "recurring" else "manual"

        fun save() {
            viewModel.addExpense(
                com.example.expensemanager.data.model.Expense(
                    amount = amt, categoryId = catId, description = description,
                    date = now, time = tm, source = source, projectId = selectedProjectId,
                    includeInMain = selectedProjectId == null || alsoCountInMain
                )
            )
            if (isRecurring && onSaveRecurring != null) {
                onSaveRecurring(description, amt, catId, description, "monthly", now)
            }
            saved = true
            onSaved()
        }

        saving = true
        if (onBeforeSave != null) onBeforeSave { save() } else save()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // Title row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Add Expense", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(Modifier.height(14.dp))

        // Mode toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceEl)
                .padding(3.dp)
        ) {
            Row {
                listOf("manual" to "✏️ Manual", "subscription" to "📺 Subscription").forEach { (id, label) ->
                    val isSelected = mode == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Surface else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) SurfaceVar else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { mode = id }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize   = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) TextPrimary else TextMuted
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))

        // Subscription picker
        if (mode == "subscription") {
            Text("POPULAR SERVICES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.07.sp)
            Spacer(Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(SUBSCRIPTION_BRANDS) { brand ->
                    val isSelected = selectedBrand?.name == brand.name
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Brand400.copy(alpha = 0.09f) else SurfaceEl)
                            .border(
                                1.5.dp,
                                if (isSelected) Brand400 else SurfaceVar,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { pickSubscription(brand) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            BrandBadge(brand = brand, size = 36)
                            Text(brand.name, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Brand400 else TextSecondary)
                            Text("PKR ${brand.amount.toLong()}", fontSize = 9.sp, color = TextMuted)
                        }
                    }
                }
            }

            selectedBrand?.let { brand ->
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brand400.copy(alpha = 0.09f))
                        .border(1.dp, Brand400.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrandBadge(brand = brand, size = 32)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(brand.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Monthly · PKR ${brand.amount.toLong()}", fontSize = 11.sp, color = TextMuted)
                        }
                        Text("✓ Selected", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Brand400)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Divider))
            Spacer(Modifier.height(14.dp))
        }

        // Amount input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceEl)
                .border(1.5.dp, SurfaceVar, RoundedCornerShape(14.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PKR", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                androidx.compose.foundation.text.BasicTextField(
                    value        = amountText,
                    onValueChange = { amountText = it },
                    modifier     = Modifier.weight(1f),
                    textStyle    = androidx.compose.ui.text.TextStyle(
                        fontSize   = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = if (amountText.isNotEmpty()) ErrorRed else TextMuted
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine   = true,
                    decorationBox = { inner ->
                        if (amountText.isEmpty()) Text("0", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = TextMuted)
                        inner()
                    }
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // Category label
        Text("CATEGORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.07.sp)
        Spacer(Modifier.height(8.dp))

        // Category chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 4.dp)) {
            items(categories) { cat ->
                val isSelected = selectedCatId == cat.id
                val catColor = remember(cat.color) {
                    runCatching { Color(AndroidColor.parseColor(cat.color)) }.getOrDefault(Color(0xFF888888))
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Brand400.copy(alpha = 0.09f) else SurfaceEl)
                        .border(1.5.dp, if (isSelected) Brand400 else SurfaceVar, RoundedCornerShape(10.dp))
                        .clickable { selectedCatId = cat.id }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(cat.icon, fontSize = 15.sp)
                    Text(
                        cat.name,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color      = if (isSelected) Brand400 else TextSecondary
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Description
        androidx.compose.foundation.text.BasicTextField(
            value         = description,
            onValueChange = { description = it },
            modifier      = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceEl)
                .border(1.dp, SurfaceVar, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = TextPrimary),
            decorationBox = { inner ->
                if (description.isEmpty()) Text("Description (optional)", fontSize = 14.sp, color = TextMuted)
                inner()
            }
        )
        Spacer(Modifier.height(12.dp))

        // Recurring toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isRecurring) AccentPurple.copy(alpha = 0.07f) else SurfaceEl)
                .border(1.dp, if (isRecurring) AccentPurple else SurfaceVar, RoundedCornerShape(12.dp))
                .clickable { isRecurring = !isRecurring }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🔄", fontSize = 16.sp)
                Text("Make recurring", fontSize = 13.sp, color = if (isRecurring) AccentPurple else TextSecondary, modifier = Modifier.weight(1f))
                // Toggle pill
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(20.dp)
                        .clip(CircleShape)
                        .background(if (isRecurring) AccentPurple else SurfaceVar)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(if (isRecurring) Alignment.CenterEnd else Alignment.CenterStart)
                            .padding(end = if (isRecurring) 3.dp else 0.dp, start = if (!isRecurring) 3.dp else 0.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
        // Side project picker
        if (projects.isNotEmpty() || onCreateProject != null) {
            Spacer(Modifier.height(12.dp))
            Text("PROJECT (OPTIONAL)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.07.sp)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 4.dp)) {
                item {
                    val isSelected = selectedProjectId == null
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Brand400.copy(alpha = 0.09f) else SurfaceEl)
                            .border(1.5.dp, if (isSelected) Brand400 else SurfaceVar, RoundedCornerShape(10.dp))
                            .clickable { selectedProjectId = null }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("None", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Brand400 else TextSecondary)
                    }
                }
                items(projects) { project ->
                    val isSelected = selectedProjectId == project.id
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Brand400.copy(alpha = 0.09f) else SurfaceEl)
                            .border(1.5.dp, if (isSelected) Brand400 else SurfaceVar, RoundedCornerShape(10.dp))
                            .clickable { selectedProjectId = project.id }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(project.icon, fontSize = 15.sp)
                        Text(project.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Brand400 else TextSecondary)
                    }
                }
                if (onCreateProject != null) {
                    item {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceEl)
                                .border(1.5.dp, AccentBlue.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .clickable {
                                    // Free tier: 1 project
                                    if (isPro || projects.isEmpty()) showCreateProject = true
                                    else showProjectProLimit = true
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("＋", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                            Text("New Project", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AccentBlue)
                        }
                    }
                }
            }

            // Scope: project-only (default) vs also counted in main spending
            if (selectedProjectId != null) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (alsoCountInMain) Brand400.copy(alpha = 0.07f) else SurfaceEl)
                        .border(1.dp, if (alsoCountInMain) Brand400 else SurfaceVar, RoundedCornerShape(12.dp))
                        .clickable { alsoCountInMain = !alsoCountInMain }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🧮", fontSize = 16.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Also count in main spending",
                                fontSize = 13.sp,
                                color = if (alsoCountInMain) Brand400 else TextSecondary
                            )
                            Text(
                                if (alsoCountInMain) "Counted in project AND monthly budget" else "Project-only — kept out of monthly budget",
                                fontSize = 10.sp, color = TextMuted
                            )
                        }
                        Switch(checked = alsoCountInMain, onCheckedChange = { alsoCountInMain = it })
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // Action row
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceEl)
                    .border(1.dp, SurfaceVar, RoundedCornerShape(12.dp))
                    .clickable { onScanReceipt() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📷", fontSize = 16.sp)
                    Text("Receipt", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AccentBlue)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceEl)
                    .border(1.dp, SurfaceVar, RoundedCornerShape(12.dp))
                    .clickable { onVoice() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎙", fontSize = 16.sp)
                    Text("Voice", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Brand400)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (saved) Color(0xFF22C55E) else Brand400)
                    .clickable(enabled = !saving) { doSave() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (saved) "✓ Saved!" else if (saving) "..." else "Save Expense",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF0A0A0A)
                )
            }
        }
    }

    if (showCreateProject && onCreateProject != null) {
        // Shared with the Side Projects screen; opens over this sheet, so the
        // half-filled expense form underneath keeps its state.
        CreateProjectDialog(
            onCreate = { name, type, budget ->
                onCreateProject(name, type, budget)
                pendingProjectSelect = true
                showCreateProject = false
            },
            onDismiss = { showCreateProject = false }
        )
    }

    if (showProjectProLimit) {
        AlertDialog(
            onDismissRequest = { showProjectProLimit = false },
            containerColor   = Surface,
            title = { Text("Unlimited projects with Pro", color = TextPrimary) },
            text  = { Text("Free includes 1 project. Upgrade to SmartSpend Pro to create unlimited projects.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showProjectProLimit = false; onUpgrade() }) {
                    Text("Upgrade", color = Brand400, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProjectProLimit = false }) { Text("Not now", color = TextSecondary) }
            }
        )
    }
}

// Keep legacy MonthHeader for backward compatibility with any references
@Composable
fun MonthHeader(month: String, total: Double, count: Int) = MonthDivider(month, total, count)

// Keep legacy aliases
@Composable
fun SwipeToDeleteExpenseItem(expense: Expense, category: Category?, onDelete: () -> Unit, onEdit: () -> Unit = {}) =
    ExpenseCard(expense, category, onDelete, onEdit)

@Composable
fun ExpenseItem(expense: Expense, category: Category?, onDeleteClick: () -> Unit = {}, onEditClick: () -> Unit = {}) =
    ExpenseCard(expense, category, onDeleteClick, onEditClick)
