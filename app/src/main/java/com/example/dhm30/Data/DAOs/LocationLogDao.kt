package com.example.dhm30.Data.DAOs

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dhm30.Data.Entities.LocationLog


@Dao
interface LocationLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(log: LocationLog)

    @Query("SELECT * FROM location_logs LIMIT :limit OFFSET :offset")
    fun getLogsPaginated(limit: Int, offset: Int): List<LocationLog>

    @Delete
    fun delete(log: LocationLog)
}