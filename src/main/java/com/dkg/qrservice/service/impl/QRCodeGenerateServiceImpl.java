package com.dkg.qrservice.service.impl;

import com.dkg.qrservice.service.QRCodeGenerateService;
import com.dkg.qrservice.util.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

@Service
public class QRCodeGenerateServiceImpl implements QRCodeGenerateService {
    @Override
    public Map<String, Object> downloadQRCode(String inputString, int width, int height) throws IOException, DKG_WriterException {

        Map<String, Object> datMap = new HashMap<>();
        datMap.put("content", getQRCodeImage(inputString, width, height));
        datMap.put("fileName", "QR_Code.png");

        return datMap;

    }

    @Override
    public byte[] getQRCodeImage(String text, int width, int height) throws DKG_WriterException, IOException {
        DKG_QRCodeDKGWriter dkgQrCodeWriter = new DKG_QRCodeDKGWriter();
        Hashtable<DKG_EncodeHintType, DKG_ErrorCorrectionLevel> hintMap = new Hashtable<>();
        hintMap.put(DKG_EncodeHintType.ERROR_CORRECTION, DKG_ErrorCorrectionLevel.L);
        DKG_BitMatrix dkgBitMatrix = dkgQrCodeWriter.encode(text, DKG_BarcodeFormat.QR_CODE, width, height, hintMap);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        //MatrixToImageConfig con = new MatrixToImageConfig( 0xFF000002 , 0xFFFFC041 ) ;

        //MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream,con);
        DKG_MatrixToImageWriter.writeToStream(dkgBitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        return pngData;
    }
}
