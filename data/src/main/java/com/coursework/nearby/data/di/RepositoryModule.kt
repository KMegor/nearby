package com.coursework.nearby.data.di

import com.coursework.nearby.data.repository.AuthRepositoryImpl
import com.coursework.nearby.data.repository.ProfileRepositoryImpl
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
}
