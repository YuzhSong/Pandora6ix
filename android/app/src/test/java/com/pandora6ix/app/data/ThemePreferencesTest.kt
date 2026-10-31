package com.pandora6ix.app.data

import org.junit.Assert.*
import org.junit.Test

class ThemePreferencesTest {
    @Test fun selectionSurvivesStoreRecreationAndAccountsStayIndependent() {
        val disk=mutableMapOf<String,String>()
        fun store()=ThemePreferences({ disk[it] },{ key,value -> disk[key]=value },setOf("warm","mint","sky"))
        store().select("first","mint")
        assertEquals("mint",store().theme("first"))
        assertEquals("warm",store().theme("second"))
        store().select("second","sky")
        assertEquals("mint",store().theme("first"))
        assertEquals("sky",store().theme("second"))
        assertEquals("warm",store().theme(null))
    }

    @Test fun unknownStoredThemeFallsBackAndInvalidWritesDoNotReplaceSelection() {
        val disk=mutableMapOf("theme_first" to "removed-theme")
        val prefs=ThemePreferences({disk[it]},{key,value->disk[key]=value},setOf("warm","mint"))
        assertEquals("warm",prefs.theme("first"))
        prefs.select("first","mint")
        assertTrue(runCatching { prefs.select("first","unknown") }.isFailure)
        assertEquals("mint",prefs.theme("first"))
    }
}
