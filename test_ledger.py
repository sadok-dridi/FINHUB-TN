import mysql.connector
import os
import decimal

def get_db_connection():
    return mysql.connector.connect(
        host=os.getenv('DB_HOST', 'pidev-mariadb'),
        user=os.getenv('DB_USER', 'pidev_user'),
        password=os.getenv('DB_PASSWORD', 'Pidev@1122SpidevS'),
        database=os.getenv('DB_NAME', 'finhub')
    )

def test_ledger():
    try:
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT * FROM wallet LIMIT 5;")
        wallet = cursor.fetchall()
        print("--- WALLETS ---")
        for w in wallet:
            print(f"ID: {w['id']} | Balance: {w['balance']} | Escrow: {w['escrow_balance']} | Status: {w['status']}")
            
            cursor.execute("SELECT id, type, amount, reference FROM wallet_transaction WHERE wallet_id = %s ORDER BY created_at ASC", (w['id'],))
            txs = cursor.fetchall()
            calc_balance = decimal.Decimal(0)
            calc_escrow = decimal.Decimal(0)
            print("  Transactions:")
            for tx in txs:
                print(f"    - {tx['type']} : {tx['amount']}")
                amt = tx['amount']
                if tx['type'] in ["CREDIT", "DEPOSIT", "RELEASE", "TRANSFER_RECEIVED", "GENESIS", "ESCROW_RCVD", "ESCROW_FEE", "ESCROW_REFUND"]:
                    calc_balance += amt
                elif tx['type'] in ["DEBIT", "HOLD", "TRANSFER_SENT"]:
                    calc_balance -= amt
                
                if tx['type'] == "HOLD":
                    calc_escrow += amt
                elif tx['type'] in ["RELEASE", "ESCROW_SENT", "ESCROW_REFUND"]:
                    calc_escrow -= amt

            print(f"  CALC BALANCE: {calc_balance} vs ACTUAL: {w['balance']}")
            print(f"  CALC ESCROW: {calc_escrow} vs ACTUAL: {w['escrow_balance']}")
            if calc_balance != w['balance'] or calc_escrow != w['escrow_balance']:
                print("  !!! MISMATCH DETECTED !!!")

    except Exception as e:
        print(f"Error: {e}")
    finally:
        if 'conn' in locals() and conn.is_connected():
            conn.close()

if __name__ == "__main__":
    test_ledger()
