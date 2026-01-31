package com.gussanxz.orgafacil.funcionalidades.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ConfiguracaoFirestore {

    // Instância estática para garantir que só exista uma conexão ativa (Singleton)
    private static FirebaseFirestore firestore;
    private static FirebaseAuth autenticacao;

    // Metodo para obter a instância do Firestore e acessar o banco de dados
    public static FirebaseFirestore getFirestore() {
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        return firestore;
    }

    //Metodo para gerenciar login/cadastro de usuários e retornar a instancia do FirebaseAuth
    public static FirebaseAuth getFirebaseAutenticacao() {
        if( autenticacao == null) {
            autenticacao = FirebaseAuth.getInstance();
        }
        return autenticacao;
    }

}
