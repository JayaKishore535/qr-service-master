package com.dkg.qrservice.controller;

import com.dkg.qrservice.service.QRCodeGenerateService;
import com.dkg.qrservice.util.DKG_WriterException;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
public class QRCodeGenerateController {

    @Autowired
    private QRCodeGenerateService qrCodeGenerateService;

    @GetMapping("/downloadQRCodeImage")
    public ResponseEntity<Resource> downloadQRCode(@RequestParam String inputString,
                                                     @RequestParam int width, @RequestParam int height) throws IOException, DKG_WriterException {

        Map<String, Object> res = qrCodeGenerateService.downloadQRCode(inputString, width, height);
        ByteArrayResource resource = new ByteArrayResource((byte[]) res.get("content"));

        System.out.println(res.get("fileName"));


        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + res.get("fileName"));

        return ResponseEntity.ok().contentType(MediaType
                .APPLICATION_OCTET_STREAM)
                .headers(headers).body(resource);
    }

    @GetMapping("/downloadQRCodeContent")
    public ByteArrayResource downloadQRCodeByte(@RequestParam String inputString,
                                            @RequestParam int width, @RequestParam int height) throws IOException, DKG_WriterException {

        byte[] res = qrCodeGenerateService.getQRCodeImage(inputString, width, height);
        ByteArrayResource resource = new ByteArrayResource(res);


        return resource;
    }

}
