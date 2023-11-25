package site.xiaozk.dailyfitness.repository

import java.io.File

/**
 * @author: xiaozhikang
 * @create: 2023/11/25
 */
interface ISettingRepository {
    suspend fun exportAllDataTo(file: File)
    suspend fun importAllDataFrom(file: File)
}