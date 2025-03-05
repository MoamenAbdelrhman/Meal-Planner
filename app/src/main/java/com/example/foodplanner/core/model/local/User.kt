package com.example.foodplanner.core.model.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = false)
    val id: String = "2",
    @ColumnInfo(name = "username")
    val username: String,
    val firebaseId: String?,
    @ColumnInfo(name = "email")
    val email: String?,
    @ColumnInfo(name = "password")
    val password: String?,
    @ColumnInfo(name = "cuisines")
    val cuisines: List<String> = emptyList(),
    @ColumnInfo(name = "favourites")
    val favourites: List<String> = emptyList(),
    @ColumnInfo(name = "isSubscribed")
    val isSubscribed: Boolean = false,
    @ColumnInfo(name = "isLoggedIn")
    val isLoggedIn: Boolean = false
)