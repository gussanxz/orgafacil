package com.gussanxz.orgafacil.util_helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.firebase.ConfiguracaoFirestore;
import com.gussanxz.orgafacil.funcionalidades.main.MainActivity;

public class DialogLogoutHelper {

    public static void mostrarDialogo(Context context) {
        // 1. Cria o BottomSheetDialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);

        // 2. Infla o SEU layout (dialog_sair.xml)
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_logout, null);
        bottomSheetDialog.setContentView(view);

        // Remove o fundo padrão do Android para respeitar suas bordas arredondadas (bg_dialog_top_rounded)
        if (bottomSheetDialog.getWindow() != null) {
            bottomSheetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // 3. Configura os Botões do seu XML
        Button btnConfirmar = view.findViewById(R.id.btnConfirmarSair);
        Button btnCancelar = view.findViewById(R.id.btnCancelarSair);

        // Ação de Confirmar (Sim, Sair agora)
        btnConfirmar.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            executarLogout(context);
        });

        // Ação de Cancelar (Manter conectado)
        btnCancelar.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }

    private static void executarLogout(Context context) {
        // 1. Desloga do Firebase (Auth)
        ConfiguracaoFirestore.getFirebaseAutenticacao().signOut();

        // 2. Desloga do Cliente Google (CRUCIAL para permitir troca de contas)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient googleClient = GoogleSignIn.getClient(context, gso);
        googleClient.signOut().addOnCompleteListener(task -> {
            // Só navega depois de limpar o Google para evitar login automático na conta errada
            irParaIntro(context);
        });
    }

    private static void irParaIntro(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        // Limpa a pilha de atividades para o usuário não poder voltar com o botão "Back"
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }
}