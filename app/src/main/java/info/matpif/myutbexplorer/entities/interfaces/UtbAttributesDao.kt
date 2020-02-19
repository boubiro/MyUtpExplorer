package info.matpif.myutbexplorer.entities.interfaces

import androidx.room.*
import info.matpif.myutbexplorer.entities.UtbAttributes

@Dao
interface UtbAttributesDao {
    @Query("SELECT * FROM utb_attributes")
    fun getAll(): List<UtbAttributes>

    @Query("SELECT * FROM utb_attributes where is_favorite = 1")
    fun getFavorite(): List<UtbAttributes>

    @Query("SELECT * FROM utb_attributes WHERE uid IN (:utbAttributesIds)")
    fun loadAllByIds(utbAttributesIds: IntArray): List<UtbAttributes>

    @Query("SELECT * FROM utb_attributes WHERE code LIKE :code LIMIT 1")
    fun findByCode(code: String): UtbAttributes

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(utbsAttributes: UtbAttributes)

    @Delete
    fun delete(utbAttributes: UtbAttributes)
}
