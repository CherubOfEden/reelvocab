package app.reelvocab.core.data.di

import android.content.Context
import androidx.room.Room
import app.reelvocab.core.common.AppDispatchers
import app.reelvocab.core.common.DefaultAppDispatchers
import app.reelvocab.core.data.cards.CardRepositoryImpl
import app.reelvocab.core.data.db.CardDao
import app.reelvocab.core.data.db.FilmDao
import app.reelvocab.core.data.db.MIGRATION_1_2
import app.reelvocab.core.data.db.MIGRATION_2_3
import app.reelvocab.core.data.db.MIGRATION_3_4
import app.reelvocab.core.data.db.ReelDatabase
import app.reelvocab.core.data.db.ReviewDao
import app.reelvocab.core.data.db.WordDao
import app.reelvocab.core.data.dictionary.DictionaryRepositoryImpl
import app.reelvocab.core.data.enrichment.EnrichmentRouter
import app.reelvocab.core.data.enrichment.FakeEnrichmentService
import app.reelvocab.core.data.enrichment.FakeTranscriptionService
import app.reelvocab.core.data.reviews.ReviewRepositoryImpl
import app.reelvocab.core.data.vault.MediaVaultRepositoryImpl
import app.reelvocab.core.domain.model.CardRepository
import app.reelvocab.core.domain.model.DictionaryRepository
import app.reelvocab.core.domain.model.EnrichmentService
import app.reelvocab.core.domain.model.MediaVaultRepository
import app.reelvocab.core.domain.model.ReviewRepository
import app.reelvocab.core.domain.model.TranscriptionService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDispatchers(): AppDispatchers = DefaultAppDispatchers()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ReelDatabase =
        Room.databaseBuilder(context, ReelDatabase::class.java, "reelvocab.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides
    fun provideCardDao(database: ReelDatabase): CardDao = database.cardDao()

    @Provides
    fun provideWordDao(database: ReelDatabase): WordDao = database.wordDao()

    @Provides
    fun provideFilmDao(database: ReelDatabase): FilmDao = database.filmDao()

    @Provides
    fun provideReviewDao(database: ReelDatabase): ReviewDao = database.reviewDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {

    @Binds
    @Singleton
    abstract fun bindDictionaryRepository(impl: DictionaryRepositoryImpl): DictionaryRepository

    @Binds
    @Singleton
    abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(impl: ReviewRepositoryImpl): ReviewRepository

    @Binds
    @Singleton
    abstract fun bindMediaVaultRepository(impl: MediaVaultRepositoryImpl): MediaVaultRepository

    @Binds
    @Singleton
    abstract fun bindEnrichmentService(impl: EnrichmentRouter): EnrichmentService

    @Binds
    @Singleton
    abstract fun bindTranscriptionService(impl: FakeTranscriptionService): TranscriptionService
}
