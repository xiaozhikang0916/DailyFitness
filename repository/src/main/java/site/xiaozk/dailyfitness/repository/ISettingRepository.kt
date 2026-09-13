package site.xiaozk.dailyfitness.repository

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.files.Path

/**
 * @author: xiaozhikang
 * @create: 2023/11/25
 */
interface ISettingRepository {
    /**
     * Serializes all user data and writes it to the file at [path].
     *
     * [path] is a kotlinx-io file path so this contract stays free of `java.io`
     * and can be implemented/tested without Android APIs.
     */
    suspend fun exportAllDataTo(path: Path)

    /**
     * Serializes all user data and writes it to an already-open [sink].
     *
     * Used when the destination is not a plain filesystem path - notably a Storage
     * Access Framework document chosen by the user, which is only reachable through
     * `ContentResolver` streams. The caller owns the [sink] lifetime; this method
     * flushes but does not close it.
     */
    suspend fun exportAllDataTo(sink: Sink)

    /** Reads a previously exported file from [path] and restores the data it contains. */
    suspend fun importAllDataFrom(path: Path)

    /** Reads a previously exported stream from [source] and restores the data it contains. */
    suspend fun importAllDataFrom(source: Source)
}
