#!/usr/bin/env python3
"""Temporary: fix unbalanced parentheses on withSession wrap lines."""
import glob
import re

FILES = []
for pat in [
    "transaction/src/main/java/com/sanedge/transaction/handler/*.java",
    "withdraw/src/main/java/com/sanedge/withdraw/handler/*.java",
    "transfer/src/main/java/com/sanedge/transfer/handler/*.java",
    "saldo/src/main/java/com/sanedge/saldo/handler/*.java",
    "card/src/main/java/com/sanedge/card/handler/*.java",
    "topup/src/main/java/com/sanedge/topup/handler/*.java",
    "merchant/src/main/java/com/sanedge/merchant/handler/*.java",
]:
    FILES.extend(glob.glob(pat))
FILES = sorted(set(FILES))

WRAP_RE = re.compile(r"^(\s*)return withSession\(\(\) -> .+$")

fixed = 0
checked = 0
for path in FILES:
    with open(path, "r", encoding="utf-8") as f:
        lines = f.readlines()
    changed = False
    out = []
    for line in lines:
        m = WRAP_RE.match(line)
        if m:
            checked += 1
            # Only fix the trailing part of the line (strip comment/newline).
            content = line.rstrip("\n")
            opens = content.count("(")
            closes = content.count(")")
            if opens > closes:
                line = content + ")" * (opens - closes) + "\n"
                changed = True
        out.append(line)
    if changed:
        with open(path, "w", encoding="utf-8") as f:
            f.writelines(out)
        fixed += 1
        print("FIXED " + path)

print("FIXED_FILES=" + str(fixed))
print("WRAP_LINES_CHECKED=" + str(checked))
