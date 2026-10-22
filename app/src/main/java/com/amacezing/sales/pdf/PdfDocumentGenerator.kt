package com.amacezing.sales.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.amacezing.sales.R
import com.amacezing.sales.model.BusinessProfile
import com.amacezing.sales.model.DiscountType
import com.amacezing.sales.model.PaymentStatus
import com.amacezing.sales.model.QuotationWithItems
import com.amacezing.sales.model.TransactionWithDetails
import com.amacezing.sales.util.Formatters
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min

/**
 * Generates professional A4 portrait PDF documents for Price Quotations and Sales Receipts.
 * Page Dimensions: 595 x 842 points (Standard A4 @ 72 dpi).
 */
object PdfDocumentGenerator {

    // Standard A4 dimensions in PostScript points (72 points per inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    // Corporate modern color palette
    private val COLOR_PRIMARY_NAVY = Color.rgb(15, 30, 54)        // #0F1E36
    private val COLOR_SECONDARY_NAVY = Color.rgb(30, 48, 80)     // #1E3050
    private val COLOR_ACCENT_CYAN = Color.rgb(0, 150, 200)        // #0096C8
    private val COLOR_ACCENT_LIGHT_CYAN = Color.rgb(224, 247, 250) // #E0F7FA
    private val COLOR_TEXT_DARK = Color.rgb(33, 37, 41)           // #212529
    private val COLOR_TEXT_MUTED = Color.rgb(108, 117, 125)       // #6C757D
    private val COLOR_BORDER = Color.rgb(226, 232, 240)           // #E2E8F0
    private val COLOR_ROW_ALT = Color.rgb(248, 250, 252)          // #F8FAFC
    private val COLOR_STATUS_PAID = Color.rgb(22, 101, 52)        // Forest green
    private val COLOR_STATUS_PAID_BG = Color.rgb(220, 252, 231)   // Soft green
    private val COLOR_STATUS_PENDING = Color.rgb(161, 98, 7)      // Warm amber
    private val COLOR_STATUS_PENDING_BG = Color.rgb(254, 249, 195)// Soft amber

    /**
     * Generates a professional A4 portrait Quotation PDF.
     */
    fun generateQuotationPdf(
        context: Context,
        quotationWithItems: QuotationWithItems,
        profile: BusinessProfile,
        businessName: String? = null
    ): File {
        val quotation = quotationWithItems.quotation
        val items = quotationWithItems.lineItems

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawDocumentBackground(canvas)

        // 1. Header (Branding + Document Title)
        var currentY = drawHeader(
            canvas = canvas,
            context = context,
            profile = profile,
            docTitle = "PRICE QUOTATION",
            docNumber = quotation.quotationNumber,
            dateLabel = "Date: ${Formatters.formatEpochMillisToDate(quotation.quotationDateEpochMillis)}",
            secondaryDateLabel = "Valid Until: ${Formatters.formatEpochMillisToDate(quotation.validUntilEpochMillis)}",
            badgeText = quotation.status.displayName.uppercase(),
            badgeBgColor = COLOR_ACCENT_LIGHT_CYAN,
            badgeTextColor = COLOR_PRIMARY_NAVY
        )

        // 2. Client / Customer & Division Block
        currentY = drawCustomerBlock(
            canvas = canvas,
            title = "QUOTATION PREPARED FOR",
            name = quotation.customerName,
            contact = quotation.customerPhone,
            email = quotation.customerEmail,
            address = quotation.customerAddress,
            division = "${businessName ?: com.amacezing.sales.model.Business.fallbackName(quotation.businessType)} Division",
            startY = currentY
        )

        // 3. Line Items Table
        val tableItems = items.map { item ->
            TableRowData(
                description = item.productName + if (item.notes.isNotBlank()) " (${item.notes})" else "",
                quantity = item.quantity.toString(),
                unitPrice = Formatters.formatCurrency(item.unitPrice),
                totalPrice = Formatters.formatCurrency(item.lineSubtotal)
            )
        }
        currentY = drawItemsTable(canvas, tableItems, currentY)

        // 4. Financial Totals Box
        val totals = mutableListOf<Pair<String, String>>()
        totals.add("Subtotal (Gross)" to Formatters.formatCurrency(quotation.grossTotal))
        if (quotation.discountAmount > 0.0) {
            val discDesc = if (quotation.discountType == DiscountType.PERCENTAGE) {
                "Discount (${quotation.discountValue}%)"
            } else {
                "Discount (Fixed)"
            }
            totals.add(discDesc to "-${Formatters.formatCurrency(quotation.discountAmount)}")
        }
        totals.add("Quotation Total" to Formatters.formatCurrency(quotation.netTotal))

        val termsText = buildString {
            if (profile.quotationTerms.isNotBlank()) {
                append(profile.quotationTerms)
            }
            if (quotation.remarks.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append("Special Remarks: ${quotation.remarks}")
            }
        }

        currentY = drawBottomSection(
            canvas = canvas,
            leftTitle = "TERMS & PAYMENT DETAILS",
            leftContent = termsText,
            paymentInstructions = profile.paymentInstructions,
            totals = totals,
            startY = currentY
        )

        // 5. Signatures and Footer
        drawSignaturesAndFooter(
            canvas = canvas,
            profile = profile,
            footerNote = profile.documentFooter.ifBlank { "Thank you for trusting aMACEzing Printing Services!" },
            showCustomerAcceptance = true
        )

        document.finishPage(page)

        // Save to cacheDir / documents
        val documentsDir = File(context.cacheDir, "documents").apply { mkdirs() }
        val fileName = "Quotation_${quotation.quotationNumber.replace("/", "_")}.pdf"
        val outputFile = File(documentsDir, fileName)

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return outputFile
    }

