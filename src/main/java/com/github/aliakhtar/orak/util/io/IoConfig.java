package com.github.aliakhtar.orak.util.io;

import java.io.File;
import java.nio.file.FileSystems;

public class IoConfig
{
    private static final String SEPARATOR = FileSystems.getDefault().getSeparator();
    private static final String WORKING_DIR =  readWorkingDir();
    private static final String EOL = System.lineSeparator();


    private static String readWorkingDir()
    {
        String dir = System.getProperty("user.dir");

        //Make sure there's no trailing slash, for consistency:
        if ( dir.endsWith( separator() ))
            dir = dir.substring(0, dir.length() -1 );

        return dir;
    }

    public static String workingDir()
    {
        return WORKING_DIR;
    }

    public static String separator()
    {
        return SEPARATOR;
    }

    public static String eol()
    {
        return EOL;
    }

    public static boolean exists(String path)
    {
        return new File(path).exists();
    }
}
