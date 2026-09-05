#!/usr/bin/env python3
"""Temporary: wrap gRPC query/stats handler service calls with Panache.withSession."""
import re

FILES = [
    "transaction/src/main/java/com/sanedge/transaction/handler/TransactionQueryGrpcHandler.java",
    "transaction/src/main/java/com/sanedge/transaction/handler/TransactionStatsAmountGrpcHandler.java",
    "transaction/src/main/java/com/sanedge/transaction/handler/TransactionStatsMethodGrpcHandler.java",
    "transaction/src/main/java/com/sanedge/transaction/handler/TransactionStatsStatusGrpcHandler.java",
    "withdraw/src/main/java/com/sanedge/withdraw/handler/WithdrawQueryGrpcHandler.java",
    "withdraw/src/main/java/com/sanedge/withdraw/handler/WithdrawStatsAmountGrpcHandler.java",
    "withdraw/src/main/java/com/sanedge/withdraw/handler/WithdrawStatsStatusGrpcHandler.java",
    "transfer/src/main/java/com/sanedge/transfer/handler/TransferQueryGrpcHandler.java",
    "transfer/src/main/java/com/sanedge/transfer/handler/TransferStatsAmountGrpcHandler.java",
    "transfer/src/main/java/com/sanedge/transfer/handler/TransferStatsStatusGrpcHandler.java",
    "saldo/src/main/java/com/sanedge/saldo/handler/SaldoStatsBalanceGrpcHandler.java",
    "saldo/src/main/java/com/sanedge/saldo/handler/SaldoStatsTotalBalanceGrpcHandler.java",
    "card/src/main/java/com/sanedge/card/handler/CardStatsBalanceGrpcHandler.java",
    "card/src/main/java/com/sanedge/card/handler/CardStatsTopupGrpcHandler.java",
    "card/src/main/java/com/sanedge/card/handler/CardStatsTransactionGrpcHandler.java",
    "card/src/main/java/com/sanedge/card/handler/CardStatsWithdrawGrpcHandler.java",
    "card/src/main/java/com/sanedge/card/handler/CardStatsTransferGrpcHandler.java",
    "card/src/main/java/com/sanedge/card/handler/CardDashboardGrpcHandler.java",
    "topup/src/main/java/com/sanedge/topup/handler/TopupStatsAmountGrpcHandler.java",
    "topup/src/main/java/com/sanedge/topup/handler/TopupStatsMethodGrpcHandler.java",
    "topup/src/main/java/com/sanedge/topup/handler/TopupStatsStatusGrpcHandler.java",
    "merchant/src/main/java/com/sanedge/merchant/handler/MerchantStatsAmountGrpcHandler.java",
    "merchant/src/main/java/com/sanedge/merchant/handler/MerchantStatsMethodGrpcHandler.java",
    "merchant/src/main/java/com/sanedge/merchant/handler/MerchantStatsTotalAmountGrpcHandler.java",
]

CALL_RE = re.compile(r"^(\s*)return ([a-z][a-zA-Z0-9]*)\.([a-zA-Z0-9]+)\(")

changed = []
skipped = []
for path in FILES:
    with open(path, "r", encoding="utf-8") as f:
        lines = f.readlines()

    content = "".join(lines)
    if "Panache.withSession" in content:
        skipped.append(path + " (already patched)")
        continue

    out = []
    wrapped = 0
    for line in lines:
        m = CALL_RE.match(line)
        if m:
            indent = m.group(1)
            field = m.group(2)
            method = m.group(3)
            head = m.group(0)
            tail = line[len(head):]
            out.append(indent + "return withSession(() -> " + field + "." + method + "(" + tail.lstrip())
            wrapped += 1
        else:
            out.append(line)

    if wrapped == 0:
        skipped.append(path + " (no calls wrapped)")
        continue

    out2 = []
    added_import = False
    for line in out:
        out2.append(line)
        if not added_import and line.startswith("import io.quarkus.grpc.GrpcService;"):
            out2.append("import io.quarkus.hibernate.reactive.panache.Panache;\n")
            added_import = True
    if not added_import:
        skipped.append(path + " (no GrpcService import found)")
        continue

    text = "".join(out2).rstrip("\n")
    if not text.endswith("\n}"):
        skipped.append(path + " (unexpected ending)")
        continue
    helper = (
        "\n    private <T> Uni<T> withSession(java.util.function.Supplier<Uni<T>> action) {\n"
        "        return Panache.withSession(action);\n"
        "    }\n"
    )
    text = text[:-1] + helper + "}"
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)
    changed.append(path + " (" + str(wrapped) + " calls)")

print("CHANGED:")
for c in changed:
    print("  " + c)
print("SKIPPED/ALREADY:")
for s in skipped:
    print("  " + s)