    /**
     * Generates a professional A4 portrait Sales Receipt PDF.
     */
    fun generateReceiptPdf(
        context: Context,
        transactionWithDetails: TransactionWithDetails,
        profile: BusinessProfile,
        customer: com.amacezing.sales.model.Customer? = null,
        businessName: String? = null
    ): File {
        val tx = transactionWithDetails.transaction
        val items = transactionWithDetails.lineItems
        val payments = transactionWithDetails.payments

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawDocumentBackground(canvas)

        val isPaid = tx.paymentStatus == PaymentStatus.PAID
        val statusBg = if (isPaid) COLOR_STATUS_PAID_BG else COLOR_STATUS_PENDING_BG
        val statusColor = if (isPaid) COLOR_STATUS_PAID else COLOR_STATUS_PENDING

        // 1. Header (Branding + Document Title)
        var currentY = drawHeader(
            canvas = canvas,
            context = context,
            profile = profile,
            docTitle = "SALES RECEIPT",
            docNumber = tx.orderNumber,
            dateLabel = "Date: ${Formatters.formatEpochMillisToDate(tx.orderDateEpochMillis)}",
            secondaryDateLabel = "Status: ${tx.paymentStatus.displayName}",
            badgeText = tx.paymentStatus.displayName.uppercase(),
            badgeBgColor = statusBg,
            badgeTextColor = statusColor
        )

        // 2. Customer & Division Block
        currentY = drawCustomerBlock(
            canvas = canvas,
            title = "RECEIPT ISSUED TO",
            name = tx.customerName,
            contact = customer?.phone.orEmpty(),
            email = customer?.email.orEmpty(),
            address = customer?.notes.orEmpty(),
            division = "${businessName ?: com.amacezing.sales.model.Business.fallbackName(tx.businessType)} Division",
            startY = currentY
        )

        // 3. Line Items Table
        val tableItems = items.map { item ->
            TableRowData(
                description = item.productName,
                quantity = item.quantity.toString(),
                unitPrice = Formatters.formatCurrency(item.unitSellingPrice),
                totalPrice = Formatters.formatCurrency(item.lineSubtotal)
            )
        }
        currentY = drawItemsTable(canvas, tableItems, currentY)

        // 4. Financial Totals
        val totals = mutableListOf<Pair<String, String>>()
        totals.add("Subtotal (Gross)" to Formatters.formatCurrency(tx.grossSales))
        if (tx.discountAmount > 0.0) {
            val discLabel = if (tx.discountType == DiscountType.PERCENTAGE) {
                "Discount (${tx.discountPercentage}%)"
            } else {
                "Discount (Fixed)"
            }
            totals.add(discLabel to "-${Formatters.formatCurrency(tx.discountAmount)}")
        }
        totals.add("Total Amount" to Formatters.formatCurrency(tx.netSales))
        totals.add("Amount Paid" to Formatters.formatCurrency(tx.amountPaid))
        totals.add("Balance Due" to Formatters.formatCurrency(tx.outstandingBalance))

        val paymentSummary = buildString {
            if (payments.isNotEmpty()) {
                append("Payment History:\n")
                payments.forEach { p ->
                    append("• ${Formatters.formatEpochMillisToDate(p.paymentDateEpochMillis)} - ${Formatters.formatCurrency(p.amount)} (${p.paymentMethod})")
                    if (p.notes.isNotBlank()) append(" [${p.notes}]")
                    append("\n")
                }
            } else if (tx.amountPaid > 0.0) {
                append("Paid: ${Formatters.formatCurrency(tx.amountPaid)}")
                if (tx.paymentNotes.isNotBlank()) append(" (${tx.paymentNotes})")
                append("\n")
            }
            if (tx.remarks.isNotBlank()) {
                append("\nRemarks: ${tx.remarks}")
            }
        }

        currentY = drawBottomSection(
            canvas = canvas,
            leftTitle = "PAYMENT ACKNOWLEDGMENT",
            leftContent = paymentSummary,
            paymentInstructions = profile.paymentInstructions,
            totals = totals,
            startY = currentY
        )

        // 5. Signatures and Footer
        drawSignaturesAndFooter(
            canvas = canvas,
            profile = profile,
            footerNote = profile.receiptFooter.ifBlank { "Thank you for your prompt payment! This serves as an official sales record." },
            showCustomerAcceptance = false
        )

        document.finishPage(page)

        val documentsDir = File(context.cacheDir, "documents").apply { mkdirs() }
        val fileName = "Receipt_${tx.orderNumber.replace("/", "_")}.pdf"
        val outputFile = File(documentsDir, fileName)

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return outputFile
    }

