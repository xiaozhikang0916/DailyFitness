package site.xiaozk.dailyfitness.repository.model

import java.time.LocalDate

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
@JvmInline
value class BodyDataWithDate(
    val personData: Map<LocalDate, List<BodyDataRecord>> = emptyMap(),
)
