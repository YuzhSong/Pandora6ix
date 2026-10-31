package com.pandora6ix.app.data

import org.junit.Assert.*
import org.junit.Test

class AvatarCacheTest {
    @Test fun absentAvatarExpiresAndCanBeReplacedByAnUpload() {
        var time=0L
        val cache=AvatarCache<String> { time }
        cache.put("employee",null)
        assertNotNull(cache.get("employee"))
        time=10_000
        assertNull(cache.get("employee"))
        cache.put("employee","photo")
        assertEquals("photo",cache.get("employee")?.value)
    }

    @Test fun uploadsReplaceOldImagesAndCachedImagesExpireWithoutLeakingAcrossRepositories() {
        var time=0L
        val cache=AvatarCache<String> { time }
        cache.put("one","old"); cache.put("two","other")
        cache.put("one","new")
        assertEquals("new",cache.get("one")?.value)
        assertEquals("other",cache.get("two")?.value)
        assertNull(AvatarCache<String> { time }.get("one"))
        time=30_000
        assertNull(cache.get("one"))
    }
}
