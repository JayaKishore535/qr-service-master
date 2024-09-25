package com.dkg.qrservice.util;
import java.util.Map;


public interface DKG_Writer {


  DKG_BitMatrix encode(String contents, DKG_BarcodeFormat format, int width, int height)
      throws DKG_WriterException;


  DKG_BitMatrix encode(String contents,
                       DKG_BarcodeFormat format,
                       int width,
                       int height,
                       Map<DKG_EncodeHintType,?> hints)
      throws DKG_WriterException;

}
