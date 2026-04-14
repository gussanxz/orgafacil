package com.gussanxz.orgafacil.funcionalidades.main;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public class OrgaFacilApp extends Application {

    private static OrgaFacilApp sInstance;

    public static OrgaFacilApp instance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        sInstance = this;
        super.onCreate();
        FirebaseApp.initializeApp(this);

        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
        );

        // Cache offline do Firestore: dados ficam salvos no disco
        // Limite de 50 MB (padrão é 40 MB)
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                                .setSizeBytes(50L * 1024 * 1024)
                                .build()
                )
                .build();
        firestore.setFirestoreSettings(settings);
    }
}