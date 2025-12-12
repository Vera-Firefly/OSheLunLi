package com.firefly.oshe.lunli.feature.UpdateLauncher;

import static com.firefly.oshe.lunli.Tools.Toast;
import static com.firefly.oshe.lunli.settings.SettingsKt.*;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.FileProvider;

import com.firefly.oshe.lunli.R;
import com.firefly.oshe.lunli.data.NewVersion;
import com.firefly.oshe.lunli.ui.dialog.updateLauncher.DownloadProgressView;
import com.firefly.oshe.lunli.ui.dialog.updateLauncher.UpdateDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateLauncher {
    private static final String API = "https://api.github.com/repos/Vera-Firefly/OSheLunLi/releases/latest";
    private static final String RELEASE_URL = "github.com/Vera-Firefly/OSheLunLi/releases/download/%s/app-release.apk";
    private static final String CACHE_APK_NAME = "cache.apk";
    private final Context context;
    private final File dir;
    private final int APP_VERSION;
    private final UpdateDialog updateDialog;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean isCancelled = false;

    public UpdateLauncher(Context context) {
        this.context = context;
        this.dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Launcher");
        this.APP_VERSION = Integer.parseInt(context.getString(R.string.version_code));
        this.updateDialog = new UpdateDialog();
    }
    public void checkForUpdates(boolean ignore) {
        if (!dir.exists() && !dir.mkdirs()) {
            handleException(new IOException("Unable to create directory: " + dir.getAbsolutePath()));
            return;
        }

        executor.execute(() -> {
            fetchReleaseInfo(new UpdateCheckCallback() {
                public void onSuccess(JSONObject releaseInfo) {
                    handleUpdateCheck(releaseInfo, ignore);
                }

                @Override
                public void onError(Exception e) {
                    handleException(e);
                }
            });
        });
    }

    interface UpdateCheckCallback {
        void onSuccess(JSONObject releaseInfo);
        void onError(Exception e);
    }

    private void fetchReleaseInfo(UpdateCheckCallback callback) {
        UpdateLauncherApi api = new UpdateLauncherApi() {};
        api.getInfo(String.valueOf(APP_VERSION), new Function1<List<NewVersion>, Unit>() {
            @Override
            public Unit invoke(List<NewVersion> newVersions) {
                try {
                    JSONObject result = new JSONObject();
                    JSONArray versionsArray = new JSONArray();

                    newVersions.sort((v1, v2) -> v2.getCreated_at().compareTo(v1.getCreated_at()));

                    for (NewVersion version : newVersions) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("tag_name", version.getTag_name())
                                .put("name", version.getName())
                                .put("body", version.getBody())
                                .put("url", version.getUrl())
                                .put("created_at", version.getCreated_at());
                        versionsArray.put(jsonObject);
                    }

                    result.put("versions", versionsArray);
                    result.put("count", newVersions.size());
                    // result.put("has_update", !newVersions.isEmpty()); 也许后面优化会用到?

                    if (!newVersions.isEmpty()) {
                        NewVersion latest = newVersions.get(0);
                        result.put("latest_tag_name", latest.getTag_name());
                        result.put("latest_name", latest.getName());
                        result.put("latest_body", latest.getBody());
                        result.put("latest_url", latest.getUrl());
                        result.put("latest_created_at", latest.getCreated_at());
                    }

                    callback.onSuccess(result);

                } catch (JSONException e) {
                    callback.onError(e);
                }
                return Unit.INSTANCE;
            }
        });
    }

    private void handleUpdateCheck(JSONObject releaseInfo, boolean ignore) {
        try {
            int remoteVersionCount = releaseInfo.getInt("count");
            if (remoteVersionCount == 0) {
                handleUpdateCheckll(releaseInfo, true, ignore);
            } else {
                handleUpdateCheckll(releaseInfo, false, ignore);
            }
        } catch (JSONException e) {
            handleException(e);
        }
    }

    private void handleUpdateCheckll(JSONObject releaseInfo, boolean latest, boolean ignore) {
        try {
            if (latest) {
                handleCachedApk(releaseInfo, ignore, true);
                showToast("已经是最新版: " + APP_VERSION);
            } else {
                handleCachedApk(releaseInfo, ignore, false);
            }
        } catch (IOException | JSONException e) {
            handleException(e);
        }
    }

    private void handleCachedApk(JSONObject releaseInfo, boolean ignore, boolean check) throws JSONException, IOException {
        File apkFile = new File(dir, CACHE_APK_NAME);
        if (check) {
            if (apkFile.exists()) deleteFileIfExists(apkFile);
            setCACHED_APP_VERSION(0);
            setSAVED_IGNORE_APP_VERSION(0);
            return;
        }

        String tagName = releaseInfo.getString("latest_tag_name");
        String versionName = releaseInfo.getString("latest_name");
        String releaseNotes = releaseInfo.getString("latest_body");
        String releaseUrl = releaseInfo.getString("latest_url");

        int remoteVersion = Integer.parseInt(tagName);
        int cachedVersion = getCACHED_APP_VERSION();
        int ignoreVersion = getSAVED_IGNORE_APP_VERSION();

        if (ignoreVersion != 0 && ignoreVersion >= remoteVersion && ignore) return;

        if (apkFile.exists() && cachedVersion != 0 && cachedVersion >= remoteVersion) {
            new Handler(Looper.getMainLooper()).post(() -> showInstallDialog(apkFile));
        } else {
            deleteFileIfExists(apkFile);
            new Handler(Looper.getMainLooper()).post(() -> showUpdateDialog(tagName, versionName, releaseNotes, releaseUrl));
        }
    }

    private void showUpdateDialog(String tagName, String versionName, String releaseNotes, String url) {

    }

    private void startDownload(String apkUrl, String tagName) {
        isCancelled = false;
        DownloadProgressView progressView = new DownloadProgressView(context);
        progressView.setTitle("下载中...");

        progressView.setOnDownloadListener(new DownloadProgressView.OnDownloadListener() {
            @Override
            public void onCancel() {
                isCancelled = true;
            }

            @Override
            public void onProgress(int progress) {
            }
        });

        executor.execute(() -> downloadApk(apkUrl, tagName, progressView));
    }

    private void updateProgressDialog(ProgressDialog progressDialog, int progress) {
        new Handler(Looper.getMainLooper()).post(() -> {
            progressDialog.setProgress(progress);
        });
    }

    private void downloadApk(String apkUrl, String tagName, DownloadProgressView progressView) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(apkUrl).build();
        File apkFile = new File(dir, CACHE_APK_NAME);

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                long totalBytes = response.body().contentLength();
                byte[] buffer = new byte[8192];
                long downloadedBytes = 0;

                try (InputStream inputStream = response.body().byteStream();
                     FileOutputStream outputStream = new FileOutputStream(apkFile)) {

                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        if (isCancelled) {
                            outputStream.close();
                            apkFile.delete();
                            return;
                        }

                        outputStream.write(buffer, 0, read);
                        downloadedBytes += read;

                        int progress = (int) (100 * downloadedBytes / totalBytes);
                        progressView.setProgress(progress);
                    }
                }


                new Handler(Looper.getMainLooper()).post(() -> showDownloadCompleteDialog(apkFile));
            } else {
                showToast("下载失败...");
            }
        } catch (IOException e) {
            handleException(e);
        } finally {
            setCACHED_APP_VERSION(Integer.parseInt(tagName));
        }
    }

    private void showDownloadCompleteDialog(File apkFile) {

    }

    private void showInstallDialog(File apkFile) {
        updateDialog.InstallDialog(context, apkFile.getAbsolutePath(), call -> {
            handleInstallResult(call, apkFile);
            return Unit.INSTANCE;
        });
    }

    private void handleInstallResult(int result, File  apkFile) {
        switch (result) {
            case 0:
                showToast("用户取消安装");
                break;
            case 1:
                reDownloadApk();
                break;
            case 2:
                installApk(apkFile);
                break;
        }
    }

    private void reDownloadApk() {
        setSAVED_IGNORE_APP_VERSION(0);
        setCACHED_APP_VERSION(0);
        cleanupFile(dir);
        checkForUpdates(true);
    }

    private void installApk(File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", apkFile);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }

    private void handleException(Exception e) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast(context, "Error: " + e.getMessage())
        );
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast(context, message));
    }

    private void showToast(int messageResId) {
        new Handler(Looper.getMainLooper()).post(() -> Toast(context, context.getString(messageResId)));
    }

    private void showToast(int messageResId, Object... formatArgs) {
        new Handler(Looper.getMainLooper()).post(() -> {
            String message = context.getString(messageResId, formatArgs);
            Toast(context, message);
        });
    }

    private void deleteFileIfExists(File file) {
        if (file.exists()) file.delete();
    }

    public static void cleanupFile(File... files) {
        for (File file : files) {
            if (file != null && file.exists()) {
                deleteRecursively(file);
            }
        }
    }

    private static boolean deleteRecursively(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        return fileOrDir.delete();
    }

}
