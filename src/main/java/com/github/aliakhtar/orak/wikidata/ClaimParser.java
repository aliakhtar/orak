package com.github.aliakhtar.orak.wikidata;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
//import com.github.aliakhtar.orak.wikidata.util.Logging;

import com.github.aliakhtar.orak.util.Logging;
import io.vertx.core.json.JsonObject;

import static com.github.aliakhtar.orak.util.Util.isBlank;
import static com.github.aliakhtar.orak.util.Util.trimAndDownCase;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

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


        Optional<String> valueType = determineValueTypeAndProcessValue(snak);

        if (! valueType.isPresent())
            return empty();

        JsonObject result = new JsonObject();
        result.put("propertyId", snak.getString("property"))
                .put("dataType", input.getString("datatype"))
                .put("valueType", determineValueTypeAndProcessValue(snak))
        ;

        return Optional.empty();
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
                value = textualValue(snak, "txt");
                return  of("url");

            case  "external-id":
                value = textualValue(snak, "externalId");
                return of("externalId");

            case "globe-coordinate":
                return of("geo");

            case "quantity":
                return of("quantity");

            case "time":
                return of("date");

            case "math":
            case "commonsMedia":
            case "wikibase-property":
                return empty();

            default:
                return of(valueType);
        }
    }


    private Optional<JsonObject> textualValue(JsonObject snak, String destKey)
    {
        if (destKey.equals("txt") && ! snak.getJsonObject("datavalue").getString("language").equals("en"))
            return empty();

        String value = snak.getJsonObject("datavalue").getString("value");

        return of( new JsonObject().put(destKey, value) );
    }

}
