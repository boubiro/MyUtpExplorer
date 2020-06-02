package info.matpif.myutbexplorer.entities.interfaces

import androidx.room.*
import info.matpif.myutbexplorer.entities.DownloadUploadManager
import info.matpif.myutbexplorer.entities.UtbAttributes

@Dao
interface DownloadUploadManagerDao {
    @Query("SELECT * FROM download_upload_manager")
    fun getAll(): List<DownloadUploadManager>

    @Query("SELECT * FROM download_upload_manager ORDER BY uid DESC")
    fun getAllOrderByIdDesc(): List<DownloadUploadManager>

    @Query("SELECT * FROM download_upload_manager WHERE uid IN (:downloadUploadManagerIds)")
    fun loadAllByIds(downloadUploadManagerIds: IntArray): List<DownloadUploadManager>

    @Query("SELECT * FROM download_upload_manager WHERE id_request LIKE :idRequest LIMIT 1")
    fun findByIdRequest(idRequest: String): DownloadUploadManager

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(downloadUploadManager: DownloadUploadManager): Long

    @Delete
    fun delete(downloadUploadManager: DownloadUploadManager)

    @Query("DELETE FROM download_upload_manager WHERE progress NOT IN (2, 4, 16, 1)")
    fun deleteAllFinished()
}
