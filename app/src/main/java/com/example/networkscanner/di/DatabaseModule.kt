package com.example.networkscanner.di

import android.content.Context
import com.example.networkscanner.data.local.AppDatabase
import com.example.networkscanner.data.local.EncryptedRoomConfig
import com.example.networkscanner.data.local.NetworkScannerDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return EncryptedRoomConfig.provideEncryptedDatabase(context)
    }

    @Provides
    fun provideDao(database: AppDatabase): NetworkScannerDao {
        return database.networkScannerDao()
    }
}
