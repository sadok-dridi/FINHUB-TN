import datetime
from decimal import Decimal, ROUND_HALF_UP

def format_decimal(value):
    d = Decimal(str(value)).quantize(Decimal("0.001"), rounding=ROUND_HALF_UP)
    return str(d)

def format_date(dt):
    dt = dt.replace(microsecond=0)
    iso = dt.isoformat() 
    if dt.second == 0:
        return iso[:-3] # Remove :00
    return iso

# Test Data (Matching Java TestHashFormat.java)
t1 = datetime.datetime(2023, 10, 27, 10, 0, 0)
t2 = datetime.datetime(2023, 10, 27, 10, 0, 5)
d1 = 100.5
d2 = 100.00

print(f"T1 (00 sec): {format_date(t1)}")
print(f"T2 (05 sec): {format_date(t2)}")
print(f"D1 (100.5): {format_decimal(d1)}")
print(f"D2 (100.00): {format_decimal(d2)}")
