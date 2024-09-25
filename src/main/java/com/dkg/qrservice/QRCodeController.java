package com.dkg.qrservice;

import com.google.zxing.WriterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class QRCodeController {

	@Autowired
	private QRCodeService qrCodeService;

	@GetMapping("/generate-qr")
	public ResponseEntity<byte[]> generateQRCode(
			@RequestParam String text,
			@RequestParam(defaultValue = "200") int width,
			@RequestParam(defaultValue = "200") int height) {
		try {
			byte[] qrCodeImage = qrCodeService.generateQRCodeImage(text, width, height);
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", "image/png");
			return new ResponseEntity<>(qrCodeImage, headers, HttpStatus.OK);
		} catch (WriterException | IOException e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
