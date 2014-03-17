package com.linchproject.servlet.repliers;

import com.linchproject.core.results.Binary;
import com.linchproject.servlet.Environment;
import com.linchproject.servlet.Replier;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
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
    public void reply(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) throws IOException {
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");

        if (!Environment.DEV && ifModifiedSince > -1 && binary.getLastModified() != null
                && binary.getLastModified().getTime() / 1000 <= ifModifiedSince / 1000) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);

        } else {
            response.setHeader("Cache-Control", (binary.isPublic()?"public":"private") +"; max-age=31536000");
            response.setDateHeader("Expires", System.currentTimeMillis() + 31536000 * 1000);

            if (binary.getLastModified() != null) {
                response.setDateHeader("Last-Modified", binary.getLastModified().getTime());
            }

            if (binary.getContentType() != null) {
                response.setContentType(binary.getContentType());
            } else if (binary.getFileName() != null) {
                response.setContentType(servletContext.getMimeType(binary.getFileName()));
            }

            if (binary.getLength() > -1) {
                response.setContentLength(binary.getLength());
            } else {
                response.setContentLength(binary.getInputStream().available());
            }

            if (binary.isAttachment() && binary.getFileName() != null) {
                response.setHeader("Content-Disposition", "attachment; filename=" + binary.getFileName());
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
}
