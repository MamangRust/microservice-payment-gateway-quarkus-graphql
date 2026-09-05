#!/usr/bin/env python3
"""Fix PostgreSQL/Hibernate SQL cast issues in stats repositories:
1. EXTRACT(YEAR FROM :yearDate)::int  ->  EXTRACT(YEAR FROM CAST(:yearDate AS DATE))::int
2. MAKE_DATE(:year::int, ...)         ->  MAKE_DATE(CAST(:year AS INTEGER), ...)
"""
import glob
import os

ROOT = "/home/hoover/Projects/java/quarkus-grpc-payment_quarkus"

OLD_A = "EXTRACT(YEAR FROM :yearDate)::int"
NEW_A = "EXTRACT(YEAR FROM CAST(:yearDate AS DATE))::int"
OLD_B = "MAKE_DATE(:year::int, m.m, 1)"
NEW_B = "MAKE_DATE(CAST(:year AS INTEGER), m.m, 1)"

files = sorted(glob.glob(os.path.join(ROOT, "*", "src", "main", "java", "**", "*Repository.java"), recursive=True))

total_a = 0
total_b = 0
changed = []
for f in files:
    with open(f, "r", encoding="utf-8") as fh:
        content = fh.read()
    orig = content
    n_a = content.count(OLD_A)
    n_b = content.count(OLD_B)
    content = content.replace(OLD_A, NEW_A).replace(OLD_B, NEW_B)
    if content != orig:
        with open(f, "w", encoding="utf-8") as fh:
            fh.write(content)
        total_a += n_a
        total_b += n_b
        changed.append(f"{os.path.relpath(f, ROOT)} (A={n_a}, B={n_b})")

print(f"TOTAL_A={total_a} TOTAL_B={total_b} FILES={len(changed)}")
for c in changed:
    print("  ", c)
