package com.example.expensemanager.data.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.expensemanager.data.model.Category
import com.example.expensemanager.data.model.Expense
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DataTransferService {

    // ── Public types ──────────────────────────────────────────────────────────

    data class ImportRow(
        val date: String,
        val time: String,
        val amount: Double,
        val category: String,
        val merchant: String?,
        val description: String?,
        val location: String?,
        val source: String?,
        val bankName: String?
    )

    data class ImportResult(
        val rows: List<ImportRow>,
        val errors: List<String>,
        val totalRowsRead: Int
    )

    // Required and optional column names (lowercase)
    val FORMAT_REQUIRED = listOf("date", "amount", "category")
    val FORMAT_OPTIONAL  = listOf("time", "merchant", "description", "location", "source", "bank_name")
    val FORMAT_EXAMPLE_ROW = "2024-01-15,14:30,500.00,Food,KFC,SMS: KFC,Clifton,sms,HBL"

    // ── XLSX Export ───────────────────────────────────────────────────────────

    fun exportToXlsx(
        expenses: List<Expense>,
        categories: List<Category>,
        outputStream: OutputStream
    ) {
        val catMap = categories.associateBy { it.id }
        val headers = listOf(
            "date", "time", "amount", "category",
            "merchant", "description", "location", "source", "bank_name"
        )
        val amountColIdx = 2

        // Build all data rows as strings
        val allRows: List<List<String>> = buildList {
            add(headers)
            expenses.forEach { e ->
                add(listOf(
                    e.date,
                    e.time,
                    e.amount.toString(),
                    catMap[e.categoryId]?.name ?: "Other",
                    e.merchant ?: "",
                    e.description,
                    e.location,
                    e.source,
                    e.bankName ?: ""
                ))
            }
        }

        // Build shared strings table
        val stringIndex = mutableMapOf<String, Int>()
        val strings = mutableListOf<String>()
        fun strIdx(s: String): Int = stringIndex.getOrPut(s) { strings.size.also { strings.add(s) } }

        // Pre-fill all string cells
        allRows.forEachIndexed { rowIdx, row ->
            row.forEachIndexed { colIdx, value ->
                val isNumeric = rowIdx > 0 && colIdx == amountColIdx
                if (!isNumeric) strIdx(value)
            }
        }

        val zos = ZipOutputStream(outputStream)

        fun entry(name: String, content: String) {
            zos.putNextEntry(ZipEntry(name))
            zos.write(content.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        entry("[Content_Types].xml", contentTypesXml())
        entry("_rels/.rels", relsXml())
        entry("xl/workbook.xml", workbookXml())
        entry("xl/_rels/workbook.xml.rels", workbookRelsXml())
        entry("xl/styles.xml", stylesXml())
        entry("xl/sharedStrings.xml", sharedStringsXml(strings))
        entry("xl/worksheets/sheet1.xml", sheetXml(allRows, amountColIdx, stringIndex))

        zos.finish()
        zos.close()
    }

    // ── PDF Export ────────────────────────────────────────────────────────────

    fun exportToPdf(
        expenses: List<Expense>,
        categories: List<Category>,
        outputStream: OutputStream
    ) {
        val catMap = categories.associateBy { it.id }
        val fmt = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 2; minimumFractionDigits = 2
        }

        // Portrait A4
        val pW = 595; val pH = 842
        val margin = 30f
        val usable = pW - margin * 2  // 535

        // Column definitions: (header, width, getValue)
        data class Col(val header: String, val width: Float, val getValue: (Expense) -> String)
        val cols = listOf(
            Col("Date",        75f)  { it.date },
            Col("Amount",      85f)  { "PKR ${fmt.format(it.amount)}" },
            Col("Category",    90f)  { catMap[it.categoryId]?.name ?: "Other" },
            Col("Merchant",   185f)  { (it.merchant?.takeIf { m -> m.isNotBlank() } ?: it.description).take(28) },
            Col("Source",      60f)  { it.source }
        )  // Total: 495, pad Merchant: 535-75-85-90-60=225 → let merchant = 225

        val adjustedCols = listOf(
            Col("Date",        75f)  { it.date },
            Col("Amount",      85f)  { "PKR ${fmt.format(it.amount)}" },
            Col("Category",    90f)  { catMap[it.categoryId]?.name ?: "Other" },
            Col("Merchant",   225f)  { (it.merchant?.takeIf { m -> m.isNotBlank() } ?: it.description).take(32) },
            Col("Source",      60f)  { it.source }
        )

        val rowH = 22f
        val headerH = 26f
        val titleH = 60f
        val rowsPerPage = ((pH - margin * 2 - titleH - headerH) / rowH).toInt()

        val document = PdfDocument()
        var pageNum = 0
        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var currentY = 0f

        val titlePaint   = Paint().apply { textSize = 16f; typeface = Typeface.DEFAULT_BOLD; color = Color.parseColor("#065F46") }
        val subtitlePaint = Paint().apply { textSize = 9f; color = Color.GRAY }
        val hdrBg        = Paint().apply { color = Color.parseColor("#065F46") }
        val hdrText      = Paint().apply { textSize = 9f; typeface = Typeface.DEFAULT_BOLD; color = Color.WHITE }
        val bodyPaint    = Paint().apply { textSize = 8f; color = Color.BLACK }
        val altBg        = Paint().apply { color = Color.parseColor("#F0FDF4") }
        val borderPaint  = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f; style = Paint.Style.STROKE }
        val pageNumPaint = Paint().apply { textSize = 8f; color = Color.GRAY }

        fun drawHeader(c: Canvas, y: Float) {
            c.drawRect(margin, y, pW - margin, y + headerH, hdrBg)
            var x = margin
            adjustedCols.forEach { col ->
                c.drawText(col.header, x + 4f, y + headerH - 8f, hdrText)
                x += col.width
            }
        }

        fun newPage(first: Boolean = false) {
            if (currentPage != null) document.finishPage(currentPage)
            pageNum++
            currentPage = document.startPage(
                PdfDocument.PageInfo.Builder(pW, pH, pageNum).create()
            )
            canvas = currentPage!!.canvas
            currentY = margin

            if (first) {
                canvas!!.drawText("Expense Report", margin, currentY + 18f, titlePaint)
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                canvas!!.drawText(
                    "$dateStr  ·  ${expenses.size} expenses  ·  Generated by ExpenseManager",
                    margin, currentY + 32f, subtitlePaint
                )
                currentY += titleH
            } else {
                currentY += 10f
            }
            drawHeader(canvas!!, currentY)
            currentY += headerH
        }

        newPage(first = true)

        expenses.forEachIndexed { idx, expense ->
            if (currentY + rowH > pH - margin - 15f) newPage()

            if (idx % 2 == 1) {
                canvas!!.drawRect(margin, currentY, pW - margin, currentY + rowH, altBg)
            }
            canvas!!.drawLine(margin, currentY + rowH, pW - margin, currentY + rowH, borderPaint)

            var x = margin
            adjustedCols.forEach { col ->
                canvas!!.drawText(col.getValue(expense), x + 4f, currentY + rowH - 7f, bodyPaint)
                x += col.width
            }
            currentY += rowH
        }

        // Page number footer
        if (currentPage != null) {
            canvas!!.drawText("Page $pageNum", pW - margin - 30f, pH - 15f, pageNumPaint)
            document.finishPage(currentPage)
        }

        document.writeTo(outputStream)
        document.close()
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    fun exportToCsv(
        expenses: List<Expense>,
        categories: List<Category>,
        outputStream: OutputStream
    ) {
        val catMap = categories.associateBy { it.id }
        val sb = StringBuilder()
        sb.appendLine("date,time,amount,category,merchant,description,location,source,bank_name")
        expenses.forEach { e ->
            sb.appendLine(listOf(
                e.date, e.time, e.amount.toString(),
                catMap[e.categoryId]?.name ?: "Other",
                e.merchant ?: "", e.description, e.location, e.source, e.bankName ?: ""
            ).joinToString(",") { csvEscape(it) })
        }
        outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
    }

    // ── CSV Import ────────────────────────────────────────────────────────────

    fun importFromCsv(inputStream: InputStream): ImportResult {
        val lines = inputStream.bufferedReader(Charsets.UTF_8)
            .readLines()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
        if (lines.isEmpty()) return ImportResult(emptyList(), listOf("File is empty"), 0)
        val headers = parseCsvLine(lines.first()).map { it.trim().lowercase() }
        val dataLines = lines.drop(1)
        return parseHeadersAndRows(headers, dataLines.map { parseCsvLine(it) }, dataLines.size)
    }

    // ── XLSX Import ───────────────────────────────────────────────────────────

    fun importFromXlsx(inputStream: InputStream): ImportResult {
        val bytes = inputStream.readBytes()
        val sharedStrings = mutableListOf<String>()
        var sheetBytes: ByteArray? = null

        val zip = ZipInputStream(bytes.inputStream())
        var entry = zip.nextEntry
        while (entry != null) {
            when (entry.name) {
                "xl/sharedStrings.xml" -> parseSharedStrings(zip.readBytes().inputStream(), sharedStrings)
                "xl/worksheets/sheet1.xml" -> sheetBytes = zip.readBytes()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
        zip.close()

        if (sheetBytes == null) return ImportResult(emptyList(), listOf("No worksheet found in XLSX file"), 0)
        val rows = parseSheetXml(sheetBytes.inputStream(), sharedStrings)
        if (rows.isEmpty()) return ImportResult(emptyList(), listOf("Spreadsheet is empty"), 0)

        val headers = rows.first().map { it.trim().lowercase() }
        val dataRows = rows.drop(1)
        return parseHeadersAndRows(headers, dataRows, dataRows.size)
    }

    // ── Shared parsing ────────────────────────────────────────────────────────

    private fun parseHeadersAndRows(
        headers: List<String>,
        dataRows: List<List<String>>,
        totalRows: Int
    ): ImportResult {
        val errors = mutableListOf<String>()

        fun idx(name: String) = headers.indexOf(name)
        val dateIdx    = idx("date")
        val timeIdx    = idx("time")
        val amountIdx  = idx("amount")
        val catIdx     = idx("category")
        val merchantIdx = idx("merchant")
        val descIdx    = idx("description")
        val locationIdx = idx("location")
        val sourceIdx  = idx("source")
        val bankIdx    = idx("bank_name")

        if (dateIdx == -1)   errors.add("Missing required column: 'date'")
        if (amountIdx == -1) errors.add("Missing required column: 'amount'")
        if (catIdx == -1)    errors.add("Missing required column: 'category'")
        if (errors.isNotEmpty()) return ImportResult(emptyList(), errors, 0)

        val imported = mutableListOf<ImportRow>()

        dataRows.forEachIndexed { i, row ->
            val rowNum = i + 2  // +1 for header, +1 for 1-indexing
            fun get(colIdx: Int) = if (colIdx >= 0 && colIdx < row.size) row[colIdx].trim() else ""

            val date   = get(dateIdx)
            val amount = get(amountIdx)
            val cat    = get(catIdx)

            if (date.isBlank() && amount.isBlank()) return@forEachIndexed  // skip empty rows

            when {
                date.isBlank() -> { errors.add("Row $rowNum: missing date — skipped"); return@forEachIndexed }
                !date.matches(Regex("""\d{4}-\d{2}-\d{2}""")) -> {
                    errors.add("Row $rowNum: invalid date '$date' (expected YYYY-MM-DD) — skipped")
                    return@forEachIndexed
                }
            }
            val parsedAmount = amount.toDoubleOrNull()
            if (parsedAmount == null || parsedAmount < 0) {
                errors.add("Row $rowNum: invalid amount '$amount' — skipped")
                return@forEachIndexed
            }
            if (cat.isBlank()) {
                errors.add("Row $rowNum: missing category — skipped")
                return@forEachIndexed
            }

            imported.add(ImportRow(
                date        = date,
                time        = get(timeIdx).ifBlank { "00:00" },
                amount      = parsedAmount,
                category    = cat,
                merchant    = get(merchantIdx).ifBlank { null },
                description = get(descIdx).ifBlank { null },
                location    = get(locationIdx).ifBlank { null },
                source      = get(sourceIdx).ifBlank { "manual" },
                bankName    = get(bankIdx).ifBlank { null }
            ))
        }

        return ImportResult(imported, errors, totalRows)
    }

    // ── XLSX XML helpers ──────────────────────────────────────────────────────

    private fun colLetter(idx: Int): String {
        var result = ""; var n = idx + 1
        while (n > 0) { n--; result = ('A' + n % 26).toString() + result; n /= 26 }
        return result
    }

    private fun contentTypesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun relsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets><sheet name="Expenses" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private fun workbookRelsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun stylesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
  </fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>"""

    private fun sharedStringsXml(strings: List<String>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${strings.size}" uniqueCount="${strings.size}">""")
        strings.forEach { sb.append("<si><t>${escapeXml(it)}</t></si>") }
        sb.append("</sst>")
        return sb.toString()
    }

    private fun sheetXml(
        rows: List<List<String>>,
        amountColIdx: Int,
        stringIndex: Map<String, Int>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEachIndexed { rowIdx, row ->
            val r = rowIdx + 1
            val isHeader = rowIdx == 0
            sb.append("""<row r="$r">""")
            row.forEachIndexed { colIdx, value ->
                val ref = "${colLetter(colIdx)}$r"
                if (!isHeader && colIdx == amountColIdx) {
                    sb.append("""<c r="$ref"><v>${value.ifBlank { "0" }}</v></c>""")
                } else {
                    val sIdx = stringIndex[value] ?: 0
                    val style = if (isHeader) """ s="1"""" else ""
                    sb.append("""<c r="$ref" t="s"$style><v>$sIdx</v></c>""")
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    // ── XLSX import helpers ───────────────────────────────────────────────────

    private fun parseSharedStrings(inputStream: InputStream, result: MutableList<String>) {
        val xpp = XmlPullParserFactory.newInstance().newPullParser()
        xpp.setInput(inputStream, "UTF-8")
        var inSi = false; var inT = false; val buf = StringBuilder()
        var e = xpp.eventType
        while (e != XmlPullParser.END_DOCUMENT) {
            when (e) {
                XmlPullParser.START_TAG -> when (xpp.name) {
                    "si" -> { inSi = true; buf.clear() }
                    "t"  -> inT = true
                }
                XmlPullParser.TEXT -> if (inSi && inT) buf.append(xpp.text)
                XmlPullParser.END_TAG -> when (xpp.name) {
                    "t"  -> inT = false
                    "si" -> { result.add(buf.toString()); inSi = false }
                }
            }
            e = xpp.next()
        }
    }

    private fun parseSheetXml(inputStream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val xpp = XmlPullParserFactory.newInstance().newPullParser()
        xpp.setInput(inputStream, "UTF-8")
        val rows = mutableListOf<List<String>>()
        val rowCells = mutableMapOf<Int, String>()
        var cellType = ""; var inV = false; var cellValue = ""
        var cellColIdx = 0; var inRow = false

        var ev = xpp.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> when (xpp.name) {
                    "row" -> { rowCells.clear(); inRow = true }
                    "c"   -> {
                        cellType = xpp.getAttributeValue(null, "t") ?: ""
                        cellColIdx = colRefToIdx(xpp.getAttributeValue(null, "r") ?: "A1")
                    }
                    "v"   -> { inV = true; cellValue = "" }
                }
                XmlPullParser.TEXT -> if (inV) cellValue += xpp.text
                XmlPullParser.END_TAG -> when (xpp.name) {
                    "v" -> {
                        rowCells[cellColIdx] = when (cellType) {
                            "s"  -> sharedStrings.getOrElse(cellValue.toIntOrNull() ?: -1) { "" }
                            "b"  -> if (cellValue == "1") "true" else "false"
                            else -> cellValue
                        }
                        inV = false
                    }
                    "row" -> if (inRow) {
                        val maxCol = (rowCells.keys.maxOrNull() ?: -1) + 1
                        rows.add((0 until maxCol).map { rowCells[it] ?: "" })
                        inRow = false
                    }
                }
            }
            ev = xpp.next()
        }
        return rows
    }

    private fun colRefToIdx(ref: String): Int {
        val letters = ref.filter { it.isLetter() }.uppercase()
        var result = 0
        letters.forEach { c -> result = result * 26 + (c - 'A' + 1) }
        return result - 1
    }

    // ── CSV helpers ───────────────────────────────────────────────────────────

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val buf = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes  -> {
                    if (i + 1 < line.length && line[i + 1] == '"') { buf.append('"'); i++ }
                    else inQuotes = false
                }
                c == ',' && !inQuotes -> { fields.add(buf.toString()); buf.clear() }
                else -> buf.append(c)
            }
            i++
        }
        fields.add(buf.toString())
        return fields
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            '"' + value.replace("\"", "\"\"") + '"'
        } else value
    }

    // ── XML helpers ───────────────────────────────────────────────────────────

    private fun escapeXml(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
