import sys
import subprocess

try:
    from rembg import remove
except ImportError:
    subprocess.check_call([sys.executable, "-m", "pip", "install", "rembg", "Pillow"])
    from rembg import remove

input_path = r"C:\Users\sadok\OneDrive\Documents\PIDEV\FINHUB-TN\FINHUB-WEB\public\images\logo.jpeg"
output_path = r"C:\Users\sadok\OneDrive\Documents\PIDEV\FINHUB-TN\FINHUB-WEB\public\images\logo_transparent.png"

with open(input_path, 'rb') as i:
    with open(output_path, 'wb') as o:
        input_data = i.read()
        output_data = remove(input_data)
        o.write(output_data)