    // --- Private Drawing Helpers ---

    private fun drawDocumentBackground(canvas: Canvas) {
        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

        // Top decorative brand accent line (Navy + Cyan dual line)
        val navyPaint = Paint().apply { color = COLOR_PRIMARY_NAVY }
        val cyanPaint = Paint().apply { color = COLOR_ACCENT_CYAN }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 5f, navyPaint)
        canvas.drawRect(0f, 5f, PAGE_WIDTH.toFloat(), 8f, cyanPaint)
    }

    private fun drawHeader(
        canvas: Canvas,
        context: Context,
        profile: BusinessProfile,
        docTitle: String,
        docNumber: String,
        dateLabel: String,
        secondaryDateLabel: String,
        badgeText: String,
        badgeBgColor: Int,
        badgeTextColor: Int
    ): Float {
        val marginX = 36f
        var startY = 32f

        // Draw Business Logo if enabled
        var textStartX = marginX
        if (profile.showLogoOnDocuments) {
            val logoBitmap = loadLogoBitmap(context, profile.logoUri)
            if (logoBitmap != null) {
                val logoSize = 48f
                val srcRect = Rect(0, 0, logoBitmap.width, logoBitmap.height)
                val dstRect = RectF(marginX, startY, marginX + logoSize, startY + logoSize)
                canvas.drawBitmap(logoBitmap, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
                textStartX = marginX + logoSize + 12f
            }
        }

        // Business Name & Details (Left side)
        val bizNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY_NAVY
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(profile.businessName.ifBlank { "aMACEzing Printing Services" }, textStartX, startY + 14f, bizNamePaint)

        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        if (profile.tagline.isNotBlank()) {
            canvas.drawText(profile.tagline, textStartX, startY + 26f, taglinePaint)
        }

        // Contact info lines
        val contactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 8f
        }
        var contactY = startY + 38f
        if (profile.showContactOnDocuments) {
            val contactLine1 = listOfNotNull(
                profile.contactNumber.takeIf { it.isNotBlank() }?.let { "Tel: $it" },
                profile.contactEmail.takeIf { it.isNotBlank() }?.let { "Email: $it" }
            ).joinToString("  |  ")
            if (contactLine1.isNotBlank()) {
                canvas.drawText(contactLine1, textStartX, contactY, contactPaint)
                contactY += 11f
            }

            val addressLine = profile.address.takeIf { it.isNotBlank() }
            if (addressLine != null) {
                canvas.drawText(addressLine, textStartX, contactY, contactPaint)
                contactY += 11f
            }

            if (profile.tin.isNotBlank()) {
                canvas.drawText("TIN: ${profile.tin}", textStartX, contactY, contactPaint)
                contactY += 11f
            }
        }

        // Right side: Document Title and Number
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY_NAVY
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val rightX = PAGE_WIDTH - marginX
        canvas.drawText(docTitle, rightX, startY + 16f, titlePaint)

        val docNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ACCENT_CYAN
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(docNumber, rightX, startY + 32f, docNumPaint)

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(dateLabel, rightX, startY + 45f, metaPaint)
        canvas.drawText(secondaryDateLabel, rightX, startY + 56f, metaPaint)

        // Status Badge (Top right)
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeBgColor }
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeTextColor
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val badgeW = 80f
        val badgeH = 14f
        val badgeRect = RectF(rightX - badgeW, startY + 62f, rightX, startY + 62f + badgeH)
        canvas.drawRoundRect(badgeRect, 3f, 3f, badgePaint)
        canvas.drawText(badgeText, badgeRect.centerX(), badgeRect.centerY() + 2.5f, badgeTextPaint)

        val headerBottomY = maxOf(contactY + 10f, startY + 86f)

        // Horizontal line separator
        val sepPaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 1f
        }
        canvas.drawLine(marginX, headerBottomY, rightX, headerBottomY, sepPaint)

        return headerBottomY + 14f
    }

    private fun drawCustomerBlock(
        canvas: Canvas,
        title: String,
        name: String,
        contact: String,
        email: String,
        address: String,
        division: String,
        startY: Float
    ): Float {
        val marginX = 36f
        val contentW = PAGE_WIDTH - (marginX * 2)
        val blockH = 54f

        // Light background card for customer info
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ROW_ALT
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val rect = RectF(marginX, startY, marginX + contentW, startY + blockH)
        canvas.drawRoundRect(rect, 4f, 4f, cardPaint)
        canvas.drawRoundRect(rect, 4f, 4f, borderPaint)

        // Title
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ACCENT_CYAN
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, marginX + 12f, startY + 15f, labelPaint)

        // Customer Name
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(name.ifBlank { "Valued Customer / Client" }, marginX + 12f, startY + 30f, namePaint)

        // Contact info subtext
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 8f
        }
        val detailLine = listOfNotNull(
            contact.takeIf { it.isNotBlank() }?.let { "Phone: $it" },
            email.takeIf { it.isNotBlank() }?.let { "Email: $it" },
            address.takeIf { it.isNotBlank() }
        ).joinToString("  •  ")

        if (detailLine.isNotBlank()) {
            canvas.drawText(detailLine, marginX + 12f, startY + 43f, detailPaint)
        }

        // Division Badge on the right of the customer card
        val divBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_SECONDARY_NAVY }
        val divTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(division, marginX + contentW - 12f, startY + 22f, divTextPaint)

        return startY + blockH + 14f
    }

    private data class TableRowData(
        val description: String,
        val quantity: String,
        val unitPrice: String,
        val totalPrice: String
    )

    private fun drawItemsTable(
        canvas: Canvas,
        items: List<TableRowData>,
        startY: Float
    ): Float {
        val marginX = 36f
        val contentW = PAGE_WIDTH - (marginX * 2)

        // Column widths: Desc = 265, Qty = 50, Unit Price = 100, Total = 108 (Total = 523)
        val colDescW = 265f
        val colQtyW = 50f
        val colUnitW = 100f
        val colTotalW = contentW - colDescW - colQtyW - colUnitW // 108f

        val headerH = 22f

        // Table Header
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_PRIMARY_NAVY }
        val headerRect = RectF(marginX, startY, marginX + contentW, startY + headerH)
        canvas.drawRoundRect(headerRect, 3f, 3f, headerBgPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerCenterPaint = Paint(headerTextPaint).apply { textAlign = Paint.Align.CENTER }
        val headerRightPaint = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }

        val headerY = startY + 14.5f
        canvas.drawText("ITEM & DESCRIPTION", marginX + 10f, headerY, headerTextPaint)
        canvas.drawText("QTY", marginX + colDescW + (colQtyW / 2), headerY, headerCenterPaint)
        canvas.drawText("UNIT PRICE", marginX + colDescW + colQtyW + colUnitW - 8f, headerY, headerRightPaint)
        canvas.drawText("TOTAL AMOUNT", marginX + contentW - 10f, headerY, headerRightPaint)

        var currentY = startY + headerH

        val rowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 9f
        }
        val rowCenterPaint = Paint(rowTextPaint).apply { textAlign = Paint.Align.CENTER }
        val rowRightPaint = Paint(rowTextPaint).apply { textAlign = Paint.Align.RIGHT }

        val linePaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 0.8f
        }

        // Draw items
        items.forEachIndexed { index, item ->
            val rowH = 22f
            if (index % 2 == 1) {
                val rowBgPaint = Paint().apply { color = COLOR_ROW_ALT }
                canvas.drawRect(marginX, currentY, marginX + contentW, currentY + rowH, rowBgPaint)
            }

            val textY = currentY + 14.5f
            // Truncate long descriptions if needed to fit column
            val displayDesc = truncateText(item.description, rowTextPaint, colDescW - 16f)
            canvas.drawText(displayDesc, marginX + 10f, textY, rowTextPaint)
            canvas.drawText(item.quantity, marginX + colDescW + (colQtyW / 2), textY, rowCenterPaint)
            canvas.drawText(item.unitPrice, marginX + colDescW + colQtyW + colUnitW - 8f, textY, rowRightPaint)
            canvas.drawText(item.totalPrice, marginX + contentW - 10f, textY, rowRightPaint)

            canvas.drawLine(marginX, currentY + rowH, marginX + contentW, currentY + rowH, linePaint)
            currentY += rowH
        }

        return currentY + 12f
    }

    private fun drawBottomSection(
        canvas: Canvas,
        leftTitle: String,
        leftContent: String,
        paymentInstructions: String,
        totals: List<Pair<String, String>>,
        startY: Float
    ): Float {
        val marginX = 36f
        val contentW = PAGE_WIDTH - (marginX * 2)

        val leftBoxW = 290f
        val rightBoxW = contentW - leftBoxW - 16f // ~217f
        val rightBoxX = marginX + leftBoxW + 16f

        // Draw Totals Box (Right Side)
        var totalsY = startY
        val totalsBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val totalsCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ROW_ALT }

        val rowHeight = 20f
        val totalsBoxHeight = (totals.size * rowHeight) + 12f
        val totalsRect = RectF(rightBoxX, totalsY, rightBoxX + rightBoxW, totalsY + totalsBoxHeight)
        canvas.drawRoundRect(totalsRect, 4f, 4f, totalsCardPaint)
        canvas.drawRoundRect(totalsRect, 4f, 4f, totalsBorderPaint)

        var lineY = totalsY + 16f
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 8.5f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 9f
            textAlign = Paint.Align.RIGHT
        }
        val boldLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY_NAVY
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val boldValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ACCENT_CYAN
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        totals.forEachIndexed { idx, (label, value) ->
            val isFinalTotal = idx == totals.size - 1 || label.contains("Total", ignoreCase = true) || label.contains("Balance", ignoreCase = true)
            if (isFinalTotal) {
                canvas.drawText(label, rightBoxX + 10f, lineY, boldLabelPaint)
                canvas.drawText(value, rightBoxX + rightBoxW - 10f, lineY, boldValuePaint)
            } else {
                canvas.drawText(label, rightBoxX + 10f, lineY, labelPaint)
                canvas.drawText(value, rightBoxX + rightBoxW - 10f, lineY, valuePaint)
            }
            lineY += rowHeight
        }

        // Draw Left Box (Terms & Payment Instructions)
        val leftTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY_NAVY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(leftTitle, marginX, startY + 12f, leftTitlePaint)

        val leftTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        var textY = startY + 24f
        if (paymentInstructions.isNotBlank()) {
            val payHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_ACCENT_CYAN
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Payment Options:", marginX, textY, payHeaderPaint)
            textY += 10f
            paymentInstructions.split("\n", "/").forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotBlank() && textY < startY + 120f) {
                    canvas.drawText("• $trimmed", marginX + 4f, textY, leftTextPaint)
                    textY += 10f
                }
            }
            textY += 4f
        }

        if (leftContent.isNotBlank()) {
            leftContent.split("\n").forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotBlank() && textY < startY + 130f) {
                    canvas.drawText(truncateText(trimmed, leftTextPaint, leftBoxW), marginX, textY, leftTextPaint)
                    textY += 9.5f
                }
            }
        }

        return maxOf(totalsY + totalsBoxHeight, textY) + 16f
    }

    private fun drawSignaturesAndFooter(
        canvas: Canvas,
        profile: BusinessProfile,
        footerNote: String,
        showCustomerAcceptance: Boolean
    ) {
        val marginX = 36f
        val contentW = PAGE_WIDTH - (marginX * 2)
        val sigLineY = 740f

        val linePaint = Paint().apply {
            color = COLOR_BORDER
            strokeWidth = 1f
        }
        val sigLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }
        val sigNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PRIMARY_NAVY
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        if (showCustomerAcceptance) {
            // Two signatures: Authorized Signatory (left) and Conforme/Accepted (right)
            val halfW = 160f
            // Left (Authorized)
            canvas.drawLine(marginX, sigLineY, marginX + halfW, sigLineY, linePaint)
            val authName = profile.ownerName.ifBlank { profile.businessName }
            canvas.drawText(authName, marginX + (halfW / 2), sigLineY + 12f, sigNamePaint)
            canvas.drawText("Authorized Signatory", marginX + (halfW / 2), sigLineY + 22f, sigLabelPaint)

            // Right (Client Conforme)
            val rightSigStart = PAGE_WIDTH - marginX - halfW
            canvas.drawLine(rightSigStart, sigLineY, rightSigStart + halfW, sigLineY, linePaint)
            canvas.drawText("Client Conforme / Signature", rightSigStart + (halfW / 2), sigLineY + 12f, sigNamePaint)
            canvas.drawText("Date of Acceptance", rightSigStart + (halfW / 2), sigLineY + 22f, sigLabelPaint)
        } else {
            // One signature: Authorized Signatory on the right
            val sigW = 180f
            val sigStartX = PAGE_WIDTH - marginX - sigW
            canvas.drawLine(sigStartX, sigLineY, sigStartX + sigW, sigLineY, linePaint)
            val authName = profile.ownerName.ifBlank { profile.businessName }
            canvas.drawText(authName, sigStartX + (sigW / 2), sigLineY + 12f, sigNamePaint)
            canvas.drawText("Authorized Signatory / Acknowledged By", sigStartX + (sigW / 2), sigLineY + 22f, sigLabelPaint)
        }

        // Bottom Document Footer
        val bottomLineY = 780f
        canvas.drawLine(marginX, bottomLineY, PAGE_WIDTH - marginX, bottomLineY, linePaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 7.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(footerNote, PAGE_WIDTH / 2f, bottomLineY + 14f, footerPaint)

        val subFooterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = 6.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Generated by aMACEzing Sales Monitoring System", PAGE_WIDTH / 2f, bottomLineY + 24f, subFooterPaint)
    }

    private fun loadLogoBitmap(context: Context, logoUriStr: String): Bitmap? {
        // Try the user-supplied custom logo first. It is usually a plain file path
        // (copied into app storage by the Profile screen), but may also be a content:// or file:// URI.
        if (logoUriStr.isNotBlank()) {
            try {
                val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                val bmp: Bitmap? = if (logoUriStr.startsWith("content://")) {
                    context.contentResolver.openInputStream(Uri.parse(logoUriStr))?.use { stream: InputStream ->
                        BitmapFactory.decodeStream(stream, null, options)
                    }
                } else {
                    val path = if (logoUriStr.startsWith("file://")) Uri.parse(logoUriStr).path else logoUriStr
                    if (path != null && java.io.File(path).exists()) {
                        BitmapFactory.decodeFile(path, options)
                    } else null
                }
                if (bmp != null) return bmp
            } catch (_: Exception) {
                // Fall back to bundled app logo
            }
        }

        // Fall back to bundled app logo drawable
        return try {
            BitmapFactory.decodeResource(context.resources, R.drawable.amacezing_logo)
        } catch (_: Exception) {
            null
        }
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return if (end > 0) text.substring(0, end) + "…" else ""
    }
}
