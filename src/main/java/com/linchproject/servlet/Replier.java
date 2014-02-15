package com.linchproject.servlet;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Georg Schmidl
 */
public interface Replier {

    void reply(HttpServletResponse response) throws IOException;
}
