package com.example.expensemanager.sms

import java.text.SimpleDateFormat
import java.util.*

data class ParsedSms(
    val amount: Double,
    val merchant: String,
    val bankName: String,
    val rawMessage: String,
    val date: String
)

object SmsParser {

    private val AMOUNT_PATTERNS = listOf(
        Regex("""(?:PKR|Rs\.?|INR)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""(?:debited|spent|charged|payment of)\s+(?:PKR|Rs\.?)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""amount[:\s]+(?:PKR|Rs\.?)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:PKR|Rs\.?|INR)""", RegexOption.IGNORE_CASE)
    )

    private val MERCHANT_PATTERNS = listOf(
        Regex("""(?:at|to|from|with)\s+([A-Za-z0-9\s&',.-]{2,40})(?:\.|,|$|\s+on|\s+via|\s+ref)""", RegexOption.IGNORE_CASE),
        Regex("""merchant[:\s]+([A-Za-z0-9\s&',.-]{2,40})(?:\.|,|$)""", RegexOption.IGNORE_CASE)
    )

    private val BANK_KEYWORDS = mapOf(
        "HBL" to listOf("HBL", "Habib Bank"),
        "MCB" to listOf("MCB", "Muslim Commercial"),
        "UBL" to listOf("UBL", "United Bank"),
        "Meezan" to listOf("Meezan", "MEBL"),
        "Allied Bank" to listOf("Allied Bank", "ABL"),
        "Bank Alfalah" to listOf("Bank Alfalah", "Alfalah"),
        "Standard Chartered" to listOf("Standard Chartered", "SCB"),
        "Easypaisa" to listOf("Easypaisa", "Telenor Bank"),
        "JazzCash" to listOf("JazzCash"),
        "SBI" to listOf("SBI", "State Bank of India"),
        "HDFC" to listOf("HDFC"),
        "ICICI" to listOf("ICICI"),
        "Axis" to listOf("Axis Bank"),
        "Paytm" to listOf("Paytm"),
        "PhonePe" to listOf("PhonePe"),
        "GPay" to listOf("Google Pay", "GPay")
    )

    fun parse(smsBody: String, sender: String, timestamp: Long): ParsedSms? {
        val amount = extractAmount(smsBody) ?: return null
        val merchant = extractMerchant(smsBody)
        val bankName = extractBankName(smsBody, sender)
        val date = formatDate(timestamp)

        return ParsedSms(
            amount = amount,
            merchant = merchant,
            bankName = bankName,
            rawMessage = smsBody,
            date = date
        )
    }

    private fun extractAmount(body: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                return amountStr.toDoubleOrNull()
            }
        }
        return null
    }

    private fun extractMerchant(body: String): String {
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                val merchant = match.groupValues[1].trim()
                if (merchant.isNotBlank()) {
                    return merchant.take(40)
                }
            }
        }
        return "Unknown"
    }

    private fun extractBankName(body: String, sender: String): String {
        val combined = "$sender $body"
        for ((bankName, keywords) in BANK_KEYWORDS) {
            if (keywords.any { combined.contains(it, ignoreCase = true) }) {
                return bankName
            }
        }
        // Try to extract from sender address
        return sender.take(20).uppercase()
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun isBankSms(sender: String, body: String): Boolean {
        val bankIndicators = listOf(
            "debit", "credit", "transaction", "transfer", "payment",
            "PKR", "Rs.", "INR", "debited", "credited", "spent",
            "balance", "account", "ATM", "POS"
        )
        val combined = "$sender $body".lowercase()
        return bankIndicators.any { combined.contains(it.lowercase()) }
    }
}
