package com.example.nssapp.di

import com.example.nssapp.feature.admin.data.repository.AdminRepositoryImpl
import com.example.nssapp.feature.admin.data.repository.AttendanceRepositoryImpl
import com.example.nssapp.feature.admin.domain.repository.AdminRepository
import com.example.nssapp.feature.admin.domain.repository.AttendanceRepository
import com.example.nssapp.feature.auth.data.repository.AuthRepositoryImpl
import com.example.nssapp.feature.auth.domain.repository.AuthRepository
import com.example.nssapp.feature.student.data.repository.StudentRepositoryImpl
import com.example.nssapp.feature.student.domain.repository.StudentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.app.Application
import androidx.room.Room
import com.example.nssapp.core.data.local.NssDatabase
import com.example.nssapp.core.data.local.dao.EventDao

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository {
        return AuthRepositoryImpl(auth, firestore)
    }

    @Provides
    @Singleton
    fun provideAdminRepository(
        firestore: FirebaseFirestore,
        eventDao: EventDao
    ): AdminRepository {
        return AdminRepositoryImpl(firestore, eventDao)
    }

    @Provides
    @Singleton
    fun provideStudentRepository(
        firestore: FirebaseFirestore,
        eventDao: EventDao
    ): StudentRepository {
        return StudentRepositoryImpl(firestore, eventDao)
    }

    @Provides
    @Singleton
    fun provideAttendanceRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): AttendanceRepository {
        return AttendanceRepositoryImpl(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideNssDatabase(app: Application): NssDatabase {
        return Room.databaseBuilder(
            app,
            NssDatabase::class.java,
            NssDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideEventDao(db: NssDatabase): EventDao {
        return db.eventDao
    }
}
