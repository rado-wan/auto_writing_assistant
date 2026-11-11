package com.example.data.repository

import com.example.data.local.SavedTextDao
import com.example.data.model.SavedText
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class SavedTextRepository(private val dao: SavedTextDao) {

    val allTextsFlow: Flow<List<SavedText>> = dao.getAllFlow()

    suspend fun getAll(): List<SavedText> = dao.getAll()

    suspend fun insert(savedText: SavedText): Long = dao.insert(savedText)

    suspend fun insertAll(list: List<SavedText>) = dao.insertAll(list)

    suspend fun update(savedText: SavedText) = dao.update(savedText)

    suspend fun updateAll(list: List<SavedText>) = dao.updateAll(list)

    suspend fun delete(savedText: SavedText) = dao.delete(savedText)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun reorder(items: List<SavedText>) {
        val updated = items.mapIndexed { index, item ->
            item.copy(orderIndex = index)
        }
        dao.updateAll(updated)
    }

    fun exportToJson(items: List<SavedText>): String {
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("title", item.title)
                put("body", item.body)
                put("isFormattingEnabled", item.isFormattingEnabled)
                put("orderIndex", item.orderIndex)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    suspend fun importFromJson(jsonString: String, replaceExisting: Boolean = false): Result<Int> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val currentCount = if (replaceExisting) 0 else dao.getCount()
            val list = mutableListOf<SavedText>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("title", "Untitled")
                val body = obj.optString("body", "")
                val isFormattingEnabled = obj.optBoolean("isFormattingEnabled", false)
                val order = obj.optInt("orderIndex", currentCount + i)
                list.add(
                    SavedText(
                        title = title,
                        body = body,
                        isFormattingEnabled = isFormattingEnabled,
                        orderIndex = order
                    )
                )
            }

            if (replaceExisting) {
                dao.deleteAll()
            }
            dao.insertAll(list)
            Result.success(list.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
