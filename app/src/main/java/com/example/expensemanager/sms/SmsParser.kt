package com.example.expensemanager.sms

import java.text.SimpleDateFormat
import java.util.*

data class ParsedSms(
    val amount: Double,
    val merchant: String,
    val bankName: String,
    val rawMessage: String,
    val date: String,
    val suggestedCategory: String = "Other",
    val confidence: Float = 0f   // 0.0 – 1.0
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
        "HBL"               to listOf("HBL", "Habib Bank"),
        "MCB"               to listOf("MCB", "Muslim Commercial"),
        "UBL"               to listOf("UBL", "United Bank"),
        "Meezan"            to listOf("Meezan", "MEBL"),
        "Allied Bank"       to listOf("Allied Bank", "ABL"),
        "Bank Alfalah"      to listOf("Bank Alfalah", "Alfalah"),
        "Standard Chartered" to listOf("Standard Chartered", "SCB"),
        "Easypaisa"         to listOf("Easypaisa", "Telenor Bank"),
        "JazzCash"          to listOf("JazzCash"),
        "SBI"               to listOf("SBI", "State Bank of India"),
        "HDFC"              to listOf("HDFC"),
        "ICICI"             to listOf("ICICI"),
        "Axis"              to listOf("Axis Bank"),
        "Paytm"             to listOf("Paytm"),
        "PhonePe"           to listOf("PhonePe"),
        "GPay"              to listOf("Google Pay", "GPay")
    )

    // Keyword → category for intelligent classification
    private val CATEGORY_KEYWORDS = mapOf(
        "Food" to listOf(
            "restaurant", "cafe", "coffee", "mcdonalds", "kfc", "burger", "pizza",
            "grill", "kitchen", "bakery", "biryani", "karahi", "dine", "dining",
            "eat", "food", "bbq", "subway", "dominos", "hardees", "wendy", "nandos",
            "butter", "tikka", "roast", "chinese", "thai", "sushi"
        ),
        "Transport" to listOf(
            "uber", "careem", "taxi", "fuel", "petrol", "pump", "transport",
            "bus", "parking", "toll", "lyft", "indriver", "swvl",
            "pso", "shell", "caltex", "total", "attock", "byco", "hascol"
        ),
        "Shopping" to listOf(
            "store", "mart", "shop", "mall", "outlet", "amazon", "daraz",
            "retail", "fashion", "clothes", "brand", "emporium", "hyperstar",
            "chase up", "gul ahmed", "khaadi", "sana safinaz", "junaid jamshed"
        ),
        "Groceries" to listOf(
            "grocery", "supermarket", "metro", "imtiaz", "naheed",
            "carrefour", "vegetables", "fruits", "ration", "agha", "al fatah"
        ),
        "Health" to listOf(
            "pharmacy", "medical", "hospital", "clinic", "doctor",
            "health", "medicine", "chemist", "aga khan", "shifa", "liaquat",
            "evercare", "lab", "diagnostic", "dawakhana", "drugstore"
        ),
        "Bills" to listOf(
            "electricity", "water", "gas", "internet", "phone",
            "bill", "utility", "mobile", "telco", "telecom",
            "ptcl", "jazz", "zong", "ufone", "telenor", "sngpl", "ssgc", "wapda", "iesco", "k-electric"
        ),
        "Entertainment" to listOf(
            "cinema", "movie", "netflix", "spotify", "game",
            "entertainment", "concert", "nueplex", "cinepax", "voot",
            "youtube", "prime", "disney", "hulu"
        )
    )

    fun parse(smsBody: String, sender: String, timestamp: Long): ParsedSms? {
        val amount = extractAmount(smsBody) ?: return null
        val rawMerchant = extractMerchant(smsBody)
        val merchant = normalizeMerchant(rawMerchant)
        val bankName = extractBankName(smsBody, sender)
        val date = formatDate(timestamp)
        val (category, confidence) = classifyCategory(merchant, smsBody)

        return ParsedSms(
            amount = amount,
            merchant = merchant,
            bankName = bankName,
            rawMessage = smsBody,
            date = date,
            suggestedCategory = category,
            confidence = confidence
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
                if (merchant.isNotBlank()) return merchant.take(40)
            }
        }
        return "Unknown"
    }

    fun normalizeMerchant(raw: String): String {
        if (raw == "Unknown") return raw
        var n = raw.trim()
        // Remove trailing branch codes like "KFC-011", "STORE #4521", "OUTLET 001"
        n = n.replace(Regex("""\s*[-/#]\s*\d+\w*\s*$"""), "").trim()
        n = n.replace(Regex("""\s+\d{3,}\s*$"""), "").trim()
        // Remove corporate suffixes
        n = n.replace(Regex("""\s+(PVT|LTD|LLC|INC|CORP|CO|SM|SB)\.?\s*$""", RegexOption.IGNORE_CASE), "").trim()
        // Title case
        return n.split(Regex("""[\s]+""")).joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }.take(40)
    }

    private fun classifyCategory(merchant: String, body: String): Pair<String, Float> {
        val searchText = "$merchant $body".lowercase()
        var bestCategory = "Other"
        var bestMatchCount = 0

        for ((category, keywords) in CATEGORY_KEYWORDS) {
            val matchCount = keywords.count { keyword -> searchText.contains(keyword.lowercase()) }
            if (matchCount > bestMatchCount) {
                bestMatchCount = matchCount
                bestCategory = category
            }
        }

        val confidence = when (bestMatchCount) {
            0    -> 0f
            1    -> 0.6f
            2    -> 0.85f
            else -> 0.95f
        }
        return Pair(bestCategory, confidence)
    }

    private fun extractBankName(body: String, sender: String): String {
        val combined = "$sender $body"
        for ((bankName, keywords) in BANK_KEYWORDS) {
            if (keywords.any { combined.contains(it, ignoreCase = true) }) return bankName
        }
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
