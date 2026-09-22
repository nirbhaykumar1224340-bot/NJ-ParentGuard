package com.nj.parentguard.location
import androidx.room.Database
import androidx.room.RoomDatabase
@Database(entities=[LocationPoint::class],version=1,exportSchema=false)
abstract class ParentGuardDatabase:RoomDatabase(){ abstract fun locationPointDao():LocationPointDao }
