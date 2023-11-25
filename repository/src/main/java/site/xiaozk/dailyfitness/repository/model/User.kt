package site.xiaozk.dailyfitness.repository.model

import kotlinx.serialization.Serializable

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
@Serializable
data class User(
    val uid: Int = 0,
    val name: String = "",
)
