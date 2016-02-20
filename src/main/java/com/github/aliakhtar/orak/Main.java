package com.github.aliakhtar.orak;

import java.util.logging.Logger;

import com.github.aliakhtar.orak.util.Logging;

public class Main
{
    private final Logger log = Logging.get(this);

    public static void main(String[] args)
    {
        new Main();
    }

    public Main()
    {
        log.info("Setup working");
    }
}
