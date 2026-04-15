package com.gussanxz.orgafacil.funcionalidades.vendas.visual.caixa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.vendas.dados.repository.CaixaRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AbrirCaixaActivity extends AppCompatActivity {

    /** Extra retornado quando o caixa é aberto com sucesso. */
    public static final String EXTRA_CAIXA_ID = "caixaId";

    private EditText      etObservacao;
    private CheckBox      cbLancamentoTardio;
    private LinearLayout  btnAbrirCaixa;
    private TextView      txtHoraAtual;
    private boolean       abrindo = false;

    private final CaixaRepository caixaRepository = new CaixaRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ac_vendas_abrir_caixa);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootAbrirCaixa), (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom);
            return insets;
        });

        inicializarComponentes();
    }

    private void inicializarComponentes() {
        ImageButton btnVoltar    = findViewById(R.id.btnVoltarAbrirCaixa);
        etObservacao             = findViewById(R.id.etObservacaoCaixa);
        cbLancamentoTardio       = findViewById(R.id.cbPermiteLancamentoTardio);
        btnAbrirCaixa            = findViewById(R.id.btnAbrirCaixa);
        txtHoraAtual             = findViewById(R.id.txtHoraAberturaAtual);

        if (btnVoltar != null)
            btnVoltar.setOnClickListener(v -> finish());

        if (txtHoraAtual != null) {
            String hora = new SimpleDateFormat("HH:mm 'de' dd/MM/yyyy", new Locale("pt", "BR"))
                    .format(new Date());
            txtHoraAtual.setText("Abertura: " + hora);
        }

        if (btnAbrirCaixa != null)
            btnAbrirCaixa.setOnClickListener(v -> confirmarAbertura());
    }

    private void confirmarAbertura() {
        if (abrindo) return;
        abrindo = true;
        habilitarBotao(false);

        String obs      = etObservacao  != null ? etObservacao.getText().toString().trim() : "";
        boolean tardio  = cbLancamentoTardio != null && cbLancamentoTardio.isChecked();

        caixaRepository.abrirCaixa(obs.isEmpty() ? null : obs, tardio,
                new CaixaRepository.VoidCallback() {
                    @Override
                    public void onSucesso(String caixaId) {
                        Intent result = new Intent();
                        result.putExtra(EXTRA_CAIXA_ID, caixaId);
                        setResult(RESULT_OK, result);
                        finish();
                    }

                    @Override
                    public void onErro(String erro) {
                        abrindo = false;
                        habilitarBotao(true);
                        Toast.makeText(AbrirCaixaActivity.this,
                                "Erro ao abrir caixa: " + erro, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void habilitarBotao(boolean habilitado) {
        if (btnAbrirCaixa != null) {
            btnAbrirCaixa.setEnabled(habilitado);
            btnAbrirCaixa.setAlpha(habilitado ? 1f : 0.6f);
        }
    }
}
