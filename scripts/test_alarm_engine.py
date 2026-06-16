"""
告警评估引擎逻辑调整 — 端到端测试脚本

测试对象：
  - AlarmEvaluationEngine (zwei-iot-alarm)
  - AlarmDedupService (Redis 计数器)
  - DB 表: alarm_criteria (id=11/12/13), alarm_record

前置条件：
  1. 后端已启动 (profile=local)，MQTT 1883 / MySQL 3306 / Redis 6379 可访问
  2. alarm_criteria 表中已存在 id=11/12/13 三条判据：
     - CR_HP_A_rain10_blue  (HP 17, rainfall_hour>10, persistCount=3, level=blue)
     - CR_HP_B_rain50_red   (HP 17, rainfall_hour>50, persistCount=2, level=red)
     - CR_MT_C_rain5_blue   (HP NULL, rainfall_hour>5,  persistCount=1, level=blue)
  3. 设备 10 (BP_YL_01) 绑定 HP 17，传感器 YL_1 拥有 rainfall_hour 属性

用法：
  python scripts/test_alarm_engine.py
  python scripts/test_alarm_engine.py --case 1     # 仅跑用例 1
"""
import json
import sys
import time
import argparse
from pathlib import Path

# Windows 控制台默认 GBK，强制 UTF-8 输出避免 UnicodeEncodeError
try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
except Exception:
    pass

import paho.mqtt.client as mqtt
import pymysql
import redis

# ──────────────────── 配置 ────────────────────
MQTT_HOST = "127.0.0.1"
MQTT_PORT = 1883
DEVICE_CODE = "BP_YL_01"
SENSOR_CODE = "YL_1"
DEVICE_USER = "48AN4J"          # device.auth_username
DEVICE_PASS = "Km0I8Ynm"        # device.auth_password
DEVICE_ID = 10
HAZARD_POINT_ID = 17
ATTR_CODE = "rainfall_hour"

MYSQL_HOST = "127.0.0.1"
MYSQL_PORT = 3306
MYSQL_USER = "root"
MYSQL_PASS = "wodepassword"
MYSQL_DB = "geo_hazard_monitor"

REDIS_HOST = "127.0.0.1"
REDIS_PORT = 6379
REDIS_DB = 0

CRITERIA_IDS = {"CR_HP_A": 11, "CR_HP_B": 12, "CR_MT_C": 13}

INTERVAL_SEC = 1.5  # 每条上报间隔，秒

# ──────────────────── 工具函数 ────────────────────
def log(msg):
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)


def connect_mqtt():
    client = mqtt.Client(client_id=f"test-{int(time.time())}", protocol=mqtt.MQTTv311)
    client.username_pw_set(DEVICE_USER, DEVICE_PASS)
    client.connect(MQTT_HOST, MQTT_PORT, keepalive=30)
    client.loop_start()
    return client


def publish(client, value):
    topic = f"sys/v1/{DEVICE_CODE}/{SENSOR_CODE}/updata"
    payload = {
        "version": "1.0",
        "sensorNo": SENSOR_CODE,
        "timestamp": int(time.time() * 1000),
        "data": {ATTR_CODE: value},
    }
    info = client.publish(topic, json.dumps(payload), qos=1)
    info.wait_for_publish(timeout=5)
    log(f"  → publish value={value} to {topic}")


def mysql_conn():
    return pymysql.connect(host=MYSQL_HOST, port=MYSQL_PORT, user=MYSQL_USER,
                           password=MYSQL_PASS, database=MYSQL_DB, charset="utf8mb4",
                           cursorclass=pymysql.cursors.DictCursor)


def redis_conn():
    return redis.Redis(host=REDIS_HOST, port=REDIS_PORT, db=REDIS_DB,
                       decode_responses=True)


def reset_state(case_name):
    """每个用例开始前：清空 device 10 的告警记录 + Redis 计数 key"""
    log(f"--- 重置状态：{case_name} ---")
    with mysql_conn() as conn:
        with conn.cursor() as cur:
            # 删除 alarm_record 及关联日志（如果有外键）
            cur.execute("SELECT id FROM alarm_record WHERE device_id = %s", (DEVICE_ID,))
            ids = [row["id"] for row in cur.fetchall()]
            if ids:
                placeholders = ",".join(["%s"] * len(ids))
                cur.execute(f"DELETE FROM alarm_record_trigger_detail WHERE alarm_record_id IN ({placeholders})", ids)
                cur.execute(f"DELETE FROM alarm_record_action_log WHERE alarm_record_id IN ({placeholders})", ids)
                cur.execute(f"DELETE FROM alarm_record WHERE id IN ({placeholders})", ids)
                conn.commit()
                log(f"  清理 {len(ids)} 条历史 alarm_record")
            else:
                log("  无历史 alarm_record")

    r = redis_conn()
    deleted = 0
    for key in r.scan_iter("alarm:pre-trigger:*"):
        r.delete(key)
        deleted += 1
    for key in r.scan_iter("alarm:last-trigger:*"):
        r.delete(key)
        deleted += 1
    log(f"  清理 {deleted} 个 Redis 计数 key")


