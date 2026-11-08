package com.example.caloscan.domain

/**
 * Finds the product's barcode number (GTIN) in the text of a QR code or DataMatrix code.
 *
 * Square codes on packages carry the number in one of these forms:
 *   - GS1 Digital Link, a web address with "/01/" and the number:
 *       https://id.gs1.org/01/03017620422003
 *   - GS1 data: "01" + the 14-digit number + more data. The «Честный ЗНАК» codes
 *     on Russian products look like this: 0104601234567893215Abc...
 *   - the same with brackets: (01)04601234567893(21)...
 *   - just the number
 *
 * Other codes, such as a link to the brand's website, contain no product number.
 */
object ProductCode {
  // An optional scanner prefix like "]d2" or "]Q3", an optional separator (ASCII 29), then "01" + 14 digits
  private val gs1Data = Regex("^(?:\\][A-Za-z][0-9])?\\u001D?01([0-9]{14})")
  private val bracketed = Regex("\\(01\\)\\s*([0-9]{14})")
  private val digitalLink = Regex("^https?://[^?#]*?/(?:01|gtin)/([0-9]{8,14})(?![0-9])", RegexOption.IGNORE_CASE)

  /** The product number in [text], or null if there is none. */
  fun fromText(text: String): String? {
    val value = text.trim()
    val candidate = when {
      value.isNotEmpty() && value.all(::isAsciiDigit) -> value
      else -> listOf(digitalLink, bracketed, gs1Data).firstNotNullOfOrNull { it.find(value)?.groupValues?.get(1) }
    } ?: return null
    // The last digit is a check digit, so random numbers in a link are not mistaken for a product
    return candidate.takeIf(::hasValidCheckDigit)?.let(::shorten)
  }

  /**
   * GS1 check digit (EAN-8, UPC-A, EAN-13, GTIN-14): from the right, the digits before the
   * check digit are multiplied by 3, 1, 3, 1…; the check digit brings the sum up to a multiple of 10.
   */
  fun hasValidCheckDigit(code: String): Boolean {
    if (code.length !in VALID_LENGTHS || !code.all(::isAsciiDigit)) return false
    val digits = code.map { it - '0' }
    val sum = digits.dropLast(1).reversed().withIndex().sumOf { (i, d) -> if (i % 2 == 0) d * 3 else d }
    return (10 - sum % 10) % 10 == digits.last()
  }

  /** "03017620422003" → "3017620422003": the 14-digit form of a 13-digit EAN has a leading 0. */
  private fun shorten(code: String): String =
    if (code.length == 14 && code.startsWith('0')) code.substring(1) else code

  private fun isAsciiDigit(c: Char) = c in '0'..'9'

  private val VALID_LENGTHS = setOf(8, 12, 13, 14)
}
