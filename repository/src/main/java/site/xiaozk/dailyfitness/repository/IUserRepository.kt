package site.xiaozk.dailyfitness.repository

import site.xiaozk.dailyfitness.repository.model.User

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/25
 */
interface IUserRepository {
    suspend fun getCurrentUser(): User
    suspend fun createUser(user: User)
}