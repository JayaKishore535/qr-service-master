package com.dkg.qrservice.util;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;


public final class DKG_StringUtils {

  private static final Charset PLATFORM_DEFAULT_ENCODING = Charset.defaultCharset();
  public static final Charset SHIFT_JIS_CHARSET = Charset.forName("SJIS");
  public static final Charset GB2312_CHARSET;
  static {
    Charset gb2312Charset;
    try {
      gb2312Charset = Charset.forName("GB2312");
    } catch (UnsupportedCharsetException ucee) {
      // Can happen on some embedded JREs?
      gb2312Charset = null;
    }
    GB2312_CHARSET = gb2312Charset;
  }
  private static final Charset EUC_JP = Charset.forName("EUC_JP");
  private static final boolean ASSUME_SHIFT_JIS =
      SHIFT_JIS_CHARSET.equals(PLATFORM_DEFAULT_ENCODING) ||
      EUC_JP.equals(PLATFORM_DEFAULT_ENCODING);

  // Retained for ABI compatibility with earlier versions
  public static final String SHIFT_JIS = "SJIS";
  public static final String GB2312 = "GB2312";

  private DKG_StringUtils() { }

  public static String guessEncoding(byte[] bytes, Map<DKG_DecodeHintType,?> hints) {
    Charset c = guessCharset(bytes, hints);
    if (c.equals(SHIFT_JIS_CHARSET)) {
      return "SJIS";
    }
    if (c.equals(StandardCharsets.UTF_8)) {
      return "UTF8";
    }
    if (c.equals(StandardCharsets.ISO_8859_1)) {
      return "ISO8859_1";
    }
    return c.name();
  }

