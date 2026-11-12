package com.coursework.nearby.data.di

import android.content.Context
import androidx.room.Room
import com.coursework.nearby.data.local.AppDatabase
import com.coursework.nearby.data.local.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "nearby.db").build()

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messageDao()
}
