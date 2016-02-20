package com.github.aliakhtar.orak.scripts;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.github.aliakhtar.orak.util.Logging;
import com.github.aliakhtar.orak.util.io.*;

public class WikiDataJsonReader
{
    private final Logger log = Logging.get(this);

    private final String path;

    public WikiDataJsonReader(String path)
    {
        this.path = path;
    }

    public void run() throws Exception
    {
        File file = new File(path);
        long start = System.currentTimeMillis();
        try(BufferedReader reader = new BufferedReader( new FileReader(file) ))
        {
            int i = 0;
            String line;
            while ( (line = reader.readLine()) != null )
            {
                i++;
                log.info(i + "");
            }
        }
        catch (Exception e) {throw e;}

        long elapsed = System.currentTimeMillis() - start;

        log.info("Elapsed: " + TimeUnit.MILLISECONDS.toMinutes( elapsed ));

    }
}
