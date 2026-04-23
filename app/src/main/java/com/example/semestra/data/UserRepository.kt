package com.example.semestra.data

class UserRepository(
    private val userDao: UserDao
) {
    suspend fun getUserByUserId(userId: String): User? = userDao.getUserByUserId(userId)

    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }

    suspend fun updatePasswordHash(userId: String, passwordHash: String) {
        userDao.updatePasswordHash(userId, passwordHash)
    }
}
