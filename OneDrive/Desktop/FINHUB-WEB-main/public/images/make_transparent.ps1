$code = @"
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;
using System;

public class ImageProcessor {
    public static void MakeWhiteTransparent(string inPath, string outPath) {
        Bitmap bmp = new Bitmap(inPath);
        BitmapData data = bmp.LockBits(new Rectangle(0, 0, bmp.Width, bmp.Height), ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
        int stride = data.Stride;
        IntPtr ptr = data.Scan0;
        int bytes = Math.Abs(data.Stride) * bmp.Height;
        byte[] rgbValues = new byte[bytes];
        Marshal.Copy(ptr, rgbValues, 0, bytes);
        for (int counter = 0; counter < rgbValues.Length; counter += 4) {
            byte b = rgbValues[counter];
            byte g = rgbValues[counter + 1];
            byte r = rgbValues[counter + 2];
            if (r > 230 && g > 230 && b > 230) {
                rgbValues[counter + 3] = 0; // set alpha to 0
            }
        }
        Marshal.Copy(rgbValues, 0, ptr, bytes);
        bmp.UnlockBits(data);
        bmp.Save(outPath, ImageFormat.Png);
        bmp.Dispose();
    }
}
"@
Add-Type -TypeDefinition $code -ReferencedAssemblies System.Drawing
[ImageProcessor]::MakeWhiteTransparent("c:\Users\sadok\OneDrive\Documents\PIDEV\FINHUB-TN\FINHUB-WEB\public\images\logo.jpeg", "c:\Users\sadok\OneDrive\Documents\PIDEV\FINHUB-TN\FINHUB-WEB\public\images\logo_transparent.png")
