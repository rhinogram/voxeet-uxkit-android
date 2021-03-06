package com.voxeet.uxkit.youtube.manifests;

import android.content.Context;
import android.support.annotation.NonNull;

import com.voxeet.sdk.manifests.AbstractManifestComponentProvider;
import com.voxeet.uxkit.presentation.controller.MediaPlayerProviderController;
import com.voxeet.uxkit.youtube.YoutubeMediaPresentationProvider;

public final class YoutubeMediaPresentationProviderManifestComponent extends AbstractManifestComponentProvider {

    @Override
    protected void init(@NonNull Context context) {
        MediaPlayerProviderController.register(new YoutubeMediaPresentationProvider());
    }

    @Override
    protected String getComponentName() {
        return YoutubeMediaPresentationProviderManifestComponent.class.getSimpleName();
    }

    @Override
    protected String getDefaultAuthority() {
        return "com.voxeet.uxkit.youtube.manifests.";
    }
}
