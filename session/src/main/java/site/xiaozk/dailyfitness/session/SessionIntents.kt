package site.xiaozk.dailyfitness.session

/**
 * Intent contract between the [:session] module and the app.
 *
 * The session module only defines the constants; the app maps them to its own
 * NavKeys / navigation (see [WorkoutSessionNavProvider]).
 */
object SessionIntents {
    /** Notification action: open the "add one set" page. */
    const val ACTION_ADD_SET = "site.xiaozk.dailyfitness.session.action.ADD_SET"

    /** Notification action: open today's workout page. */
    const val ACTION_OPEN_TODAY = "site.xiaozk.dailyfitness.session.action.OPEN_TODAY"

    /** Service action: start a workout session. */
    const val ACTION_START = "site.xiaozk.dailyfitness.session.action.START"

    /** Service action: finish the workout session. */
    const val ACTION_FINISH = "site.xiaozk.dailyfitness.session.action.FINISH"

    /** Optional extra: preselected train action id (for ACTION_ADD_SET). */
    const val EXTRA_ACTION_ID = "site.xiaozk.dailyfitness.session.extra.ACTION_ID"
}
