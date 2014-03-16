package com.linchproject.servlet.repliers;

import com.linchproject.core.results.Binary;
import com.linchproject.servlet.Environment;
import com.linchproject.servlet.Replier;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Georg Schmidl
 */
public class BinaryReplier implements Replier {

    private Binary binary;

    public BinaryReplier(Binary binary) {
        this.binary = binary;
    }

    @Override
    public void reply(HttpServletResponse response) throws IOException {
        if (Environment.DEV) {
            response.setHeader("Cache-Control", "no-cache, no-store");
        } else {
            response.setHeader("Cache-Control", "public; max-age=3600");
            response.setDateHeader("Expires", System.currentTimeMillis() + 3600 * 1000);
        }

        InputStream inputStream = binary.getInputStream();
        OutputStream outputStream = response.getOutputStream();

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.close();
        inputStream.close();
    }
}
