package com.github.aliakhtar.orak.wikidata;

import com.github.aliakhtar.orak.util.Logging;
import com.github.aliakhtar.orak.util.Util;
import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import static com.github.aliakhtar.orak.util.Util.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;

//import com.github.aliakhtar.orak.wikidata.util.Logging;

public class ClaimParser implements Callable<Optional<JsonObject>>
{
    private final Logger log = Logging.get(this);

    private final JsonObject input;
    private Optional<JsonObject> value = empty();
    public ClaimParser(JsonObject claimInput)
    {
        this.input = claimInput;
    }

    @Override
    public Optional<JsonObject> call() throws Exception
    {
        JsonObject snak = input.getJsonObject("mainsnak");
        if (snak == null)
            return empty();


        Optional<String> valueType = empty();
        try
        {
            valueType = determineValueTypeAndProcessValue(snak);
        }
        catch (Exception e)
        {
            throw new RuntimeException(input.encodePrettily(), e);
        }

        if (! valueType.isPresent() || ! value.isPresent())
        {
            if (! valueType.isPresent())
                log.warning("No value and valueType present: " + snak.encodePrettily());
            return empty();
        }

        String rankStr = input.getString("rank");
        int rank = -1;
        if ("preferred".equals(rankStr))
            rank = 1;
        else if ("normal".equals(rankStr))
            rank = 0;

        JsonObject result = new JsonObject();
        result.put("propertyId", snak.getString("property"))
              .put("valueType", valueType.get())
              .put("value", value.get())
              .put("rank", rank)
        ;

        return Optional.of(result);
    }

    private Optional<String> determineValueTypeAndProcessValue(JsonObject snak)
    {
        String snakType = trimAndDownCase(snak.getString("snaktype"));

        if (isBlank(snakType))
            return of("unknown");

        if ("novalue".equals(snakType))
            return of(  "none" );

        if ("somevalue".equals(snakType))
            return of("unknown");

        if (! snakType.equals("value"))
            return of(snakType);

        String valueType = trimAndDownCase( snak.getString("datatype") );

        if (isBlank(valueType))
            return empty();

        switch (valueType)
        {
            case "monolingualtext":
                value = textualValue(snak, "txt");
                return of( "txt");

            case "string":
                value = textualValue(snak, "str");
                return  of("str");

            case  "url":
                value = textualValue(snak, "url");
                return  of("url");

            case  "external-id":
                value = textualValue(snak, "externalId");
                return of("externalId");

            case "globe-coordinate":
                //value = geoValue(snak);
                return of("geo");

            case "quantity":
                value = qtyType(snak);
                return of("quantity");

            case "time":
                value = dateValue(snak);
                return of("date");

            case "wikibase-item":
                value = relationship(snak, "item");
                return of("item");

            case "wikibase-property":
                value = relationship(snak, "property");
                return of("property");

            case "math":
            case "commonsMedia":
                return empty();

            default:
                return of(valueType);
        }
    }


    private Optional<JsonObject> textualValue(JsonObject snak, String destKey)
    {
        if (destKey.equals("txt") && ! snak.getJsonObject("datavalue").getJsonObject("value").getString("language").equals("en"))
            return empty();

        String value = (! destKey.equals("txt")) ? snak.getJsonObject("datavalue").getString("value")
                                                 : snak.getJsonObject("datavalue").getJsonObject("value").getString("text");

        return of( new JsonObject().put(destKey, value) );
    }

    private Optional<JsonObject> dateValue(JsonObject snak)
    {
        JsonObject val = snak.getJsonObject("datavalue").getJsonObject("value");
        String date = Util.stripPaddedZeroDate( val.getString("time") );

        if (isBlank(date) || date.startsWith("-"))
            return empty();
        int i = date.indexOf("T");
        if (i != -1)
            date = date.substring(0, i);

        String[] parts = date.split("-");
        if (parts[1].equals("00"))
            parts[1] = "01";

        if (parts[2].equals("00"))
            parts[2] = "01";

        date = parts[0] + "-" + parts[1] + "-" + parts[2];

        if (! Util.isValidDate(date))
            return empty();

        return of( new JsonObject().put("date", date));
    }

    private Optional<JsonObject> qtyType(JsonObject snak)
    {
        JsonObject val = snak.getJsonObject("datavalue").getJsonObject("value");
        String amount = val.getString("amount");
        String unit = safe( val.getString("unit") );

        if (isBlank(amount))
            return empty();

        if (amount.startsWith("+") || amount.startsWith("-"))
            amount = amount.substring(1);

        try
        {
            double dblAmt = Double.parseDouble(amount);
            JsonObject qty = new JsonObject().put("amount", dblAmt).put("unit", unit);
            return of( new JsonObject().put("quantity", qty) );
        }
        catch (Exception e)
        {
            log.warning("Failed to parse double in quantity: " + amount + " , " + e.toString());
            return empty();
        }
    }

    private Optional<JsonObject> geoValue(JsonObject snak)
    {
        JsonObject val = snak.getJsonObject("datavalue").getJsonObject("value");
        try
        {
            Double lat = val.getDouble("latitude");
            Double lon = val.getDouble("longitude");

            if (lat == null || lon == null)
                return empty();

            String coords = lat + "," + lon;
            return of(new JsonObject().put("geo", coords));
        }
        catch (Exception e)
        {
            return empty();
        }
    }

    private Optional<JsonObject> relationship(JsonObject snak, String key)
    {
        JsonObject val = snak.getJsonObject("datavalue").getJsonObject("value");
        if (val == null)
            return empty();

        String type = val.getString("entity-type");
        Integer numericId = val.getInteger("numeric-id");

        if (isBlank(type) || numericId == null)
            return empty();

        String prefix = (type.equals("property")) ? "P" : "Q";

        String id = prefix + numericId;

        return of( new JsonObject().put(key, id));
    }
}
