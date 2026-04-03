package com.gussanxz.orgafacil;

import android.content.Context;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;

import androidx.annotation.NonNull;

@GlideModule
public class GlideConfig extends AppGlideModule {

    private static final int CACHE_DISCO_MB = 200; // 100 MB para imagens em disco
    private static final int CACHE_MEMORIA_MB = 12; // 12 MB em memória RAM

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        builder.setDiskCache(
                new InternalCacheDiskCacheFactory(
                        context,
                        "imagens_catalogo",
                        CACHE_DISCO_MB * 1024 * 1024L
                )
        );
        builder.setMemoryCache(
                new LruResourceCache(CACHE_MEMORIA_MB * 1024 * 1024L)
        );
    }
}