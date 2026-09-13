package site.xiaozk.dailyfitness.repository

import kotlinx.io.files.Path

/**
 * @author: xiaozhikang
 * @create: 2023/11/25
 */
interface ISettingRepository {
    /**
     * Serializes all user data and writes it to [path].
     *
     * [path] is a kotlinx-io file path so this contract stays free of `java.io`
     * and can be implemented/tested without Android APIs.
     */
    suspend fun exportAllDataTo(path: Path)

    /** Reads a previously exported file from [path] and restores the data it contains. */
    suspend fun importAllDataFrom(path: Path)
}
