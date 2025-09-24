package com.example.habibitar.util;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public final class QR {
    public static Bitmap make(String content, int size) throws WriterException {
        BarcodeEncoder enc = new BarcodeEncoder();
        return enc.encodeBitmap(content, BarcodeFormat.QR_CODE, size, size);
    }
}
