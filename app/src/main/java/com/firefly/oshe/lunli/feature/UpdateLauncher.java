package com.firefly.oshe.lunli.feature;

import static com.firefly.oshe.lunli.Tools.Toast;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import androidx.collection.MutableObjectList;

import com.firefly.oshe.lunli.R;
import com.firefly.oshe.lunli.data.NewVersion;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateLauncher {
    private static final String API = "https://api.github.com/repos/Vera-Firefly/OSheLunLi/releases/latest";
    private static final String RELEASE_URL = "github.com/Vera-Firefly/OSheLunLi/releases/download/%s/app-release.apk";
    private static final String CACHE_APK_NAME = "cache.apk";
    private static final String APK_VERSION_FILE_NAME = "apk_version";
    private static final String IGNORE_VERSION_FILE_NAME = "ignore_version";
    private final Context context;
    private final File dir;
    private final int APP_VERSION;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public UpdateLauncher(Context context) {
        this.context = context;
        this.dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Launcher");
        this.APP_VERSION = Integer.parseInt(context.getString(R.string.version_code));
    }

    public void checkForUpdates(boolean ignore) {
        if (!dir.exists() && !dir.mkdirs()) {
            handleException(new IOException("Unable to create directory: " + dir.getAbsolutePath()));
            return;
        }

        executor.execute(() -> {
            JSONObject releaseInfo = null;// fetchReleaseInfo();
            if (releaseInfo != null) handleUpdateCheck(releaseInfo, ignore);
            else showToast("当前版本号：" + APP_VERSION);
        });
    }

    private JSONObject fetchReleaseInfo() {
        UpdateLauncherApi api = new UpdateLauncherApi() { };
        @NotNull List<@NotNull NewVersion> newVersions = api.getInfo(String.valueOf(APP_VERSION));
        LinkedHashMap<String, JSONObject> objectMap = new LinkedHashMap<>();
        newVersions.forEach(it -> {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("tag_name", it.getTag_name())
                        .put("name", it.getName())
                        .put("body", it.getBody())
                        .put("created_at", it.getCreated_at());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            objectMap.put(it.getTag_name(), jsonObject);
        });
        return null;
    }

    private JSONObject fetchNewVersion(LinkedHashMap<String, JSONObject> obj) {
        return null;
    }

    private void handleUpdateCheck(JSONObject releaseInfo, boolean ignore) {
        try {
            int remoteVersionCode = Integer.parseInt(releaseInfo.getString("tag_name").replaceAll("[^\\d]", ""));
            if (remoteVersionCode > APP_VERSION) {
                handleCachedApk(releaseInfo, ignore, false);
            } else {
                // if (!ignore) ShowToast(R.string.settings_updatelauncher_updated, String.valueOf(APP_VERSION));
                handleCachedApk(releaseInfo, ignore, true);
            }
        } catch (IOException | JSONException e) {
            handleException(e);
        }
    }

    private void handleCachedApk(JSONObject releaseInfo, boolean ignore, boolean check) throws JSONException, IOException {
        String tagName = releaseInfo.getString("tag_name");
        String versionName = releaseInfo.getString("name");
        String releaseNotes = releaseInfo.getString("body");
        File apkFile = new File(dir, CACHE_APK_NAME);
        File apkVersionFile = new File(dir, APK_VERSION_FILE_NAME);
        File ignoreVersionFile = new File(dir, IGNORE_VERSION_FILE_NAME);

        if (ignoreVersionFile.exists() && shouldIgnoreVersion(ignoreVersionFile, tagName) && ignore && !check) return;

        if (apkFile.exists() && apkVersionFile.exists() && cachedVersionIsValid(apkVersionFile, tagName) && !check) {
            // new Handler(Looper.getMainLooper()).post(() -> showInstallDialog(apkFile));
        } else {
            deleteFileIfExists(apkFile);
            deleteFileIfExists(apkVersionFile);
            // if (!check) new Handler(Looper.getMainLooper()).post(() -> showUpdateDialog(tagName, versionName, releaseNotes));
        }
    }

    private boolean shouldIgnoreVersion(File ignoreVersionFile, String tagName) throws IOException {
        String savedIgnoreVersion = ""; // Tools.read(ignoreVersionFile);
        int savedIgnoreVersionCode = Integer.parseInt(savedIgnoreVersion.replaceAll("[^\\d]", ""));
        int releaseVersionCode = Integer.parseInt(tagName.replaceAll("[^\\d]", ""));
        if (savedIgnoreVersionCode < releaseVersionCode) deleteFileIfExists(ignoreVersionFile);
        return savedIgnoreVersionCode >= releaseVersionCode;
    }

    private boolean cachedVersionIsValid(File apkVersionFile, String tagName) throws IOException {
        String savedTagName = "";// Tools.read(apkVersionFile);
        int savedVersionCode = Integer.parseInt(savedTagName.replaceAll("[^\\d]", ""));
        int releaseVersionCode = Integer.parseInt(tagName.replaceAll("[^\\d]", ""));
        return savedVersionCode >= releaseVersionCode;
    }

    private void handleException(Exception e) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast(context, "Error: " + e.getMessage())
        );
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast(context, message)
        );
    }

    private void showToast(int messageResId) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast(context, context.getString(messageResId))
        );
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

}
