package com.flutterforge.di

import com.flutterforge.data.repository.AuthRepositoryImpl
import com.flutterforge.data.repository.GitHubRepositoryImpl
import com.flutterforge.data.repository.ProjectRepositoryImpl
import com.flutterforge.data.repository.SettingsRepositoryImpl
import com.flutterforge.domain.repository.AuthRepository
import com.flutterforge.domain.repository.GitHubRepository
import com.flutterforge.domain.repository.ProjectRepository
import com.flutterforge.domain.repository.SettingsRepository
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
    abstract fun bindGitHubRepository(impl: GitHubRepositoryImpl): GitHubRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
