package com.raisoft.grabi.domain.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class CountryClassifierTest {

    @Test
    fun codeTokenInFileName() {
        assertEquals("US", CountryClassifier.classify(listOf("us-client.conf")))
        assertEquals("DE", CountryClassifier.classify(listOf("node-de-1")))
    }

    @Test
    fun multiWordCountryName() {
        assertEquals("US", CountryClassifier.classify(listOf("united-states", "wg")))
        assertEquals("GB", CountryClassifier.classify(listOf("United Kingdom - London")))
    }

    @Test
    fun fullNameToken() {
        assertEquals("DE", CountryClassifier.classify(listOf("germany-frankfurt")))
        assertEquals("JP", CountryClassifier.classify(listOf("Japan-Tokyo")))
    }

    @Test
    fun aliases() {
        assertEquals("GB", CountryClassifier.classify(listOf("uk", "london")))
        assertEquals("NL", CountryClassifier.classify(listOf("holland")))
        assertEquals("US", CountryClassifier.classify(listOf("usa")))
    }

    @Test
    fun pathSegmentsClassify() {
        assertEquals("SG", CountryClassifier.classify(listOf("wg0", "configs/sg/wg0.conf")))
    }

    @Test
    fun unknownFallsBackToOther() {
        assertEquals("OTHER", CountryClassifier.classify(listOf("wireguard-configs")))
        assertEquals("OTHER", CountryClassifier.classify(emptyList()))
        assertEquals("OTHER", CountryClassifier.classify(listOf("aaaaaaaaaaaaaa")))
    }

    @Test
    fun countryLookup() {
        assertEquals("United States", CountryClassifier.country("US").name)
        assertEquals(CountryClassifier.OTHER, CountryClassifier.country("XX"))
    }
}