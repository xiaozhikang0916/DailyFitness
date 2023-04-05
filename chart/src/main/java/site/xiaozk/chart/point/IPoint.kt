package site.xiaozk.chart.point

import androidx.compose.runtime.Stable

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/4/5
 */

/**
 * An interface to describe how a point will be
 * displayed in the chart
 */
@Stable
interface IPoint: Comparable<IPoint> {
    /**
     * The x-axis value of this point
     */
    val xValue: Float

    /**
     * The y-axis value of this point
     */
    val yValue: Float

    /**
     * How tho display the x value in string of this point
     */
    val xDisplay: String

    /**
     * How tho display the y value in string of this point
     */
    val yDisplay: String

    override fun compareTo(other: IPoint): Int {
        return xValue.compareTo(other.xValue)
    }
}

data class PointHolder(
    override val xValue: Float = 0f,
    override val yValue: Float = 0f,
    override val xDisplay: String = xValue.toString(),
    override val yDisplay: String = yValue.toString(),
): IPoint