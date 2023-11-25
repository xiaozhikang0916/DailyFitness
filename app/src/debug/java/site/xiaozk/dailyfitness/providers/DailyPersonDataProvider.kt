package site.xiaozk.dailyfitness.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import site.xiaozk.dailyfitness.repository.model.BodyDataWithDate
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
class DailyPersonDataProvider : PreviewParameterProvider<BodyDataRecord> {
    override val values: Sequence<BodyDataRecord>
        get() = sequenceOf(
            BodyDataRecord(0, Clock.System.now(), 70f, 70f, 60f, 70f, 0.2f),
            BodyDataRecord(0, Clock.System.now(), 70f, 70f, 60f, 70f, 0.2f),
            BodyDataRecord(0, Clock.System.now(), 70f, 70f, 60f, 70f, 0.2f)
        )
}

class BodyPageDataProvider : PreviewParameterProvider<BodyDataWithDate> {
    override val values: Sequence<BodyDataWithDate>
        get() = sequenceOf(
            BodyDataWithDate(
                mapOf(
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date to DailyPersonDataProvider().values.toList()
                )
            )
        )
}