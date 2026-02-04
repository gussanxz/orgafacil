package com.gussanxz.orgafacil.util_helper;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

public class SenhaForcaHelper {

    private final View v1, v2, v3, v4;
    private final TextView textDicaSenha;

    // Cores (Hardcoded para simplificar, ou você pode buscar do resources se passar context)
    private final int COR_NEUTRA = Color.parseColor("#DDDDDD");
    private final int COR_FRACA = Color.RED;
    private final int COR_MEDIA = Color.parseColor("#FF9800"); // Laranja
    private final int COR_BOA = Color.parseColor("#FBC02D");   // Amarelo
    private final int COR_FORTE = Color.parseColor("#4CAF50"); // Verde

    public SenhaForcaHelper(View v1, View v2, View v3, View v4, TextView textDicaSenha) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.v4 = v4;
        this.textDicaSenha = textDicaSenha;
    }

    /**
     * Calcula o nível de 0 a 4.
     * Útil para validações lógicas fora da UI (ex: habilitar botão).
     */
    public int calcularNivel(String senha) {
        int nivel = 0;
        if (senha.length() >= 6) nivel++;
        if (senha.matches(".*[A-Z].*")) nivel++;
        if (senha.matches(".*[0-9].*")) nivel++;
        if (senha.matches(".*[@#$%^&+=!].*")) nivel++;
        return nivel;
    }

    public boolean ehSegura(String senha) {
        return calcularNivel(senha) >= 3;
    }

    /**
     * Atualiza as cores das barras e o texto da dica.
     */
    public void atualizarVisual(String senha) {
        int nivel = calcularNivel(senha);
        boolean vazia = senha.isEmpty();

        // Reseta tudo para neutro
        v1.setBackgroundColor(COR_NEUTRA);
        v2.setBackgroundColor(COR_NEUTRA);
        v3.setBackgroundColor(COR_NEUTRA);
        v4.setBackgroundColor(COR_NEUTRA);

        if (vazia) {
            textDicaSenha.setVisibility(View.GONE);
            return;
        }

        textDicaSenha.setVisibility(View.VISIBLE);

        if (nivel >= 1) {
            v1.setBackgroundColor(COR_FRACA);
            configurarTexto("Muito fraca", COR_FRACA);
        }
        if (nivel >= 2) {
            v1.setBackgroundColor(COR_MEDIA);
            v2.setBackgroundColor(COR_MEDIA);
            configurarTexto("Fraca", COR_MEDIA);
        }
        if (nivel >= 3) {
            v1.setBackgroundColor(COR_BOA);
            v2.setBackgroundColor(COR_BOA);
            v3.setBackgroundColor(COR_BOA);
            configurarTexto("Boa", COR_BOA);
        }
        if (nivel >= 4) {
            v1.setBackgroundColor(COR_FORTE);
            v2.setBackgroundColor(COR_FORTE);
            v3.setBackgroundColor(COR_FORTE);
            v4.setBackgroundColor(COR_FORTE);
            configurarTexto("Forte", COR_FORTE);
        }
    }

    private void configurarTexto(String texto, int cor) {
        textDicaSenha.setText(texto);
        textDicaSenha.setTextColor(cor);
    }
}