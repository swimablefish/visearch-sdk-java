package com.visenze.visearch;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.visenze.visearch.internal.http.ViSearchHttpClientImpl;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EncodingUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ViSearchHttpClientTest {
    enum CommandType {GET, POST, POST_IMAGE_01, POST_IMAGE_02, INVALID}
    private String validEndpoint = "http://localhost/";
    private String validAccessKey = "$%&valid_access key-123";
    private String validSecretKey = "validRANDOMsecrete#!34key";
    private String invalidEndpoint = "invalid url";
    private String invalidAccessKey = null;
    private String path = "";
    private Multimap<String, String> params = ArrayListMultimap.create();
    private CloseableHttpClient mockedHttpClient = mock(CloseableHttpClient.class);

    @Test
    public void testValidGetMethod() {
        testValidMethodCalls("get", path, params);
    }

    @Test
    public void testValidPostMethod() {
        testValidMethodCalls("post", path, params);
    }

    @Test
    public void testFirstValidPostImageMethod() {
        testValidMethodCalls("postImage01", path, params, mock(File.class));
    }

    @Test
    public void testSecondValidPostImageMethod() {
        testValidMethodCalls("postImage02", path, params, new byte[10], "test file name String");
    }

    private void testValidMethodCalls(String cmdString, Object... parameters) {
        ViSearchHttpClientImpl client = new ViSearchHttpClientImpl(validEndpoint, validAccessKey, validSecretKey, mockedHttpClient);
        ArgumentCaptor<HttpUriRequest> argument = ArgumentCaptor.forClass(HttpUriRequest.class);

        try {
            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(response.getEntity()).thenReturn(new StringEntity("test"));
            when(mockedHttpClient.execute(argument.capture())).thenReturn(response);

            CommandType cmd = determineCommandType(cmdString);
            switch(cmd) {
                case GET:
                    client.get((String)parameters[0], (Multimap<String, String>)parameters[1]);
                    break;
                case POST:
                    client.post((String)parameters[0], (Multimap<String, String>)parameters[1]);
                    break;
                case POST_IMAGE_01:
                    client.postImage((String)parameters[0], (Multimap<String, String>)parameters[1], (File)parameters[2]);
                    break;
                case POST_IMAGE_02:
                    client.postImage((String)parameters[0], (Multimap<String, String>)parameters[1], (byte[])parameters[2], (String)parameters[3]);
                    break;
                default:
                    assert(false); // should not be executed
            }
        } catch (IOException e) {
            assert(false);  // should not be executed
        }

        HttpUriRequest request = argument.getValue();
        Header[] headerArray = request.getAllHeaders();
        String expected = "Basic " + EncodingUtils.getAsciiString(Base64.encodeBase64(EncodingUtils.getAsciiBytes(validAccessKey + ":" + validSecretKey)));

        boolean isFound = false;
        for (int i=0; i<headerArray.length; i++) {
            if (headerArray[i].getValue().equals(expected)) {
                isFound = true; // found credentials
                break;
            }
        }
        assertTrue(isFound);
    }

    private CommandType determineCommandType(String cmdString) {
        if (cmdString == null) {
            throw new Error("command cannot be null!");
        } else {
            if (cmdString.equalsIgnoreCase("get")) {
                return CommandType.GET;
            } else if (cmdString.equalsIgnoreCase("post")) {
                return CommandType.POST;
            } else if (cmdString.equalsIgnoreCase("postImage01")) {
                return CommandType.POST_IMAGE_01;
            } else if (cmdString.equalsIgnoreCase("postImage02")) {
                return CommandType.POST_IMAGE_02;
            } else {
                return CommandType.INVALID;
            }
        }
    }

    @Test
    public void testInvalidEndPointUsingGetMethod() {
        ViSearchHttpClientImpl client;
        try {
            client = new ViSearchHttpClientImpl(invalidEndpoint, validAccessKey, validSecretKey, mockedHttpClient);
            client.get(path, params);
            assert(false); // should not be executed
        } catch (Exception e) {
            assertTrue(e instanceof ViSearchException);
        }
    }

    @Test
    public void testInvalidEndPointUsingPostMethod() {
        ViSearchHttpClientImpl client;
        try {
            client = new ViSearchHttpClientImpl(invalidEndpoint, validAccessKey, validSecretKey, mockedHttpClient);
            client.post(path, params);
            assert(false); // should not be executed
        } catch (Exception e) {
            assertTrue(e instanceof ViSearchException);
        }
    }

    @Test
    public void testInvalidAccessKeyUsingGetMethod() {
        ViSearchHttpClientImpl client;
        try {
            client = new ViSearchHttpClientImpl(validEndpoint, invalidAccessKey, validSecretKey, mockedHttpClient);
            client.get(path, params);
            assert(false); // should not be executed
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testInvalidParamsUsingPostMethod() {
        ViSearchHttpClientImpl client;
        try {
            client = new ViSearchHttpClientImpl(validEndpoint, validAccessKey, validSecretKey, mockedHttpClient);
            client.post(path, null);
            assert(false); // should not be executed
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
        }
    }

    @Test
    public void testHttpClientThrowsIOExceptionUsingPostMethod() {
        ViSearchHttpClientImpl client;
        try {
            client = new ViSearchHttpClientImpl(validEndpoint, validAccessKey, validSecretKey, mockedHttpClient);
            when(mockedHttpClient.execute(Matchers.<HttpUriRequest>any())).thenThrow(new IOException("test IOException"));
            client.post(path, params);
            assert(false); // should not be executed
        } catch (Exception e) {
            assertTrue(e instanceof NetworkException);
        }
    }

    @Test
    public void testCloseableHttpResponseThrowsIllegalArgumentExceptionUsingPostMethod() {
        ViSearchHttpClientImpl client;
        try {
            client = new ViSearchHttpClientImpl(validEndpoint, validAccessKey, validSecretKey, mockedHttpClient);
            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(mockedHttpClient.execute(Matchers.<HttpUriRequest>any())).thenReturn(response);
            doThrow(new IllegalArgumentException("test IllegalArgumentException")).when(response).getEntity();
            client.post(path, params);
            assert(false); // should not be executed
        } catch (Exception e) {
            assertTrue(e instanceof NetworkException);
        }
    }

    @Test
    public void testInvalidFileUsingFirstPostImageMethod() {
        ViSearchHttpClientImpl client;
        try {
            client = new ViSearchHttpClientImpl(validEndpoint, validAccessKey, validSecretKey, mockedHttpClient);
            client.postImage(path, params, null);
            assert (false); // should not be executed
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testInvalidByteArrayUsingSecondPostImageMethod() {
        ViSearchHttpClientImpl client;
        try {
            client = new ViSearchHttpClientImpl(validEndpoint, validAccessKey, validSecretKey, mockedHttpClient);
            client.postImage(path, params, null, "test file name String");
            assert(false); // should not be executed
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
        }
    }
}
