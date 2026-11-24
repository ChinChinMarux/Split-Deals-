package com.example.splitthedeals.ui.bill

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.splitthedeals.data.BillStore
import com.example.splitthedeals.data.remote.SyncManager
import com.example.splitthedeals.domain.BillInput
import com.example.splitthedeals.domain.BillStatus
import com.example.splitthedeals.domain.DiscountKind
import com.example.splitthedeals.domain.ItemInput
import com.example.splitthedeals.domain.Money
import com.example.splitthedeals.domain.PaymentStatus
import com.example.splitthedeals.domain.PersonInput
import com.example.splitthedeals.domain.SplitCalculator
import com.example.splitthedeals.domain.SplitMode
import com.example.splitthedeals.domain.TaxKind
import com.example.splitthedeals.ui.common.ErrorText
import com.example.splitthedeals.ui.common.HairlineCard
import com.example.splitthedeals.ui.common.MoneyText
import com.example.splitthedeals.ui.common.PrimaryButton
import com.example.splitthedeals.ui.common.ScreenHeader
import com.example.splitthedeals.ui.common.SectionHeader
import com.example.splitthedeals.ui.navigation.Screen
import java.util.UUID

private data class PersonDraft(val id: String, val name: String)

private data class ItemDraft(
    val id: String,
    val name: String,
    val priceText: String,
    val sharerIds: List<String>,
    val expanded: Boolean = true,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillScreen(navController: NavController, billId: String? = null) {
    val context = LocalContext.current
    val initialBill = remember(billId) {
        if (billId == null) null else BillStore(context).load(billId)
    }

    var title by remember(initialBill) { mutableStateOf(initialBill?.title ?: "") }
    var people by remember(initialBill) {
        mutableStateOf(
            initialBill?.people?.map { PersonDraft(it.id, it.name) }?.ifEmpty { null }
                ?: listOf(
                    PersonDraft(UUID.randomUUID().toString(), ""),
                    PersonDraft(UUID.randomUUID().toString(), ""),
                ),
        )
    }
    var items by remember(initialBill) {
        mutableStateOf(
            initialBill?.items?.map {
                ItemDraft(it.id, it.name, Money.formatInput(it.priceCents), it.sharerIds, expanded = it.name.isBlank())
            }?.ifEmpty { null }
                ?: listOf(ItemDraft(UUID.randomUUID().toString(), "", "", emptyList())),
        )
    }
    var mode by remember(initialBill) { mutableStateOf(initialBill?.mode ?: SplitMode.BY_ITEM) }
    var taxKind by remember(initialBill) { mutableStateOf(initialBill?.taxKind ?: TaxKind.PERCENT) }
    var taxPercent by remember(initialBill) {
        mutableStateOf(
            if (initialBill?.taxKind == TaxKind.PERCENT) Money.formatPercentInput(initialBill.taxPercentBps) else "10",
        )
    }
    var taxFixed by remember(initialBill) {
        mutableStateOf(if (initialBill?.taxKind == TaxKind.FIXED) Money.formatInput(initialBill?.taxCents ?: 0L) else "")
    }
    var otherCosts by remember(initialBill) {
        mutableStateOf(if ((initialBill?.otherCostsCents ?: 0L) > 0L) Money.formatInput(initialBill?.otherCostsCents ?: 0L) else "")
    }
    var discountKind by remember(initialBill) { mutableStateOf(initialBill?.discountKind ?: DiscountKind.NONE) }
    var discountPercent by remember(initialBill) {
        mutableStateOf(
            if (initialBill?.discountKind == DiscountKind.PERCENT) Money.formatPercentInput(initialBill.discountPercentBps) else "10",
        )
    }
    var discountFixed by remember(initialBill) {
        mutableStateOf(if (initialBill?.discountKind == DiscountKind.FIXED) Money.formatInput(initialBill.discountCents) else "")
    }
    var error by remember { mutableStateOf<String?>(null) }

    fun calculate() {
        val validPeople = people.filter { it.name.isNotBlank() }
        if (validPeople.size < 2) {
            error = "Minimal 2 orang dengan nama."
            return
        }
        val distinctNames = validPeople.map { it.name.trim().lowercase() }
        if (distinctNames.size != distinctNames.toSet().size) {
            error = "Nama orang tidak boleh duplikat."
            return
        }
        val drafts = items.filter { it.name.isNotBlank() }
        if (drafts.isEmpty()) {
            error = "Minimal 1 item."
            return
        }

        val parsedItems = mutableListOf<ItemInput>()
        for (draft in drafts) {
            val price = Money.parse(draft.priceText)
            if (price == null) {
                error = "Harga item '${draft.name}' tidak valid."
                return
            }
            if (mode == SplitMode.BY_ITEM && draft.sharerIds.isEmpty()) {
                error = "Item '${draft.name}' belum ada anggota."
                return
            }
            parsedItems.add(ItemInput(draft.id, draft.name.trim(), price, draft.sharerIds))
        }

        if (parsedItems.sumOf { it.priceCents } <= 0L) {
            error = "Total belanja harus lebih dari nol."
            return
        }

        val percentBps = if (taxKind == TaxKind.PERCENT) {
            val parsed = Money.parsePercent(taxPercent)
            if (parsed == null || parsed <= 0) {
                error = "Persen pajak tidak valid."
                return
            }
            parsed
        } else 0

        val fixedCents = if (taxKind == TaxKind.FIXED) {
            val parsed = Money.parse(taxFixed)
            if (parsed == null && taxFixed.isNotBlank()) {
                error = "Pajak tidak valid."
                return
            }
            parsed ?: 0L
        } else 0L

        val otherCostsCents = if (otherCosts.isBlank()) 0L else {
            val parsed = Money.parse(otherCosts)
            if (parsed == null) {
                error = "Biaya lain tidak valid."
                return
            }
            parsed
        }

        val discountPercentBps = if (discountKind == DiscountKind.PERCENT) {
            val parsed = Money.parsePercent(discountPercent)
            if (parsed == null || parsed !in 0..10_000) {
                error = "Persen diskon harus antara 0 dan 100."
                return
            }
            parsed
        } else 0
        val discountCents = if (discountKind == DiscountKind.FIXED) {
            val parsed = Money.parse(discountFixed)
            if (parsed == null && discountFixed.isNotBlank()) {
                error = "Diskon tidak valid."
                return
            }
            parsed ?: 0L
        } else 0L

        val bill = BillInput(
            id = initialBill?.id ?: UUID.randomUUID().toString(),
            title = title.ifBlank { "Makan bersama" },
            people = validPeople.map { PersonInput(it.id, it.name.trim()) },
            items = parsedItems,
            mode = mode,
            taxKind = taxKind,
            taxPercentBps = percentBps,
            taxCents = fixedCents,
            otherCostsCents = otherCostsCents,
            discountKind = discountKind,
            discountPercentBps = discountPercentBps,
            discountCents = discountCents,
            status = initialBill?.status ?: BillStatus.BERJALAN,
            payments = initialBill?.payments ?: validPeople.map { PaymentStatus(it.id, false) },
            createdAt = initialBill?.createdAt ?: 0L,
        )

        try {
            SplitCalculator.calculate(bill)
            val saved = BillStore(context).save(bill)
            SyncManager.push(saved)
            error = null
            navController.navigate(Screen.billDetail(saved.id)) {
                popUpTo(Screen.Home) { inclusive = false }
            }
        } catch (e: IllegalArgumentException) {
            error = e.message
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars).imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ScreenHeader("Split", "Buat tagihan baru.", modifier = Modifier.weight(1f))
                TextButton(onClick = { navController.popBackStack() }) { Text("Kembali") }
            }
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nama bill (opsional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        item { SectionHeader("Orang") }
        items(people.size, key = { people[it].id }) { index ->
            val person = people[index]
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = person.name,
                        onValueChange = { name ->
                            people = people.mapIndexed { position, draft ->
                                if (position == index) draft.copy(name = name) else draft
                            }
                        },
                        label = { Text("Orang ${index + 1}") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    if (people.size > 2) {
                        OutlinedButton(
                            onClick = {
                                people = people.filterIndexed { position, _ -> position != index }
                                items = items.map { it.copy(sharerIds = it.sharerIds - person.id) }
                            },
                            modifier = Modifier.padding(start = 8.dp).semantics { contentDescription = "Hapus orang" },
                        ) { Text("Hapus") }
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { people = people + PersonDraft(UUID.randomUUID().toString(), "") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("Tambah orang") }
        }

        item { SectionHeader("Cara split") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == SplitMode.BY_ITEM,
                    onClick = { mode = SplitMode.BY_ITEM },
                    label = { Text("Per item") },
                )
                FilterChip(
                    selected = mode == SplitMode.EVEN,
                    onClick = { mode = SplitMode.EVEN },
                    label = { Text("Rata") },
                )
            }
        }

        item { SectionHeader("Item") }
        items(items.size, key = { items[it].id }) { index ->
            val item = items[index]
            val isExpanded = item.expanded || item.name.isBlank()
            ItemCard(
                item = item,
                expanded = isExpanded,
                people = people,
                mode = mode,
                onToggleExpanded = {
                    if (item.name.isNotBlank()) {
                        items = items.mapIndexed { p, d -> if (p == index) d.copy(expanded = !d.expanded) else d }
                    }
                },
                onNameChange = { name ->
                    items = items.mapIndexed { p, d -> if (p == index) d.copy(name = name) else d }
                },
                onPriceChange = { price ->
                    items = items.mapIndexed { p, d ->
                        if (p == index) d.copy(priceText = price.filter { it.isDigit() || it == '.' }) else d
                    }
                },
                onSharerToggle = { personId ->
                    items = items.map { d ->
                        if (d.id != item.id) d
                        else d.copy(
                            sharerIds = if (personId in d.sharerIds) d.sharerIds - personId else d.sharerIds + personId,
                        )
                    }
                },
                onEveryoneToggle = {
                    val allSelected = people.isNotEmpty() && people.all { it.id in item.sharerIds }
                    items = items.map { d ->
                        if (d.id != item.id) d
                        else d.copy(sharerIds = if (allSelected) emptyList() else people.map { it.id })
                    }
                },
                onDelete = { items = items.filterIndexed { p, _ -> p != index } },
            )
        }
        item {
            OutlinedButton(
                onClick = { items = items + ItemDraft(UUID.randomUUID().toString(), "", "", emptyList()) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("Tambah item") }
        }


        item { SectionHeader("Diskon") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = discountKind == DiscountKind.NONE, onClick = { discountKind = DiscountKind.NONE }, label = { Text("Tanpa") })
                FilterChip(selected = discountKind == DiscountKind.PERCENT, onClick = { discountKind = DiscountKind.PERCENT }, label = { Text("Persen") })
                FilterChip(selected = discountKind == DiscountKind.FIXED, onClick = { discountKind = DiscountKind.FIXED }, label = { Text("Nominal") })
            }
        }
        if (discountKind == DiscountKind.PERCENT) {
            item {
                OutlinedTextField(
                    value = discountPercent,
                    onValueChange = { discountPercent = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Persen diskon (cth 10)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }
        if (discountKind == DiscountKind.FIXED) {
            item {
                OutlinedTextField(
                    value = discountFixed,
                    onValueChange = { discountFixed = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Diskon (Misal: 1000)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }


        item { SectionHeader("Biaya Lainnya") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = taxKind == TaxKind.NONE, onClick = { taxKind = TaxKind.NONE }, label = { Text("Tanpa pajak") })
                FilterChip(selected = taxKind == TaxKind.PERCENT, onClick = { taxKind = TaxKind.PERCENT }, label = { Text("Pajak %") })
                FilterChip(selected = taxKind == TaxKind.FIXED, onClick = { taxKind = TaxKind.FIXED }, label = { Text("Pajak nominal") })
            }
        }
        if (taxKind == TaxKind.PERCENT) {
            item {
                OutlinedTextField(
                    value = taxPercent,
                    onValueChange = { taxPercent = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Persen pajak (cth 10)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }
        if (taxKind == TaxKind.FIXED) {
            item {
                OutlinedTextField(
                    value = taxFixed,
                    onValueChange = { taxFixed = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Pajak (Misal: 1000)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }
        item {
            OutlinedTextField(
                value = otherCosts,
                onValueChange = { otherCosts = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Biaya lain: service/ongkir (Misal: 1000)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }

        item { ErrorText(error) }
        item { PrimaryButton(text = "Hitung & Simpan", onClick = { calculate() }) }
        item { Spacer(Modifier.padding(bottom = 16.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemCard(
    item: ItemDraft,
    expanded: Boolean,
    people: List<PersonDraft>,
    mode: SplitMode,
    onToggleExpanded: () -> Unit,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onSharerToggle: (String) -> Unit,
    onEveryoneToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val priceCents = remember(item.priceText) { Money.parse(item.priceText) ?: 0L }
    val everyoneSelected = people.isNotEmpty() && people.all { it.id in item.sharerIds }
    HairlineCard {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggleExpanded() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(item.name.ifBlank { "Item baru" }, style = MaterialTheme.typography.titleMedium)
                if (item.name.isNotBlank()) {
                    Text(
                        Money.format(priceCents),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (item.name.isNotBlank()) {
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Tutup" else "Buka",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(top = 12.dp)) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = onNameChange,
                    label = { Text("Nama item") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = item.priceText,
                    onValueChange = onPriceChange,
                    label = { Text("Harga (Misal: 1000)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (mode == SplitMode.BY_ITEM) {
                    Text(
                        "Dibagi ke:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        FilterChip(
                            selected = everyoneSelected,
                            onClick = onEveryoneToggle,
                            label = { Text("Semuanya") },
                        )
                        people.forEach { person ->
                            FilterChip(
                                selected = person.id in item.sharerIds,
                                onClick = { onSharerToggle(person.id) },
                                label = { Text(person.name.ifBlank { "Orang" }) },
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.padding(top = 12.dp).semantics { contentDescription = "Hapus item" },
                ) { Text("Hapus item") }
            }
        }
    }
}

