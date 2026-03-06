package com.example.nssapp.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.nssapp.core.data.local.dao.EventDao
import com.example.nssapp.core.data.local.entity.EventEntity

@Database(entities = [EventEntity::class], version = 1, exportSchema = false)
abstract class NssDatabase : RoomDatabase() {
    abstract val eventDao: EventDao
    
    companion object {
        const val DATABASE_NAME = "nss_db"
    }
}
