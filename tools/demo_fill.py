"""Fill a StudyGrid backup with realistic history (Gimnasio + Estudio) from a start date to today.

Existing goals other than Gimnasio/Estudio are kept untouched. If Gimnasio/Estudio already exist, their
logs are replaced (ids, colors, targets and positions kept); otherwise they are created at the end.

Usage: python demo_fill.py <exported.json> <out.json> <start YYYY-MM-DD> <today YYYY-MM-DD>
"""
import json
import random
import sys
from datetime import date, timedelta


def argb(hex_rgb: int) -> int:
    return (0xFF000000 | hex_rgb) - (1 << 32)  # signed 32-bit, as Kotlin stores it


src, out, start_s, today_s = sys.argv[1:5]
start, today = date.fromisoformat(start_s), date.fromisoformat(today_s)
data = json.load(open(src, encoding="utf-8"))
assert data.get("version") == 1, "unexpected backup version"
rng = random.Random(2026)  # deterministic: same input -> same history

goals = data["goals"]
by_title = {g["title"]: g for g in goals}
next_id = max((g["id"] for g in goals), default=0) + 1
next_pos = max((g.get("position", i) for i, g in enumerate(goals)), default=-1) + 1


def ensure(title, desc, color, weekly):
    global next_id, next_pos
    if title in by_title:
        return by_title[title]
    g = {"id": next_id, "title": title, "description": desc, "color": argb(color),
         "weeklyTarget": weekly, "position": next_pos}
    next_id += 1
    next_pos += 1
    goals.append(g)
    return g


gym = ensure("Gimnasio", "Pesas y cardio", 0xFF8A65, 4)
study = ensure("Estudio", "Roadmap Data Engineer", 0x2BD9FE, 6)
logs = [l for l in data["logs"] if l["goalId"] not in (gym["id"], study["id"])]

study_topics = {7: ["SQL básico", "JOINs y GROUP BY", "Subqueries", "Índices"],
                8: ["Kimball cap. 1", "Kimball cap. 2", "Kimball cap. 3", "Modelo estrella"],
                9: ["Modelos en dbt", "Tests en dbt", "Airflow DAGs", "Orquestación"],
                10: ["Spark DataFrames", "Joins en Spark", "Particionado", "Spark SQL"],
                11: ["Proyecto final", "Window Functions", "Pipeline end-to-end", "Repaso general"]}
gym_split = {0: "Pierna", 1: "Pecho y tríceps", 2: "Espalda y bíceps", 3: "Cardio 30 min", 4: "Hombro", 5: "Full body"}

vacation = (date(2026, 8, 10), date(2026, 8, 16))   # no study
flu = (date(2026, 10, 12), date(2026, 10, 18))      # no gym


def within(d, span):
    return span[0] <= d <= span[1]


day = start
while day <= today:
    wd = day.weekday()
    months_in = (day.year - start.year) * 12 + day.month - start.month  # 0 in July .. 4 in November

    # Estudio: ramps up month by month; Sundays mostly off; vacation week; a few random misses.
    if not within(day, vacation) and not (wd == 6 and rng.random() < 0.8) and rng.random() > 0.08:
        base = 1 + months_in * 0.6 + (0.6 if wd in (1, 2, 3) else 0) - (0.8 if wd == 5 else 0)
        lvl = max(1, min(4, round(base + rng.uniform(-0.8, 0.8))))
        topic = rng.choice(study_topics.get(day.month, ["Repaso"]))
        logs.append({"goalId": study["id"], "date": day.isoformat(), "intensity": lvl, "note": topic})

    # Gimnasio: 3 days/week at first, 4-5 from September; flu week; occasional skips.
    planned = {0, 2, 4} if day.month < 9 else {0, 1, 2, 4, 5}
    if wd in planned and not within(day, flu) and rng.random() > 0.12:
        lvl = max(1, min(4, round(2 + months_in * 0.35 + rng.uniform(-1, 1.2))))
        logs.append({"goalId": gym["id"], "date": day.isoformat(), "intensity": lvl, "note": gym_split[wd]})

    day += timedelta(days=1)

data["goals"], data["logs"] = goals, logs
json.dump(data, open(out, "w", encoding="utf-8"), ensure_ascii=False, indent=2)

# Self-check before anything reaches the phone.
keys = [(l["goalId"], l["date"]) for l in logs]
assert len(keys) == len(set(keys)), "duplicate day"
assert all(1 <= l["intensity"] <= 4 for l in logs if l["goalId"] in (gym["id"], study["id"]))
for g in (gym, study):
    mine = [l for l in logs if l["goalId"] == g["id"]]
    hist = {k: sum(l["intensity"] == k for l in mine) for k in range(1, 5)}
    print(f"{g['title']}: {len(mine)} días, niveles {hist}, {mine[0]['date']} -> {mine[-1]['date']}")
print("kept:", [g["title"] for g in goals if g["id"] not in (gym["id"], study["id"])])
