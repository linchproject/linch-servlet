package com.linchproject.servlet.repliers;

import com.linchproject.core.results.Binary;
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
