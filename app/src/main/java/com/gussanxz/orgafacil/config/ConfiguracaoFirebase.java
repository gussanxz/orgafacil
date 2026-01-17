package com.gussanxz.orgafacil.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

public class ConfiguracaoFirebase {
    private static FirebaseFirestore firestore;

    public static FirebaseFirestore getFirestore() {
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        return firestore;
    }

    private static FirebaseAuth autenticacao;
    private static DatabaseReference firebase;

    //retornar a instancia do FirebaseDatabase
    public static DatabaseReference getFirebaseDatabase(){
        if ( firebase == null ) {
            firebase = FirebaseDatabase.getInstance().getReference();
        }
        return firebase;
    }

    //retornar a instancia do FirebaseAuth
    public static FirebaseAuth getFirebaseAutenticacao() {
        if( autenticacao == null) {
            autenticacao = FirebaseAuth.getInstance();
        }
        return autenticacao;
    }

}
