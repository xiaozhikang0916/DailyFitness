package site.xiaozk.dailyfitness.database.repo

import site.xiaozk.dailyfitness.database.dao.UserDao
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.User
import javax.inject.Inject

typealias DBUser = site.xiaozk.dailyfitness.database.model.DBUser

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/1
 */
class UserRepository @Inject constructor(
    private val userDao: UserDao,
) : IUserRepository {
    private var currentUser: User? = null
    override suspend fun getCurrentUser(): User {
        // todo multi user?
        currentUser = userDao.getAllUsers().firstOrNull()?.toRepoEntity()
        return currentUser ?: run {
            site.xiaozk.dailyfitness.database.model.DBUser(name = "app").toRepoEntity().also {
                currentUser = it
                createUser(it)
            }
        }
    }

    override suspend fun createUser(user: User) {
        userDao.createUser(
            DBUser(
                name = user.name
            )
        )
    }
}