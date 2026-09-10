#!/usr/bin/env python3
"""Regenerate the debug test data inside fitness.db (Room asset).

Usage:
    python3 generate_test_data.py [path/to/fitness.db]

Writes a meaningful 4-split history relative to *today* (last 12 days, never
today) into the existing Room schema, preserving the schema/identity hash so
that `createFromAsset("fitness.db")` keeps working.

Data written (debug builds only):
  - 5 parts / 13 actions with realistic Chinese names and correct parameter kinds
  - 12 sessions (3 cycles of the split) with progressive overload
  - nothing for "today" -> AI Coach Case A testable immediately,
    Case B after adding one set in the app.

Re-run this script to refresh the dates before manual acceptance testing.
"""

import datetime as dt
import sqlite3
import sys

DB = sys.argv[1] if len(sys.argv) > 1 else "fitness.db"
TODAY = dt.date.today()
SESSION_HOUR = 19

# part name -> list of (action name, weighted, counted, timed, sets, reps, weight_kg, weight_step_kg, duration_s, duration_step_s)
LIBRARY = [
    ("胸部", [
        ("杠铃卧推", True, True, False, 4, 8, 60.0, 2.5, 0, 0),
        ("哑铃飞鸟", True, True, False, 3, 12, 10.0, 1.0, 0, 0),
        ("俯卧撑", False, True, False, 3, 15, 0.0, 0.0, 0, 0),
    ]),
    ("背部", [
        ("引体向上", False, True, False, 3, 8, 0.0, 0.0, 0, 0),
        ("杠铃划船", True, True, False, 4, 10, 50.0, 2.5, 0, 0),
        ("高位下拉", True, True, False, 3, 12, 45.0, 2.5, 0, 0),
    ]),
    ("腿部", [
        ("杠铃深蹲", True, True, False, 4, 8, 80.0, 5.0, 0, 0),
        ("腿举", True, True, False, 3, 10, 120.0, 10.0, 0, 0),
        ("保加利亚分腿蹲", False, True, False, 3, 12, 0.0, 0.0, 0, 0),
    ]),
    ("肩部", [
        ("哑铃推举", True, True, False, 3, 10, 15.0, 1.5, 0, 0),
        ("侧平举", True, True, False, 3, 15, 7.5, 0.5, 0, 0),
    ]),
    ("核心", [
        ("平板支撑", False, False, True, 3, 0, 0.0, 0.0, 60, 15),
        ("卷腹", False, True, False, 3, 20, 0.0, 0.0, 0, 0),
    ]),
]

# 4-split cycle; one cycle = 4 sessions.
SPLIT_CYCLE = [["胸部"], ["背部"], ["腿部"], ["肩部", "核心"]]
HISTORY_DAYS = 12


def epoch_ms(date: dt.date, hour: int, minute: int = 0) -> int:
    moment = dt.datetime(date.year, date.month, date.day, hour, minute)
    return int(moment.timestamp() * 1000)


def main() -> None:
    con = sqlite3.connect(DB)
    con.execute("PRAGMA foreign_keys = ON")
    cur = con.cursor()

    # Make sure the (single) user row exists.
    cur.execute("SELECT uid FROM user ORDER BY uid LIMIT 1")
    row = cur.fetchone()
    if row is None:
        cur.execute("INSERT INTO user (name) VALUES ('app')")
        user_id = cur.lastrowid
    else:
        user_id = row[0]

    # Library --------------------------------------------------------------
    cur.execute("DELETE FROM daily_train_action")
    cur.execute("DELETE FROM train_action")
    cur.execute("DELETE FROM train_part")
    cur.execute("DELETE FROM sqlite_sequence WHERE name IN ('daily_train_action','train_action','train_part')")

    part_ids = {}
    action_ids = {}
    for part_index, (part_name, actions) in enumerate(LIBRARY, start=1):
        cur.execute("INSERT INTO train_part (id, part_name) VALUES (?, ?)", (part_index, part_name))
        part_ids[part_name] = part_index
        for action_offset, spec in enumerate(actions):
            name, weighted, counted, timed = spec[0], spec[1], spec[2], spec[3]
            action_id = len(action_ids) + 1
            cur.execute(
                "INSERT INTO train_action (id, action_name, partId, isTimedAction, isWeightedAction, isCountedAction) "
                "VALUES (?, ?, ?, ?, ?, ?)",
                (action_id, name, part_index, int(timed), int(weighted), int(counted)),
            )
            action_ids[name] = action_id

    # History --------------------------------------------------------------
    for cycle_index in range(1, HISTORY_DAYS + 1):
        date = TODAY - dt.timedelta(days=HISTORY_DAYS + 1 - cycle_index)  # 12..1 days ago
        part_names = SPLIT_CYCLE[(cycle_index - 1) % len(SPLIT_CYCLE)]
        week = (cycle_index - 1) // len(SPLIT_CYCLE)
        session_start = epoch_ms(date, SESSION_HOUR)

        for part_index, part_name in enumerate(part_names):
            for action_index, spec in enumerate(dict(LIBRARY)[part_name]):
                name, weighted, counted, timed, sets, reps, weight, weight_step, duration, duration_step = spec
                for set_index in range(sets):
                    action_time = (
                        session_start
                        + part_index * 15 * 60_000
                        + action_index * 8 * 60_000
                        + set_index * 3 * 60_000
                    )
                    cur.execute(
                        "INSERT INTO daily_train_action "
                        "(usingActionId, userId, actionTime, takenCount, note, takenDuration, timeUnit, takenWeight, weightUnit) "
                        "VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?)",
                        (
                            action_ids[name],
                            user_id,
                            action_time,
                            reps if counted else None,
                            float(duration + duration_step * week) if timed else None,
                            "Sec" if timed else None,
                            float(weight + weight_step * week) if weighted else None,
                            "Kg" if weighted else None,
                        ),
                    )

    con.commit()
    # Flush WAL so the asset's main file is self-contained for createFromAsset.
    cur.execute("PRAGMA wal_checkpoint(TRUNCATE)")
    con.commit()
    counts = {
        "parts": cur.execute("SELECT count(*) FROM train_part").fetchone()[0],
        "actions": cur.execute("SELECT count(*) FROM train_action").fetchone()[0],
        "records": cur.execute("SELECT count(*) FROM daily_train_action").fetchone()[0],
    }
    con.close()
    print(f"seeded {DB}: {counts}")


if __name__ == "__main__":
    main()
