package io.zentity.resolution;

import com.fasterxml.jackson.databind.JsonNode;
import io.zentity.common.Json;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.test.ESTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;

public abstract class AbstractITCase extends ESTestCase {
    protected final static int HTTP_TEST_PORT = 9400;
    protected static RestClient client;

    @BeforeClass
    public static void startRestClient() throws IOException {
        client = RestClient.builder(new HttpHost("localhost", HTTP_TEST_PORT)).build();
        try {
            Response response = client.performRequest(new Request("GET", "/"));
            JsonNode json = Json.MAPPER.readTree(response.getEntity().getContent());
            assertEquals("You Know, for Search", json.get("tagline").textValue());
        } catch (IOException e) {
            // If we have an exception here, let's ignore the test
            assumeThat("Integration tests are skipped", e.getMessage(), not(containsString("Connection refused")));
            fail("Something wrong is happening. REST Client seemed to raise an exception.");
            if (client != null) {
                client.close();
                client = null;
            }
        }
    }

    @AfterClass
    public static void stopRestClient() throws IOException {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}