def query_alarms():
    with mysql_conn() as conn:
        with conn.cursor() as cur:
            cur.execute("""
                SELECT id, alarm_level, alarm_level_text, criteria_id,
                       trigger_count, alarm_message, create_time, last_trigger_time
                FROM alarm_record
                WHERE device_id = %s
                ORDER BY id
            """, (DEVICE_ID,))
            return cur.fetchall()


def query_redis_counters():
    r = redis_conn()
    out = {}
    for key in r.scan_iter("alarm:pre-trigger:*"):
        out[key] = r.get(key)
    return out


def send_sequence(client, values):
    for v in values:
        publish(client, v)
        time.sleep(INTERVAL_SEC)


# ──────────────────── 断言辅助 ────────────────────
class Result:
    def __init__(self):
        self.passed = []
        self.failed = []

    def check(self, name, ok, expected, actual):
        if ok:
            self.passed.append(name)
            log(f"  ✓ {name}: PASS")
        else:
            self.failed.append(name)
            log(f"  ✗ {name}: FAIL — expected={expected!r}, actual={actual!r}")


# ──────────────────── 测试用例 ────────────────────
def case_1_priority_exclusion(client, result):
    """用例1：优先级排他 — 仅满足 CR_MT_C，HP 判据存在 → 0 条告警"""
    reset_state("case1 优先级排他")
    send_sequence(client, [6])
    time.sleep(1)
    alarms = query_alarms()
    result.check("c1_0_alarm", len(alarms) == 0, "0 条告警", f"{len(alarms)} 条: {alarms}")


def case_2_independent_accumulation(client, result):
    """用例2：计数器独立 — value=11 ×3 → CR_HP_A 达 persistCount=3 触发；CR_HP_B 计数始终为 0"""
    reset_state("case2 计数器独立")
    send_sequence(client, [11, 11, 11])
    time.sleep(1)
    alarms = query_alarms()
    result.check("c2_count", len(alarms) == 1, "1 条告警", f"{len(alarms)} 条")
    if alarms:
        a = alarms[0]
        result.check("c2_level", a["alarm_level"] == 1, "level=1(蓝)", f"level={a['alarm_level']}")
        result.check("c2_criteria", a["criteria_id"] == CRITERIA_IDS["CR_HP_A"],
                     f"criteria={CRITERIA_IDS['CR_HP_A']}", f"criteria={a['criteria_id']}")
    # CR_HP_B 计数应不存在 (11 不满足 >50 → 每次 clearPreTrigger)
    counters = query_redis_counters()
    cr_hp_b_keys = [k for k in counters if f":{CRITERIA_IDS['CR_HP_B']}:" in k]
    result.check("c2_B_no_counter", len(cr_hp_b_keys) == 0,
                 "无 CR_HP_B 计数 key", f"keys={cr_hp_b_keys}")


def case_3_level_independent_reset(client, result):
    """用例3：等级独立重置 — 序列 [60, 20, 60, 60]
    预期：
      step1 (60): blue=1, red=1
      step2 (20): blue=2, red=0(重置)
      step3 (60): blue=3 ✓候选(level=1), red=1
      step4 (60): blue 重新计数 (上次已 trigger 清零)，red=2 ✓候选(level=4)
    最终：2 条 alarm_record — 一条 level=1 (CR_HP_A)，一条 level=4 (CR_HP_B)
    """
    reset_state("case3 等级独立重置")
    send_sequence(client, [60, 20, 60, 60])
    time.sleep(1)
    alarms = query_alarms()
    result.check("c3_count", len(alarms) == 2, "2 条告警", f"{len(alarms)} 条")
    by_crit = {a["criteria_id"]: a for a in alarms}

    a = by_crit.get(CRITERIA_IDS["CR_HP_A"])
    result.check("c3_A_exists", a is not None, "CR_HP_A 告警存在", "未生成 CR_HP_A 告警")
    if a:
        result.check("c3_A_level", a["alarm_level"] == 1, "level=1", f"level={a['alarm_level']}")

    b = by_crit.get(CRITERIA_IDS["CR_HP_B"])
    result.check("c3_B_exists", b is not None, "CR_HP_B 告警存在", "未生成 CR_HP_B 告警")
    if b:
        result.check("c3_B_level", b["alarm_level"] == 4, "level=4", f"level={b['alarm_level']}")


