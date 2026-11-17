package com.raisoft.grabi.domain.vpn

data class Country(val code: String, val name: String)

object CountryClassifier {

    val OTHER = Country("OTHER", "Other")

    val COMMON = listOf(
        Country("US", "United States"),
        Country("GB", "United Kingdom"),
        Country("DE", "Germany"),
        Country("NL", "Netherlands"),
        Country("FR", "France"),
        Country("JP", "Japan"),
        Country("SG", "Singapore"),
        Country("CA", "Canada"),
        Country("AU", "Australia"),
        Country("IN", "India"),
        Country("SE", "Sweden"),
        Country("CH", "Switzerland"),
        Country("IT", "Italy"),
        Country("ES", "Spain"),
        Country("BR", "Brazil"),
        Country("KR", "South Korea"),
    )

    private val EXTRA = listOf(
        Country("PL", "Poland"),
        Country("RO", "Romania"),
        Country("UA", "Ukraine"),
        Country("TR", "Turkey"),
        Country("HK", "Hong Kong"),
        Country("TW", "Taiwan"),
        Country("ID", "Indonesia"),
        Country("MY", "Malaysia"),
        Country("TH", "Thailand"),
        Country("VN", "Vietnam"),
        Country("PH", "Philippines"),
        Country("AE", "United Arab Emirates"),
        Country("ZA", "South Africa"),
        Country("MX", "Mexico"),
        Country("AR", "Argentina"),
        Country("NZ", "New Zealand"),
        Country("IE", "Ireland"),
        Country("NO", "Norway"),
        Country("FI", "Finland"),
        Country("DK", "Denmark"),
        Country("AT", "Austria"),
        Country("BE", "Belgium"),
        Country("PT", "Portugal"),
        Country("CZ", "Czechia"),
        Country("GR", "Greece"),
        Country("HU", "Hungary"),
        Country("BG", "Bulgaria"),
        Country("RU", "Russia"),
        Country("IL", "Israel"),
        Country("SA", "Saudi Arabia"),
        Country("EG", "Egypt"),
        Country("NG", "Nigeria"),
        Country("KE", "Kenya"),
        Country("PK", "Pakistan"),
        Country("BD", "Bangladesh"),
        Country("LK", "Sri Lanka"),
    )

    private val ALIASES = mapOf(
        "usa" to "US",
        "america" to "US",
        "uk" to "GB",
        "britain" to "GB",
        "england" to "GB",
        "holland" to "NL",
        "deutschland" to "DE",
        "korea" to "KR",
        "emirates" to "AE",
        "dubai" to "AE",
    )

    private val all = COMMON + EXTRA
    private val byCode = all.associateBy { it.code }

    fun country(code: String): Country = byCode[code] ?: OTHER

    fun classify(hints: List<String>): String {
        val tokens = hints
            .flatMap { it.lowercase().split(Regex("[^a-z]+")) }
            .filter { it.isNotEmpty() }
            .toSet()
        for (c in all) {
            val words = c.name.lowercase().split(" ")
            if (words.size > 1 && words.all { it in tokens }) return c.code
        }
        for (c in all) {
            if (c.code.lowercase() in tokens) return c.code
            val words = c.name.lowercase().split(" ")
            if (words.size == 1 && words[0] in tokens) return c.code
        }
        for ((alias, code) in ALIASES) {
            if (alias in tokens) return code
        }
        return OTHER.code
    }
}