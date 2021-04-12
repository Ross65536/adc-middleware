package pt.inesctec.adcauthmiddleware.utils;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pt.inesctec.adcauthmiddleware.http.ClientError;
import pt.inesctec.adcauthmiddleware.http.HttpException;
import pt.inesctec.adcauthmiddleware.http.Json;

public final class SpringUtils {

    /**
     * Get bearer token from user request.
     *
     * @param request the request
     * @return the bearer token
     */
    public static String getBearer(HttpServletRequest request) {
        var auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null) {
            return null;
        }
        auth = auth.trim();

        if (!auth.startsWith("Bearer ")) {
            return null;
        }

        return auth.replace("Bearer ", "");
    }

    /**
     * Remaps HTTP errors to client errors for use in exception handling.
     *
     * @param httpRequest the HTTP request function
     * @return the response stream
     * @throws Exception when request fails
     */
    public static InputStream catchForwardingError(
            ThrowingSupplier<InputStream, Exception> httpRequest) throws Exception {
        try {
            return httpRequest.get();
        } catch (ClientError e) {
            throw e.toHttpException();
        }
    }

    /**
     * Build HTTP exception with status code and body JSON error with status and message.
     *
     * @param status HTTP status code
     * @param msg    the body error message
     * @return the exception
     */
    public static HttpException buildHttpException(HttpStatus status, String msg) {
        var json = errorToJson(status.value(), msg);
        return new HttpException(status.value(), json, Optional.of(MediaType.APPLICATION_JSON_VALUE));
    }

    /**
     * Build spring response with status code and and string body and content type.
     *
     * @param status      HTTP status code
     * @param msg         HTTP body
     * @param contentType content type header.
     * @return the spring response entity
     */
    public static ResponseEntity<String> buildResponse(int status, String msg, String contentType) {
        var headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, contentType);
        var httpStatus = HttpStatus.valueOf(status);

        return new ResponseEntity<>(msg, headers, httpStatus);
    }

    /**
     * Build Spring response with JSON error body with status code and message.
     *
     * @param status status code
     * @param msg    body message.
     * @return the spring response entity.
     */
    public static ResponseEntity<String> buildJsonErrorResponse(HttpStatus status, String msg) {
        return buildJsonErrorResponse(status, msg, ImmutableMap.of());
    }

    /**
     * Build Spring error response JSON with additional header.
     *
     * @param status  status code
     * @param msg     error message
     * @param headers additional headers
     * @return the spring response model
     */
    public static ResponseEntity<String> buildJsonErrorResponse(
            HttpStatus status, String msg, Map<String, String> headers) {
        var responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.forEach(responseHeaders::set);
        var json = errorToJson(status.value(), msg);

        return new ResponseEntity<>(json, responseHeaders, status);
    }

    /**
     * Build spring json stream response.
     *
     * @param streamer stream producer
     * @return stream response
     */
    public static ResponseEntity<StreamingResponseBody> buildJsonStream(StreamingResponseBody streamer) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(streamer);
    }

    /**
     * Build spring json stream response.
     *
     * @param is byte stream
     * @return stream response
     */
    public static ResponseEntity<StreamingResponseBody> buildJsonStream(InputStream is) {
        return SpringUtils.buildJsonStream((OutputStream os) -> IOUtils.copy(is, os));
    }

    /**
     * Build spring TSV stream response.
     *
     * @param streamer stream producer
     * @return stream response
     */
    public static ResponseEntity<StreamingResponseBody> buildTsvStream(
            StreamingResponseBody streamer) {
        return ResponseEntity.ok().header("Content-Type", "text/tab-separated-values").body(streamer);
    }

    /**
     * Build JSON string from status code and message. JSON schema is based on ADC v1 API error codes.
     *
     * @param statusCode HTTP status code
     * @param msg        error message.
     * @return the JSON string.
     */
    private static String errorToJson(int statusCode, String msg) {
        Map<String, Object> error = buildStatusMessage(statusCode, msg);

        try {
            return Json.toJson(error);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create json");
        }
    }

    /**
     * Build error map from status code and error message.
     *
     * @param statusCode status code
     * @param msg        error message.
     * @return map
     */
    public static Map<String, Object> buildStatusMessage(int statusCode, String msg) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", statusCode);
        if (msg != null) {
            error.put("message", msg);
        }
        return error;
    }
}
