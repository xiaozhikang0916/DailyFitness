package site.xiaozk.chart.point

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/4/5
 */
@JvmInline
value class Line(
    val points: List<IPoint>,
) {
    val minX: Float
        get() = points.minOf { it.xValue }
    val maxX: Float
        get() = points.maxOf { it.xValue }
    val minY: Float
        get() = points.minOf { it.yValue }
    val maxY: Float
        get() = points.maxOf { it.yValue }
}
