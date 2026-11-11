package com.coursework.nearby.domain.model

import com.coursework.nearby.domain.error.ServerError
import org.junit.Assert.assertEquals
import org.junit.Test

class RelationTest {
    @Test
    fun `relation is parsed from the server code`() {
        assertEquals(Relation.INCOMING, Relation.fromCode("incoming"))
        assertEquals(Relation.BLOCKED_BY_ME, Relation.fromCode("blocked_by_me"))
        assertEquals(Relation.NONE, Relation.fromCode("something_new"))
    }

    @Test
    fun `server error is parsed from the exception text`() {
        assertEquals(ServerError.COOLDOWN, ServerError.fromCode("cooldown"))
        assertEquals(ServerError.BLOCK_NOT_AVAILABLE, ServerError.fromCode(" block_not_available "))
        assertEquals(ServerError.UNKNOWN, ServerError.fromCode(null))
    }

    @Test
    fun `radius falls back to default for unsupported values`() {
        assertEquals(SearchRadius.KM_5, SearchRadius.fromKm(5))
        assertEquals(SearchRadius.KM_1, SearchRadius.fromKm(2))
    }
}
