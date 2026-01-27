Reguły wyliczania Interwałów Heartbeat

Zaimplementuj logikę wyliczania pola interval komunikatu zwrotnego BootNotiﬁcationResponse.

Wymagania:

Heartbeat interwal może być dostosowany przez administratora systemu dla:
- konkretnego podzbioru urządzeń wskazanych po deviceId,
- wszystkich urządzeń określonego modelu (vendor + model)

gdzie model może być określony za pomocą wyrażenia regularnego

- pozostałych urządzeń (interwał domyślny).

Obecne reguły:

Interwały dla podzbioru urządzeń:

Interval

DeviceIds

600s

EVB-P4562137, ALF-9571445

2700s

t53_8264_019, EVB-P15079256

Interwały dla modeli:

Interval

60s

120s

Interwał domyślny: 1800s

Vendor

Alfen BV

Model (regex)

NG920-5250[6-9]

ChargeStorm AB

Chargestorm Connected
