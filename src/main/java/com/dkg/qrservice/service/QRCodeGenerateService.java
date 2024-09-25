package com.dkg.qrservice.service;

import com.dkg.qrservice.util.DKG_WriterException;

import java.io.IOException;
import java.util.Map;

public interface QRCodeGenerateService {
    Map<String, Object> downloadQRCode(String inputString, int width, int height) throws IOException, DKG_WriterException;
    byte[] getQRCodeImage(String text, int width, int height) throws DKG_WriterException, IOException ;
}
