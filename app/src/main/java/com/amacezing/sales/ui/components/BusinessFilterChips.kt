package com.amacezing.sales.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.amacezing.sales.model.BusinessType
import com.amacezing.sales.ui.theme.EngravingBrown
import com.amacezing.sales.ui.theme.SintraBoardBlue

@Composable
fun BusinessFilterChips(
    selectedBusiness: BusinessType?,
    onSelectBusiness: (BusinessType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedBusiness == null,
            onClick = { onSelectBusiness(null) },
            label = { Text("All Businesses") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = "All Businesses",
                    modifier = Modifier.padding(2.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.testTag("filter_chip_all_businesses")
        )

        FilterChip(
            selected = selectedBusiness == BusinessType.ENGRAVING,
            onClick = { onSelectBusiness(BusinessType.ENGRAVING) },
            label = { Text("Engraving") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Handyman,
                    contentDescription = "Engraving",
                    modifier = Modifier.padding(2.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = EngravingBrown,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.testTag("filter_chip_engraving")
        )

        FilterChip(
            selected = selectedBusiness == BusinessType.SINTRA_BOARD,
            onClick = { onSelectBusiness(BusinessType.SINTRA_BOARD) },
            label = { Text("Sintra Board") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = "Sintra Board",
                    modifier = Modifier.padding(2.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SintraBoardBlue,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.testTag("filter_chip_sintra_board")
        )
    }
}
