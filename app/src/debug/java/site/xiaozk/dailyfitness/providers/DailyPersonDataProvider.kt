package site.xiaozk.dailyfitness.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import site.xiaozk.dailyfitness.repository.model.BodyDataWithDate
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord
import java.time.Instant
import java.time.ZoneId

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
class DailyPersonDataProvider : PreviewParameterProvider<BodyDataRecord> {
    override val values: Sequence<BodyDataRecord>
        get() = sequenceOf(
            BodyDataRecord(0, Instant.now(), 70f, 70f, 60f, 70f, 0.2f),
            BodyDataRecord(0, Instant.now(), 70f, 70f, 60f, 70f, 0.2f),
            BodyDataRecord(0, Instant.now(), 70f, 70f, 60f, 70f, 0.2f)
        )
}

class BodyPageDataProvider : PreviewParameterProvider<BodyDataWithDate> {
    override val values: Sequence<BodyDataWithDate>
        get() = sequenceOf(
            BodyDataWithDate(
                mapOf(
                    Instant.now().atZone(ZoneId.systemDefault()).toLocalDate() to DailyPersonDataProvider().values.toList()
                )
            )
        )
}