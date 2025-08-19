package com.firefly.oshe.lunli.client;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

public class Client {
    private final OkHttpClient client;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final String API_BASE;
    private final ConcurrentHashMap<String, Call> ongoingCalls = new ConcurrentHashMap<>();

    public interface ResultCallback {
        void onSuccess(String content);
        void onFailure(String error);
    }

    public Client(Context context) {
        String token = Token.TOKEN();
        this.API_BASE = Token.DefaultAPI();
        int poolSize = Runtime.getRuntime().availableProcessors() + 1;
        this.executor = Executors.newFixedThreadPool(poolSize);
        this.mainHandler = new Handler(Looper.getMainLooper());

        this.client = new OkHttpClient.Builder()
                .cache(new Cache(context.getCacheDir(), 10 * 1024 * 1024))
                .addInterceptor(new CacheControlInterceptor())
                .addInterceptor(new RetryInterceptor(3))
                .addInterceptor(new AuthInterceptor(token))
                .addInterceptor(new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BASIC))
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private static class CacheControlInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request);

            if (request.method().equals("GET")) {
                CacheControl cacheControl = new CacheControl.Builder()
                        .maxAge(5, TimeUnit.MINUTES)
                        .build();
                
                return response.newBuilder()
                        .header("Cache-Control", cacheControl.toString())
                        .build();
            }
            
            return response;
        }
    }

    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;
        
        public RetryInterceptor(int maxRetries) {
            this.maxRetries = maxRetries;
        }
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException exception = null;
            
            for (int i = 0; i <= maxRetries; i++) {
                try {
                    response = chain.proceed(request);
                    if (response.isSuccessful()) {
                        return response;
                    }
                } catch (IOException e) {
                    exception = e;
                    if (i == maxRetries) {
                        throw exception;
                    }
                }
                
                try {
                    Thread.sleep(1000 * (i + 1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during retry", e);
                }
            }

            return response;
        }
    }

    private static class AuthInterceptor implements Interceptor {
        private final String token;
        
        public AuthInterceptor(String token) {
            this.token = token;
        }
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(request);
        }
    }

    public void uploadData(String path, String fileName, String content, ResultCallback callback) {
        String requestId = "upload-" + System.currentTimeMillis();
        executor.execute(() -> {
            try {
                String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.name())
                        .replace("+", "%20");
                String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                        .replace("+", "%20");
                
                String apiUrl = String.format("%s/%s/%s.json", API_BASE, encodedPath, encodedFileName);

                String contentBase64 = android.util.Base64.encodeToString(
                    content.getBytes(StandardCharsets.UTF_8),
                    android.util.Base64.NO_WRAP
                );

                JSONObject requestBody = new JSONObject();
                requestBody.put("message", "Create " + path + ": " + fileName);
                requestBody.put("content", contentBase64);

                Request request = new Request.Builder()
                    .url(apiUrl)
                    .put(RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json")
                    ))
                    .build();

                try (Response response = executeRequest(request, requestId)) {
                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + response.message());
                    }
                    notifySuccess(callback, "File uploaded successfully");
                }
            } catch (Exception e) {
                notifyFailure(callback, formatError(e));
            } finally {
                ongoingCalls.remove(requestId);
            }
        });
    }

    public void getData(String path, String fileName, ResultCallback callback) {
        String requestId = "get-" + System.currentTimeMillis();
        executor.execute(() -> {
            try {
                String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.name());
                String apiUrl = String.format("%s/%s", API_BASE, encodedPath);
                
                String json = extractResponseContent(apiUrl, requestId);
                JSONArray files = new JSONArray(json);
                String targetFileName = fileName + ".json";

                for (int i = 0; i < files.length(); i++) {
                    JSONObject file = files.getJSONObject(i);
                    if ("file".equals(file.getString("type")) 
                            && targetFileName.equals(file.getString("name"))) {
                        String downloadUrl = file.getString("download_url");
                        String content = extractResponseContent(downloadUrl, requestId);
                        notifySuccess(callback, content);
                        return;
                    }
                }

                notifyFailure(callback, "File not found");
            } catch (Exception e) {
                notifyFailure(callback, formatError(e));
            } finally {
                ongoingCalls.remove(requestId);
            }
        });
    }

    public void getDir(String path, ResultCallback callback) {
        String requestId = "dir-" + System.currentTimeMillis();
        executor.execute(() -> {
            try {
                String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.name());
                String apiUrl = String.format("%s/%s", API_BASE, encodedPath);

                String json = extractResponseContent(apiUrl, requestId);
                notifySuccess(callback, json);
            } catch (Exception e) {
                notifyFailure(callback, formatError(e));
            } finally {
                ongoingCalls.remove(requestId);
            }
        });
    }

    public void getMultipleFiles(String path, List<String> fileNames, ResultCallback callback) {
        String requestId = "multi-get-" + System.currentTimeMillis();
        executor.execute(() -> {
            try {
                String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.name());
                String apiUrl = String.format("%s/%s", API_BASE, encodedPath);
                
                String json = extractResponseContent(apiUrl, requestId);
                JSONArray files = new JSONArray(json);
                
                JSONObject result = new JSONObject();
                for (String fileName : fileNames) {
                    String targetFileName = fileName + ".json";
                    for (int i = 0; i < files.length(); i++) {
                        JSONObject file = files.getJSONObject(i);
                        if ("file".equals(file.getString("type")) 
                                && targetFileName.equals(file.getString("name"))) {
                            String downloadUrl = file.getString("download_url");
                            result.put(fileName, extractResponseContent(downloadUrl, requestId));
                            break;
                        }
                    }
                }
                
                notifySuccess(callback, result.toString());
            } catch (Exception e) {
                notifyFailure(callback, formatError(e));
            } finally {
                ongoingCalls.remove(requestId);
            }
        });
    }

    public void updateData(String path, String fileName, String newContent, ResultCallback callback) {
        String requestId = "update-" + System.currentTimeMillis();
        executor.execute(() -> {
            try {
                String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.name())
                        .replace("+", "%20");
                String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                        .replace("+", "%20");
                
                String apiUrl = String.format("%s/%s/%s.json", API_BASE, encodedPath, encodedFileName);

                Request getRequest = new Request.Builder().url(apiUrl).build();
                String sha;
                try (Response getResponse = executeRequest(getRequest, requestId)) {
                    if (!getResponse.isSuccessful()) {
                        throw new IOException("Failed to get file: HTTP " + getResponse.code());
                    }
                    JSONObject fileInfo = new JSONObject(getResponse.body().string());
                    sha = fileInfo.getString("sha");
                }

                String contentBase64 = android.util.Base64.encodeToString(
                    newContent.getBytes(StandardCharsets.UTF_8),
                    android.util.Base64.NO_WRAP
                );

                JSONObject requestBody = new JSONObject();
                requestBody.put("message", "Update " + path + ": " + fileName);
                requestBody.put("content", contentBase64);
                requestBody.put("sha", sha);

                Request putRequest = new Request.Builder()
                    .url(apiUrl)
                    .put(RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json")
                    ))
                    .build();

                try (Response putResponse = executeRequest(putRequest, requestId)) {
                    if (!putResponse.isSuccessful()) {
                        throw new IOException("Upload failed: HTTP " + putResponse.code());
                    }
                    notifySuccess(callback, "File updated successfully");
                }
            } catch (Exception e) {
                notifyFailure(callback, formatError(e));
            } finally {
                ongoingCalls.remove(requestId);
            }
        });
    }

    public void deleteData(String path, String fileName, ResultCallback callback) {
        String requestId = "delete-" + System.currentTimeMillis();
        executor.execute(() -> {
            try {
                String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.name())
                        .replace("+", "%20");
                String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                        .replace("+", "%20");

                String apiUrl = String.format("%s/%s/%s.json", API_BASE, encodedPath, encodedFileName);

                Request getRequest = new Request.Builder().url(apiUrl).build();
                String sha;
                try (Response getResponse = executeRequest(getRequest, requestId)) {
                    if (!getResponse.isSuccessful()) {
                        throw new IOException("Failed to get file: HTTP " + getResponse.code());
                    }
                    JSONObject fileInfo = new JSONObject(getResponse.body().string());
                    sha = fileInfo.getString("sha");
                }

                String contentBase64 = android.util.Base64.encodeToString(
                        sha.getBytes(StandardCharsets.UTF_8),
                        android.util.Base64.NO_WRAP
                );

                JSONObject requestBody = new JSONObject();
                requestBody.put("message", "Delete " + path + ": " + fileName);
                requestBody.put("content", contentBase64);
                requestBody.put("sha", sha);

                Request putRequest = new Request.Builder()
                        .url(apiUrl)
                        .delete(RequestBody.create(
                                requestBody.toString(),
                                MediaType.parse("application/json")
                        ))
                        .build();

                try (Response putResponse = executeRequest(putRequest, requestId)) {
                    if (!putResponse.isSuccessful()) {
                        throw new IOException("Delete failed: HTTP " + putResponse.code());
                    }
                    notifySuccess(callback, "File deleted successfully");
                }
            } catch (Exception e) {
                notifyFailure(callback, formatError(e));
            } finally {
                ongoingCalls.remove(requestId);
            }
        });
    }

    public void cancelRequest(String requestId) {
        Call call = ongoingCalls.get(requestId);
        if (call != null) {
            call.cancel();
            ongoingCalls.remove(requestId);
        }
    }

    private String extractResponseContent(String apiUrl, String requestId) throws IOException {
        Response response = null;
        try {
            response = executeRequest(new Request.Builder().url(apiUrl).build(), requestId);

            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + response.message());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Response body is null");
            }

            return body.string();
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private Response executeRequest(Request request, String requestId) throws IOException {
        long startTime = System.currentTimeMillis();
        Call call = client.newCall(request);
        ongoingCalls.put(requestId, call);
        
        try {
            Response response = call.execute();
            logRequestPerformance(startTime, request.url().toString(), response.isSuccessful());
            return response;
        } finally {
            ongoingCalls.remove(requestId);
        }
    }

    private void logRequestPerformance(long startTime, String url, boolean success) {
        long duration = System.currentTimeMillis() - startTime;
        Log.d("Client", String.format("Request to %s took %dms, success: %b", 
                url, duration, success));
    }

    private void notifySuccess(ResultCallback callback, String content) {
        mainHandler.post(() -> callback.onSuccess(content));
    }

    private void notifyFailure(ResultCallback callback, String error) {
        mainHandler.post(() -> callback.onFailure(error));
    }

    private String formatError(Exception e) {
        if (e instanceof JSONException) return e.getMessage();
        if (e instanceof IOException) return "Network error: " + e.getMessage();
        return "Error: " + e.getMessage();
    }

    public void shutdown() {
        executor.shutdownNow();
        ongoingCalls.values().forEach(Call::cancel);
        ongoingCalls.clear();
    }
}