def case_4_highest_level_wins(client, result):
    """用例4：最高等级胜出 — value=60 ×2，第 2 步 CR_HP_B red 触发，CR_HP_A blue 未达 persistCount=3
    最终：1 条 level=4 告警
    """
    reset_state("case4 最高等级胜出")
    send_sequence(client, [60, 60])
    time.sleep(1)
    alarms = query_alarms()
    result.check("c4_count", len(alarms) == 1, "1 条告警", f"{len(alarms)} 条")
    if alarms:
        a = alarms[0]
        result.check("c4_level", a["alarm_level"] == 4, "level=4(红)", f"level={a['alarm_level']}")
        result.check("c4_criteria", a["criteria_id"] == CRITERIA_IDS["CR_HP_B"],
                     f"criteria={CRITERIA_IDS['CR_HP_B']}", f"criteria={a['criteria_id']}")


def case_5_multi_criteria_candidates(client, result):
    """用例5：多判据候选合并 — value=51 ×3
    预期：
      step1 (51): blue=1, red=1
      step2 (51): blue=2, red=2 ✓候选(level=4) → 触发 CR_HP_B 告警
      step3 (51): blue=3 ✓候选(level=1), red 重新从 1 开始 → 触发 CR_HP_A 告警
    最终：2 条 — 一条 level=4 (CR_HP_B)，一条 level=1 (CR_HP_A)
    """
    reset_state("case5 多判据候选")
    send_sequence(client, [51, 51, 51])
    time.sleep(1)
    alarms = query_alarms()
    result.check("c5_count", len(alarms) == 2, "2 条告警", f"{len(alarms)} 条")
    by_crit = {a["criteria_id"]: a for a in alarms}

    b = by_crit.get(CRITERIA_IDS["CR_HP_B"])
    result.check("c5_B_exists", b is not None, "CR_HP_B 告警存在", "未生成")
    if b:
        result.check("c5_B_level", b["alarm_level"] == 4, "level=4", f"level={b['alarm_level']}")

    a = by_crit.get(CRITERIA_IDS["CR_HP_A"])
    result.check("c5_A_exists", a is not None, "CR_HP_A 告警存在", "未生成")
    if a:
        result.check("c5_A_level", a["alarm_level"] == 1, "level=1", f"level={a['alarm_level']}")


CASES = {
    1: ("优先级排他", case_1_priority_exclusion),
    2: ("计数器独立累加", case_2_independent_accumulation),
    3: ("等级独立重置", case_3_level_independent_reset),
    4: ("最高等级胜出", case_4_highest_level_wins),
    5: ("多判据候选合并", case_5_multi_criteria_candidates),
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--case", type=int, choices=list(CASES.keys()),
                        help="只跑指定用例；不指定则全部按顺序跑")
    args = parser.parse_args()

    log("=== 连接 MQTT broker ===")
    client = connect_mqtt()
    time.sleep(1)

    overall = Result()
    cases_to_run = [args.case] if args.case else sorted(CASES.keys())

    for n in cases_to_run:
        name, fn = CASES[n]
        log(f"\n=== 用例 {n}: {name} ===")
        per_case = Result()
        try:
            fn(client, per_case)
        except Exception as e:
            log(f"  ✗ 用例异常：{e!r}")
            per_case.failed.append(f"case{n}_exception")
        log(f"用例 {n} 结果：{len(per_case.passed)} PASS / {len(per_case.failed)} FAIL")
        if per_case.failed:
            log(f"  失败项：{per_case.failed}")
        overall.passed.extend(per_case.passed)
        overall.failed.extend(per_case.failed)

    client.loop_stop()
    client.disconnect()

    log(f"\n=== 总计：{len(overall.passed)} PASS / {len(overall.failed)} FAIL ===")
    if overall.failed:
        log(f"失败项：{overall.failed}")
        sys.exit(1)


if __name__ == "__main__":
    main()
