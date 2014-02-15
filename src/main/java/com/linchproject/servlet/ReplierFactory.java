package com.linchproject.servlet;

import com.linchproject.core.Result;
import com.linchproject.core.results.Binary;
import com.linchproject.core.results.Error;
import com.linchproject.core.results.Redirect;
import com.linchproject.core.results.Success;
import com.linchproject.servlet.repliers.BinaryReplier;
import com.linchproject.servlet.repliers.ErrorReplier;
import com.linchproject.servlet.repliers.RedirectReplier;
import com.linchproject.servlet.repliers.SuccessReplier;

/**
 * @author Georg Schmidl
 */
public class ReplierFactory {

    public static Replier getReplier(Result result) {
        if (result instanceof Success) {
            return new SuccessReplier((Success) result);
        } else if (result instanceof Binary) {
            return new BinaryReplier((Binary) result);
        } else if (result instanceof Redirect) {
            return new RedirectReplier((Redirect) result);
        } else if (result instanceof Error) {
            return new ErrorReplier((Error) result);
        } else {
            Error error = new Error();
            error.setMessage("Unknown result: " + result.getClass().getName());
            return new ErrorReplier(error);
        }
    }
}
