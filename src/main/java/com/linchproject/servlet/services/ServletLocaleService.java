package com.linchproject.servlet.services;

import com.linchproject.http.LocaleService;
import com.linchproject.servlet.ServletThreadLocal;

import java.util.Locale;

/**
 * @author Georg Schmidl
 */
public class ServletLocaleService implements LocaleService {

    @Override
    public Locale getLocale() {
        return ServletThreadLocal.getRequest().getLocale();
    }
}
