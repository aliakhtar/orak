package com.github.aliakhtar.orak.util;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class Util
{
    private static final Random RAND = new Random();
    private static final Pattern ALPHABETS = Pattern.compile("[a-z]+", CASE_INSENSITIVE);

    private static final Logger log = Logging.get(Util.class);

    private static final Pattern DATE_PADDED_ZEROES = Pattern.compile("\\+[0]+");

    public static void sleep(long millis)
    {
        log.info("Sleeping for : " + millis);
        try
        {
            Thread.sleep(millis);
            log.info("Woken up after sleeping for : " + millis);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public static long microTime()
    {
        return TimeUnit.NANOSECONDS.toMicros( System.nanoTime() );
    }


    public static BufferedImage base64StringToImage(String base64Str)
    {
        try
        {
            return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64Str)));
        }
        catch (final IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public static void writeToFile(String path, BufferedImage img)
    {
        try
        {
            File output = new File(path);
            ImageIO.write(img, "PNG", output);
            log.info("Written " + output.getAbsolutePath());
        }
        catch (Exception e) {throw new RuntimeException(e);}
    }

    public static int reverse(int number)
    {
        number = -number;
        return number;
    }

    public static boolean hasLetters(String str)
    {
        return (str != null && ALPHABETS.matcher(str).find());
    }


    public static <T> T random(T[] items)
    {
        return items [ RAND.nextInt( items.length ) ];
    }

    public static long random(long start, long end)
    {
        return RAND.longs(start, end).findFirst().getAsLong();
    }

    public static String safe(String str)
    {
        return  (str != null) ? str : "";
    }

    public static String safe(JsonArray data, int pos)
    {
        if (data.size() < pos)
            return " ? ";
        try
        {
            String result = data.getString(pos);
            return (result != null) ? result : "";
        }
        catch (ClassCastException e)
        {
            Object value = data.getValue(pos);
            return String.valueOf(value);
        }
    }

    public static int safeInt(JsonArray data, int pos)
    {
        if (data.size() < pos)
            return -1;
        try
        {
            Integer result = data.getInteger(pos);
            return (result != null) ? result : -2;
        }
        catch (ClassCastException e)
        {
            return -3;
        }
    }

    public static int safeInt(String str)
    {
        if (str == null)
            return -1;
        try
        {
            return Integer.parseInt( trim(str) );
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }


    public static String safe(JsonObject data, String key)
    {
        if (! data.containsKey(key))
            return " ? ";

        try
        {
            String result = data.getString(key);
            return (result != null) ? result : "--";
        }
        catch (ClassCastException e)
        {
            Object value = data.getMap().get(key);
            return String.valueOf(value);
        }
    }

    public static int safeInt(JsonObject data, String key)
    {
        if (! data.containsKey(key))
            return -1;

        try
        {
            Integer result = data.getInteger(key);
            return (result != null) ? result : -1;
        }
        catch (ClassCastException e)
        {
            return -3;
        }
    }

    public static double safeDouble(JsonObject data, String key)
    {
        if (! data.containsKey(key))
            return -1;

        try
        {
            Double result = data.getDouble(key);
            return (result != null) ? result : -1;
        }
        catch (ClassCastException e)
        {
            return -3;
        }
    }


    public static boolean isBlank(String str)
    {
        return (str == null || str.trim().isEmpty());
    }

    public static String trimAndDownCase(String str)
    {
        if (str != null)
            str = str.trim().toLowerCase();
        return str;
    }

    public static String trim(String str)
    {
        if (str != null)
            str = str.trim();

        return str;
    }

    public static boolean isTrimmed(String str)
    {
        return (! str.startsWith(" ") && ! str.endsWith(" "));
    }

    public static String parseBeforeSpace(String str)
    {
        int spaceIndex = str.indexOf(" ");
        if (spaceIndex != -1)
            str = str.substring(0, spaceIndex);

        return trim( str );
    }


    public static boolean isInt(String str)
    {
        try
        {
            return Integer.parseInt(str) >= 0;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    public static <T> void addTask(Callable<T> task, Consumer<T> cb, ErrorHandler handler, ListeningExecutorService executor)
    {
        ListenableFuture<T> future = executor.submit(task);

        Futures.addCallback(future, new FutureCallback<T>()
        {
            @Override
            public void onSuccess(T result)
            {
                cb.accept(result);
            }

            @Override
            public void onFailure(Throwable t)
            {
                handler.accept(t);
            }
        });
    }


    public static String stripPaddedZeroDate(String input)
    {
        input = trim(input);
        Matcher matcher = DATE_PADDED_ZEROES.matcher(input);
        if (! matcher.find())
            return input;

        return "+" + matcher.replaceFirst("");
    }
}
