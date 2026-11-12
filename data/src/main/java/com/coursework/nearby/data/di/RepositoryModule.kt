package com.coursework.nearby.data.di

import com.coursework.nearby.data.repository.AuthRepositoryImpl
import com.coursework.nearby.data.location.FusedLocationProvider
import com.coursework.nearby.data.repository.ChatRepositoryImpl
import com.coursework.nearby.data.repository.ConnectionRepositoryImpl
import com.coursework.nearby.data.repository.PresenceRepositoryImpl
import com.coursework.nearby.data.repository.ProfileRepositoryImpl
import com.coursework.nearby.data.repository.SettingsRepositoryImpl
import com.coursework.nearby.domain.location.LocationProvider
import com.coursework.nearby.domain.repository.SettingsRepository
import com.coursework.nearby.domain.repository.ChatRepository
import com.coursework.nearby.domain.repository.ConnectionRepository
import com.coursework.nearby.domain.repository.PresenceRepository
import com.coursework.nearby.domain.repository.AuthRepository
import com.coursework.nearby.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindPresenceRepository(impl: PresenceRepositoryImpl): PresenceRepository

    @Binds
    @Singleton
    abstract fun bindConnectionRepository(impl: ConnectionRepositoryImpl): ConnectionRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: FusedLocationProvider): LocationProvider
}
