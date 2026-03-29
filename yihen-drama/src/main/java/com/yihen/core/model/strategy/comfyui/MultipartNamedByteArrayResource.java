package com.yihen.core.model.strategy.comfyui;

import org.springframework.core.io.ByteArrayResource;

public class MultipartNamedByteArrayResource extends ByteArrayResource {

    private final String filename;

    private MultipartNamedByteArrayResource(String filename, byte[] byteArray) {
        super(byteArray);
        this.filename = filename;
    }

    public static MultipartNamedByteArrayResource of(String filename, byte[] byteArray) {
        return new MultipartNamedByteArrayResource(filename, byteArray);
    }

    @Override
    public String getFilename() {
        return filename;
    }
}
