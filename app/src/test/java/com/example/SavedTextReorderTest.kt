package com.example

import com.example.data.model.SavedText
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedTextReorderTest {

    @Test
    fun testReorderLogic() {
        val original = listOf(
            SavedText(id = 1, title = "A", body = "Body A", orderIndex = 0),
            SavedText(id = 2, title = "B", body = "Body B", orderIndex = 1),
            SavedText(id = 3, title = "C", body = "Body C", orderIndex = 2)
        )

        // Swap B to position 0 (B, A, C)
        val reorderedList = mutableListOf(original[1], original[0], original[2])
        val updated = reorderedList.mapIndexed { index, item ->
            item.copy(orderIndex = index)
        }

        assertEquals(2L, updated[0].id)
        assertEquals(0, updated[0].orderIndex)

        assertEquals(1L, updated[1].id)
        assertEquals(1, updated[1].orderIndex)

        assertEquals(3L, updated[2].id)
        assertEquals(2, updated[2].orderIndex)
    }

    @Test
    fun testAlphabeticalSorting() {
        val list = listOf(
            SavedText(id = 1, title = "Zebra", body = "1"),
            SavedText(id = 2, title = "apple", body = "2"),
            SavedText(id = 3, title = "Banana", body = "3")
        )

        val asc = list.sortedBy { it.title.lowercase() }
        assertEquals("apple", asc[0].title)
        assertEquals("Banana", asc[1].title)
        assertEquals("Zebra", asc[2].title)

        val desc = list.sortedByDescending { it.title.lowercase() }
        assertEquals("Zebra", desc[0].title)
        assertEquals("Banana", desc[1].title)
        assertEquals("apple", desc[2].title)
    }
}
