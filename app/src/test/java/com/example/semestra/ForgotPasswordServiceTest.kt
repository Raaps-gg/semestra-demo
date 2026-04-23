package com.example.semestra

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.semestra.data.AppDatabase
import com.example.semestra.data.User
import com.example.semestra.data.UserRepository
import com.example.semestra.logic.ForgotPasswordResult
import com.example.semestra.logic.ForgotPasswordService
import com.example.semestra.logic.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForgotPasswordServiceTest {

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
    fun reset_updatesHash_whenAnswerMatches() = runBlocking {
        val repo = UserRepository(db.userDao())
        repo.insertUser(
            User(
                userId = "u1",
                firstName = "F",
                lastName = "L",
                email = "e@e.com",
                phoneNumber = "1234567890",
                passwordHash = PasswordHasher.hash("OldPass1"),
                securityQuestion = "Pet?",
                securityAnswer = "Dog"
            )
        )
        val service = ForgotPasswordService(repo)
        val result = service.resetPassword("u1", "dog")
        assertTrue(result is ForgotPasswordResult.Success)
        val newHash = repo.getUserByUserId("u1")!!.passwordHash
        assertEquals(PasswordHasher.hash((result as ForgotPasswordResult.Success).temporaryPassword), newHash)
    }

    @Test
    fun reset_fails_whenAnswerWrong() = runBlocking {
        val repo = UserRepository(db.userDao())
        repo.insertUser(
            User(
                userId = "u2",
                firstName = "F",
                lastName = "L",
                email = "e2@e.com",
                phoneNumber = "1234567890",
                passwordHash = PasswordHasher.hash("OldPass1"),
                securityQuestion = "Pet?",
                securityAnswer = "Dog"
            )
        )
        val service = ForgotPasswordService(repo)
        val result = service.resetPassword("u2", "cat")
        assertTrue(result is ForgotPasswordResult.Error)
    }
}
