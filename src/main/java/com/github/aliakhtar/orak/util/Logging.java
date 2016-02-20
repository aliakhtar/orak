package com.github.aliakhtar.orak.util;

import java.util.logging.Logger;

public class Logging
{
    public static Logger get(Object source)
    {
        return Logger.getLogger(String.valueOf(source));
    }
}
