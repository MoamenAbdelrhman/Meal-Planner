package com.example.foodplanner.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firebaseId: String?,
    val email: String,
    val name: String
)