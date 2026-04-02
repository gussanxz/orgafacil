package com.gussanxz.orgafacil.util_helper;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class MascaraMoedaWatcher implements TextWatcher {

    private static final long VALOR_MAXIMO_CENTAVOS = 999_999_999L;
    private final EditText editText;
    private boolean editando = false;
    private long centavos = 0L;

    public MascaraMoedaWatcher(EditText editText) {
        this.editText = editText;
    }

    public void setValorInicial(double valor) {
        centavos = MoedaHelper.doubleParaCentavos(valor);
        aplicarFormatacao();
    }

    public double getValorDouble() {
        return MoedaHelper.centavosParaDouble(centavos);
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (editando) return;

        String apenasDigitos = s.toString().replaceAll("[^0-9]", "");

        long novosCentavos;
        try {
            novosCentavos = apenasDigitos.isEmpty() ? 0L : Long.parseLong(apenasDigitos);
        } catch (NumberFormatException e) {
            novosCentavos = 0L;
        }

        if (novosCentavos > VALOR_MAXIMO_CENTAVOS) novosCentavos = VALOR_MAXIMO_CENTAVOS;

        centavos = novosCentavos;
        aplicarFormatacao();
    }

    private void aplicarFormatacao() {
        editando = true;
        editText.setText(MoedaHelper.formatarCentavosParaBRL(centavos));
        editText.setSelection(editText.getText().length());
        editando = false;
    }
}