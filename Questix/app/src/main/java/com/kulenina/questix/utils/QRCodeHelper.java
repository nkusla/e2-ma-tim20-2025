package com.kulenina.questix.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Hashtable;

public class QRCodeHelper {
    private static final int QR_CODE_SIZE = 512;
    private static final String QR_PREFIX = "QUESTIX_USER:";

    public static Bitmap generateQRCode(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }

        try {
            String qrData = QR_PREFIX + userId;

            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);

            return bitMatrixToBitmap(bitMatrix);

        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap bitMatrixToBitmap(BitMatrix bitMatrix) {
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    public static String extractUserIdFromQR(String qrData) {
        if (qrData != null && qrData.startsWith(QR_PREFIX)) {
            return qrData.substring(QR_PREFIX.length());
        }
        return null;
    }

    public static boolean isValidQuestixUserQR(String qrData) {
        return qrData != null && qrData.startsWith(QR_PREFIX) && qrData.length() > QR_PREFIX.length();
    }
}
