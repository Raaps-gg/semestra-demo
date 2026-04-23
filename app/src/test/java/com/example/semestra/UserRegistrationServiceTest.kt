package com.example.semestra

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.UserRepository
import com.example.semestra.logic.RegistrationInput
import com.example.semestra.logic.RegistrationResult
import com.example.semestra.logic.UserRegistrationService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserRegistrationServiceTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun register_rejectsPasswordWithoutLetter() = runBlocking {
        val service = UserRegistrationService(UserRepository(db.userDao()))
        val result = service.register(
            RegistrationInput(
                userId = "validuser1",
                firstName = "A",
                lastName = "B",
                email = "a@b.com",
                phoneNumber = "1234567890",
                password = "12345678",
                securityQuestion = "Q?",
                securityAnswer = "A"
            )
        )
        assertTrue(result is RegistrationResult.ValidationFailed)
    }

    @Test
    fun register_success() = runBlocking {
        val service = UserRegistrationService(UserRepository(db.userDao()))
        val result = service.register(
            RegistrationInput(
                userId = "validuser2",
                firstName = "A",
                lastName = "B",
                email = "user2@b.com",
                phoneNumber = "1234567890",
                password = "Password1",
                securityQuestion = "Q?",
                securityAnswer = "A"
            )
        )
        assertTrue(result is RegistrationResult.Success)
    }
}