  public static Charset guessCharset(byte[] bytes, Map<DKG_DecodeHintType,?> hints) {
    if (hints != null && hints.containsKey(DKG_DecodeHintType.CHARACTER_SET)) {
      return Charset.forName(hints.get(DKG_DecodeHintType.CHARACTER_SET).toString());
    }

    // First try UTF-16, assuming anything with its BOM is UTF-16
    if (bytes.length > 2 &&
        ((bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) ||
         (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE))) {
      return StandardCharsets.UTF_16;
    }

    // For now, merely tries to distinguish ISO-8859-1, UTF-8 and Shift_JIS,
    // which should be by far the most common encodings.
    int length = bytes.length;
    boolean canBeISO88591 = true;
    boolean canBeShiftJIS = true;
    boolean canBeUTF8 = true;
    int utf8BytesLeft = 0;
    int utf2BytesChars = 0;
    int utf3BytesChars = 0;
    int utf4BytesChars = 0;
    int sjisBytesLeft = 0;
    int sjisKatakanaChars = 0;
    int sjisCurKatakanaWordLength = 0;
    int sjisCurDoubleBytesWordLength = 0;
    int sjisMaxKatakanaWordLength = 0;
    int sjisMaxDoubleBytesWordLength = 0;
    int isoHighOther = 0;

    boolean utf8bom = bytes.length > 3 &&
        bytes[0] == (byte) 0xEF &&
        bytes[1] == (byte) 0xBB &&
        bytes[2] == (byte) 0xBF;

    for (int i = 0;
         i < length && (canBeISO88591 || canBeShiftJIS || canBeUTF8);
         i++) {

      int value = bytes[i] & 0xFF;

      // UTF-8 stuff
      if (canBeUTF8) {
        if (utf8BytesLeft > 0) {
          if ((value & 0x80) == 0) {
            canBeUTF8 = false;
          } else {
            utf8BytesLeft--;
          }
        } else if ((value & 0x80) != 0) {
          if ((value & 0x40) == 0) {
            canBeUTF8 = false;
          } else {
            utf8BytesLeft++;
            if ((value & 0x20) == 0) {
              utf2BytesChars++;
            } else {
              utf8BytesLeft++;
              if ((value & 0x10) == 0) {
                utf3BytesChars++;
              } else {
                utf8BytesLeft++;
                if ((value & 0x08) == 0) {
                  utf4BytesChars++;
                } else {
                  canBeUTF8 = false;
                }
              }
            }
          }
        }
      }

      // ISO-8859-1 stuff
      if (canBeISO88591) {
        if (value > 0x7F && value < 0xA0) {
          canBeISO88591 = false;
        } else if (value > 0x9F && (value < 0xC0 || value == 0xD7 || value == 0xF7)) {
          isoHighOther++;
        }
      }

      // Shift_JIS stuff
      if (canBeShiftJIS) {
        if (sjisBytesLeft > 0) {
          if (value < 0x40 || value == 0x7F || value > 0xFC) {
            canBeShiftJIS = false;
          } else {
            sjisBytesLeft--;
          }
        } else if (value == 0x80 || value == 0xA0 || value > 0xEF) {
          canBeShiftJIS = false;
        } else if (value > 0xA0 && value < 0xE0) {
          sjisKatakanaChars++;
          sjisCurDoubleBytesWordLength = 0;
          sjisCurKatakanaWordLength++;
          if (sjisCurKatakanaWordLength > sjisMaxKatakanaWordLength) {
            sjisMaxKatakanaWordLength = sjisCurKatakanaWordLength;
          }
        } else if (value > 0x7F) {
          sjisBytesLeft++;
          //sjisDoubleBytesChars++;
          sjisCurKatakanaWordLength = 0;
          sjisCurDoubleBytesWordLength++;
          if (sjisCurDoubleBytesWordLength > sjisMaxDoubleBytesWordLength) {
            sjisMaxDoubleBytesWordLength = sjisCurDoubleBytesWordLength;
          }
        } else {
          //sjisLowChars++;
          sjisCurKatakanaWordLength = 0;
          sjisCurDoubleBytesWordLength = 0;
        }
      }
    }

    if (canBeUTF8 && utf8BytesLeft > 0) {
      canBeUTF8 = false;
    }
    if (canBeShiftJIS && sjisBytesLeft > 0) {
      canBeShiftJIS = false;
    }

    // Easy -- if there is BOM or at least 1 valid not-single byte character (and no evidence it can't be UTF-8), done
    if (canBeUTF8 && (utf8bom || utf2BytesChars + utf3BytesChars + utf4BytesChars > 0)) {
      return StandardCharsets.UTF_8;
    }
    // Easy -- if assuming Shift_JIS or >= 3 valid consecutive not-ascii characters (and no evidence it can't be), done
    if (canBeShiftJIS && (ASSUME_SHIFT_JIS || sjisMaxKatakanaWordLength >= 3 || sjisMaxDoubleBytesWordLength >= 3)) {
      return SHIFT_JIS_CHARSET;
    }
    // Distinguishing Shift_JIS and ISO-8859-1 can be a little tough for short words. The crude heuristic is:
    // - If we saw
    //   - only two consecutive katakana chars in the whole text, or
    //   - at least 10% of bytes that could be "upper" not-alphanumeric Latin1,
    // - then we conclude Shift_JIS, else ISO-8859-1
    if (canBeISO88591 && canBeShiftJIS) {
      return (sjisMaxKatakanaWordLength == 2 && sjisKatakanaChars == 2) || isoHighOther * 10 >= length
          ? SHIFT_JIS_CHARSET : StandardCharsets.ISO_8859_1;
    }

    // Otherwise, try in order ISO-8859-1, Shift JIS, UTF-8 and fall back to default platform encoding
    if (canBeISO88591) {
      return StandardCharsets.ISO_8859_1;
    }
    if (canBeShiftJIS) {
      return SHIFT_JIS_CHARSET;
    }
    if (canBeUTF8) {
      return StandardCharsets.UTF_8;
    }
    // Otherwise, we take a wild guess with platform encoding
    return PLATFORM_DEFAULT_ENCODING;
  }

}
