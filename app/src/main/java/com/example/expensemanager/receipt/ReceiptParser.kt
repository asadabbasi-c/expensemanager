package com.example.expensemanager.receipt

import java.text.SimpleDateFormat
import java.util.*

data class ParsedReceipt(
    val amount: Double?,
    val merchant: String?,
    val date: String?,
    val rawText: String
)

object ReceiptParser {

    /**
     * Tries "TOTAL / GRAND TOTAL / AMOUNT DUE" keywords first (most reliable),
     * then falls back to the largest PKR/Rs amount found on the receipt.
     */
    fun parse(rawText: String): ParsedReceipt = ParsedReceipt(
        amount   = extractAmount(rawText),
        merchant = extractMerchant(rawText),
        date     = extractDate(rawText),
        rawText  = rawText
    )

    private fun extractAmount(text: String): Double? {
        // Priority 1: labelled total line
        val totalPattern = Regex(
            """(?:grand\s+total|total\s+amount|total\s+due|amount\s+due|net\s+total|payable|total)[:\s*]+(?:PKR|Rs\.?)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        totalPattern.find(text)
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?.let { return it }

        // Priority 2: largest PKR / Rs amount on any line
        val pkrPattern = Regex("""(?:PKR|Rs\.?)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        val largest = pkrPattern.findAll(text)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .maxOrNull()
        if (largest != null) return largest

        // Priority 3: last decimal number on a line (bare amount like "1,250.00")
        val barePattern = Regex("""([0-9,]{1,10}\.[0-9]{2})\s*$""", RegexOption.MULTILINE)
        return barePattern.findAll(text)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .maxOrNull()
    }

    private fun extractMerchant(text: String): String? {
        // The first non-trivial line is usually the store name
        return text.lines()
            .map { it.trim() }
            .firstOrNull { line ->
                line.length in 3..60
                    && line.any { it.isLetter() }
                    && !line.all { it.isDigit() || it == '-' || it == '/' || it == ':' }
            }
            ?.take(40)
    }

    private fun extractDate(text: String): String? {
        // yyyy-MM-dd / yyyy/MM/dd
        Regex("""(\d{4}[-/]\d{2}[-/]\d{2})""").find(text)?.groupValues?.get(1)
            ?.let { normalizeDate(it, "yyyy-MM-dd") ?: normalizeDate(it, "yyyy/MM/dd") }
            ?.let { return it }

        // dd-MM-yyyy / dd/MM/yyyy
        Regex("""(\d{2}[-/]\d{2}[-/]\d{4})""").find(text)?.groupValues?.get(1)
            ?.let { normalizeDate(it, "dd-MM-yyyy") ?: normalizeDate(it, "dd/MM/yyyy") }
            ?.let { return it }

        // "15 Mar 2024" style
        Regex("""\b(\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{4})\b""",
            RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)
            ?.let { normalizeDate(it, "d MMM yyyy") ?: normalizeDate(it, "dd MMM yyyy") }
            ?.let { return it }

        return null
    }

    private fun normalizeDate(raw: String, inFmt: String): String? {
        return runCatching {
            val date = SimpleDateFormat(inFmt, Locale.ENGLISH).parse(raw.replace("/", "-").replace(" ", " "))!!
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        }.getOrNull()
    }
}
