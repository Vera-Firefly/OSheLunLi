package com.firefly.oshe.lunli.feature.Announcement;

import static com.firefly.oshe.lunli.Tools.Toast;
import static com.firefly.oshe.lunli.settings.SettingsKt.*;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.firefly.oshe.lunli.data.Announcement;
import com.firefly.oshe.lunli.ui.dialog.announcement.AnnDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Unit;
import kotlinx.serialization.json.Json;

public class FetchAnn {

    private final Context context;
    private final int ANN_VERSION;
    private final AnnDialog annDialog;
    private final AnnApi annApi = new AnnApi();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public FetchAnn(Context context) {
        this.context = context;
        this.annDialog = new AnnDialog(context);
        this.ANN_VERSION = getSAVED_ANN_VERSION();
    }

    public void checkForAnns() {
        executor.execute(() -> fetchAnnVersion(new AnnsCheckCall() {
            public void onSuccess(JSONObject jsonObject) { handleAnnCheck(jsonObject); }
            public void onFailure(Exception e) { handleException(e); }
        }));
    }

    interface AnnsCheckCall {
        void onSuccess(JSONObject jsonObject);
        void onFailure(Exception e);
    }

    private void fetchAnnVersion(AnnsCheckCall call) {
        annApi.getInfo(String.valueOf(ANN_VERSION), newAnns -> {
            try {
                JSONObject result = new JSONObject();
                JSONArray versionsArray = new JSONArray();

                newAnns.sort((v1, v2) -> v2.getCreated_at().compareTo(v1.getCreated_at()));

                for (Announcement ann : newAnns) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("date", ann.getDate())
                            .put("body", ann.getBody())
                            .put("created_at", ann.getCreated_at());
                    versionsArray.put(jsonObject);
                }

                result.put("versions", versionsArray)
                        .put("count", newAnns.size());

                if (!newAnns.isEmpty()) {
                    Announcement latest = newAnns.get(0);
                    result.put("latest_date", latest.getDate())
                            .put("latest_body", latest.getBody())
                            .put("latest_created_at", latest.getCreated_at());
                }

                call.onSuccess(result);

            } catch (JSONException e) {
                call.onFailure(e);
            }
            return Unit.INSTANCE;
        });
    }

    private void handleAnnCheck(JSONObject info) {
        try {
            int remoteVersionCount = info.getInt("count");
            if (remoteVersionCount != 0) handleAnns(info);
        } catch (JSONException | IOException e) {
            handleException(e);
        }
    }

    private void handleAnns(JSONObject info) throws JSONException, IOException {
        JSONArray versionsArray = info.getJSONArray("versions");
        List<Announcement> allVersions = new ArrayList<>();
        int resultVersion = Integer.parseInt(info.getString("latest_date"));

        for (int i = 0; i < versionsArray.length(); i++) {
            JSONObject versionObj = versionsArray.getJSONObject(i);
            Announcement ann = new Announcement(
                    versionObj.getString("date"),
                    versionObj.getString("body"),
                    versionObj.getString("created_at")
            );
            allVersions.add(ann);
        }

        new Handler(Looper.getMainLooper()).post(() -> showAnnDialog(allVersions, resultVersion));
    }

    private void showAnnDialog(List<Announcement> anns, int resultVersion) {
        annDialog.onAnnDialog(anns, result -> {
            handleAnnResult(result, resultVersion);
            return Unit.INSTANCE;
        });
    }

    private void handleAnnResult(int result, int resultVersion) {
        switch (result) {
            case 0:
                setSAVED_ANN_VERSION(resultVersion);
                break;
            case 1:
                // 暂时不处理
                break;
        }
    }

    private void handleException(Exception e) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast(context, "Error: " + e.getMessage())
        );
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast(context, message));
    }

}
