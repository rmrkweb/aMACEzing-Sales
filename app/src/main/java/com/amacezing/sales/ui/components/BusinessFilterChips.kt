package com.amacezing.sales.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Print
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.ui.theme.AccentCyan

/**
 * Business selector. State and callbacks are owned by the caller (the ViewModel's selectedBusiness /
 * setBusinessFilter); this component only renders them.
 */
@Composable
fun BusinessFilterChips(
    selectedBusiness: BusinessType?,
    onSelectBusiness: (BusinessType?) -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false
) {
    Column(modifier = modifier) {
        if (showLabel) {
            FilterSectionLabel("Business")
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactFilterChip(
                label = "All Businesses",
                selected = selectedBusiness == null,
                onClick = { onSelectBusiness(null) },
                selectedColor = AccentCyan,
                leadingIcon = Icons.Default.Business,
                modifier = Modifier.testTag("filter_chip_all_businesses")
            )
            CompactFilterChip(
                label = "Engraving",
                selected = selectedBusiness == BusinessType.ENGRAVING,
                onClick = { onSelectBusiness(BusinessType.ENGRAVING) },
                selectedColor = AccentCyan,
                leadingIcon = Icons.Default.Handyman,
                modifier = Modifier.testTag("filter_chip_engraving")
            )
            CompactFilterChip(
                label = "Sintra Board",
                selected = selectedBusiness == BusinessType.SINTRA_BOARD,
                onClick = { onSelectBusiness(BusinessType.SINTRA_BOARD) },
                selectedColor = AccentCyan,
                leadingIcon = Icons.Default.Print,
                modifier = Modifier.testTag("filter_chip_sintra_board")
            )
        }
    }
}
