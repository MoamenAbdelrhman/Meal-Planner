package com.example.foodplanner.core.model.local.source

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.foodplanner.core.model.FavouriteMealDao
import com.example.foodplanner.core.model.FavouriteMealEntity
import com.example.foodplanner.core.model.MealEntity
import com.example.foodplanner.core.model.local.MyTypeConverters
import com.example.foodplanner.core.model.local.User
import com.example.foodplanner.meal_plan.model.MealPlanDao
import com.example.foodplanner.meal_plan.model.MealPlanEntity

@Database(entities = [User::class, MealEntity::class, MealPlanEntity::class, FavouriteMealEntity::class], version = 8, exportSchema = false)
@TypeConverters(MyTypeConverters::class)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun favouriteMealDao(): FavouriteMealDao

    companion object {
        @Volatile
        private var INSTANCE: UserDatabase? = null

        fun getDatabaseInstance(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}