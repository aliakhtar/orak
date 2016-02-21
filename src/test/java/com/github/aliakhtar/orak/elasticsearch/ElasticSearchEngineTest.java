package com.github.aliakhtar.orak.elasticsearch;

import com.github.aliakhtar.orak.BaseTest;
import io.vertx.core.json.JsonObject;
import org.elasticsearch.action.bulk.BulkResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ElasticSearchEngineTest extends BaseTest
{

    private ElasticSearchEngine es;

    @Before
    public void setUp() throws Exception
    {
        es = new ElasticSearchEngine(env);
    }

    @Test
    public void testPutBulk() throws Exception
    {
        es.dropIndex("test_people");

        List<JsonObject> mockData = new ArrayList<>();
        for (int i =  0; i < 100; i++)
        {
            JsonObject person = new JsonObject();
            person.put("index", i);
            person.put("name", "Person # " + i);
            mockData.add(person);
        }

        BulkResponse resp = es.putBulk("test_people", "fake", mockData);
        assertFalse(resp.buildFailureMessage(), resp.hasFailures());

        assertTrue( es.dropIndex("test_people") );
    }
}