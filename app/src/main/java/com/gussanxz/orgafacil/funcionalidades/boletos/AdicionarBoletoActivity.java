package com.gussanxz.orgafacil.funcionalidades.boletos;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import java.util.EnumMap;
import java.util.Map;
import com.gussanxz.orgafacil.R;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdicionarBoletoActivity extends AppCompatActivity {

    private static final String TAG = "AdicionarBoletoActivity";
    private static final int REQ_CAMERA = 101;

    private TextInputEditText campoDescricao, campoCodigoBarras, campoValor, campoDataVencimento;
    private TextView textoArquivoAnexado;
    private Calendar dataSelecionada = Calendar.getInstance();
    private BoletoRepository repository = new BoletoRepository();
    private boolean formatandoCodigo = false;

    // Launcher para câmera (foto)
    private final ActivityResultLauncher<Intent> launcherCamera =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String codigo = result.getData().getStringExtra(EscanearBoletoActivity.EXTRA_CODIGO);
                    if (codigo != null && !codigo.isEmpty()) {
                        // Remove não-dígitos antes de setar (ZXing ITF pode retornar espaços em alguns devices)
                        String codigoLimpo = codigo.replaceAll("[^0-9]", "");
                        campoCodigoBarras.setText(codigoLimpo);
                        extrairInfosDoCodigo(codigoLimpo);
                    }
                }
            });


    // Launcher para seleção de PDF/imagem da galeria/arquivos
    private final ActivityResultLauncher<Intent> launcherArquivo =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) processarArquivo(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adicionar_boleto);

        campoDescricao      = findViewById(R.id.campoDescricao);
        campoCodigoBarras   = findViewById(R.id.campoCodigoBarras);
        campoValor          = findViewById(R.id.campoValor);
        campoDataVencimento = findViewById(R.id.campoDataVencimento);
        textoArquivoAnexado = findViewById(R.id.textoArquivoAnexado);

        campoDataVencimento.setOnClickListener(v -> abrirDatePicker());

        campoCodigoBarras.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (formatandoCodigo) return;
                formatandoCodigo = true;
                String apenasNumeros = s.toString().replaceAll("[^0-9]", "");
                String formatado = formatarCodigoBarras(apenasNumeros);
                campoCodigoBarras.setText(formatado);
                campoCodigoBarras.setSelection(formatado.length());
                formatandoCodigo = false;
            }
        });

        findViewById(R.id.btnEscanearCodigo).setOnClickListener(v -> abrirCamera());
        findViewById(R.id.btnAnexarPdf).setOnClickListener(v -> abrirSeletorArquivo());
        findViewById(R.id.btnSalvar).setOnClickListener(v -> salvarBoleto());
    }

    // ── Câmera ───────────────────────────────────────────────────────────────

    private void abrirCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
            return;
        }
        launcherCamera.launch(new Intent(this, EscanearBoletoActivity.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            abrirCamera();
        } else {
            Toast.makeText(this, "Permissão de câmera negada.", Toast.LENGTH_SHORT).show();
        }
    }

    // ── ML Kit — leitura do código de barras ─────────────────────────────────

    private void processarImagemComZxing(Bitmap bitmap) {
        new Thread(() -> {
            try {
                int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
                bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0,
                        bitmap.getWidth(), bitmap.getHeight());

                RGBLuminanceSource source = new RGBLuminanceSource(
                        bitmap.getWidth(), bitmap.getHeight(), pixels);
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

                Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
                hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                MultiFormatReader reader = new MultiFormatReader();
                reader.setHints(hints);

                Result result = reader.decode(binaryBitmap);
                String raw = result.getText();

                runOnUiThread(() -> {
                    campoCodigoBarras.setText(raw);
                    extrairInfosDoCodigo(raw);
                });

            } catch (NotFoundException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Nenhum código encontrado na imagem.",
                                Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Erro ao ler código: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    // ── Seletor de arquivo ───────────────────────────────────────────────────

    private void abrirSeletorArquivo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"application/pdf", "image/jpeg", "image/png"});
        launcherArquivo.launch(intent);
    }

    private void processarArquivo(Uri uri) {
        String tipo = getContentResolver().getType(uri);
        textoArquivoAnexado.setText("Arquivo anexado: " + uri.getLastPathSegment());

        if (tipo != null && tipo.equals("application/pdf")) {
            processarPdf(uri);
        } else {
            // imagem
            try {
                Bitmap bmp = android.provider.MediaStore.Images.Media
                        .getBitmap(getContentResolver(), uri);
                processarImagemComZxing(bmp);
            } catch (IOException e) {
                Toast.makeText(this, "Erro ao abrir imagem.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processarPdf(Uri uri) {
        try {
            ParcelFileDescriptor pfd = getContentResolver()
                    .openFileDescriptor(uri, "r");
            if (pfd == null) return;
            PdfRenderer renderer = new PdfRenderer(pfd);
            // Renderiza a primeira página e aplica ML Kit
            PdfRenderer.Page page = renderer.openPage(0);
            Bitmap bmp = Bitmap.createBitmap(page.getWidth(), page.getHeight(),
                    Bitmap.Config.ARGB_8888);
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
            renderer.close();
            pfd.close();
            processarImagemComZxing(bmp);
        } catch (IOException e) {
            Toast.makeText(this, "Erro ao processar PDF.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Erro PDF", e);
        }
    }

    // ── Extração de dados do código de barras ────────────────────────────────

    private void extrairInfosDoCodigo(String entrada) {
        try {
            String limpo = entrada.replaceAll("[^0-9]", "");
            long centavos = 0;

            if (limpo.length() == 44 && !limpo.startsWith("8")) {
                // ── ITF-44 bancário ──────────────────────────────────────────────
                // Estrutura: Banco(3)+Moeda(1)+DV(1)+FatorVenc(4)+Valor(10)+CL(25)
                // Valor: posições 5 a 14 (índice 0)
                centavos = Long.parseLong(limpo.substring(5, 15));

            } else if (limpo.length() == 44 && limpo.startsWith("8")) {
                // ── ITF-44 tributos/concessionárias ─────────────────────────────
                // Estrutura: Produto(1)+Segmento(1)+RealEfetivo(1)+DV(1)+Valor(11)+...
                // Valor: posições 4 a 14
                centavos = Long.parseLong(limpo.substring(4, 15));

            } else if (limpo.length() == 47) {
                // ── Linha digitável bancária (47 dígitos sem fmt) ────────────────
                // Campo5 começa na posição 33: fator(4)+valor(10)
                // Valor: posições 37 a 46
                centavos = Long.parseLong(limpo.substring(37, 47));

            } else if (limpo.length() == 48) {
                // ── Linha digitável tributos/concessionárias (48 dígitos) ────────
                // Estrutura: Campo1(10)+Campo2(11)+Campo3(11)+Campo4(11)+DV(1)+Valor(14)
                // Valor está nos últimos 14 caracteres: fatorData(6) + valor(8) para alguns
                // ou nos caracteres 5 a 15 dependendo do segmento.
                // Forma segura: bloco de valor está nas posições 4–14 do código de barras
                // Converte linha digitável tributos para barras primeiro:
                String p1 = limpo.substring(0, 9);  // sem dv do campo1
                String p2 = limpo.substring(10, 20); // sem dv do campo2
                String p3 = limpo.substring(21, 31); // sem dv do campo3
                // campo4 = dv geral em 32; campo5 = data+valor em 33-46
                String codigoBarras48 = p1 + p2 + p3 + limpo.substring(32, 48);
                if (codigoBarras48.startsWith("8")) {
                    centavos = Long.parseLong(codigoBarras48.substring(4, 15));
                }
            }

            if (centavos > 0) {
                double reais = centavos / 100.0;
                campoValor.setText(String.format(Locale.getDefault(), "%.2f", reais));
            } else {
                Log.d(TAG, "Valor zero. len=" + limpo.length()
                        + " inicio=" + (limpo.isEmpty() ? "?" : limpo.charAt(0)));
            }
        } catch (Exception e) {
            Log.w(TAG, "Erro ao extrair valor: " + entrada, e);
        }
    }

    /**
     * Converte a linha digitável bancária (47 dígitos) para o código de barras ITF-44.
     *
     * Linha digitável: AAAA9.AAAAA BBBBB.BBBBBB CCCCC.CCCCCC K EEEEEEEEEEEE
     * Agrupando sem formatação (47 dígitos):
     *   Campo1 = posições 0-9   (banco+moeda+campo livre início + dígito)
     *   Campo2 = posições 10-20 (campo livre meio + dígito)
     *   Campo3 = posições 21-31 (campo livre fim + dígito)
     *   Campo4 = posição 32     (dígito verificador do código de barras)
     *   Campo5 = posições 33-46 (fator vencimento + valor)
     *
     * Código de barras (44 dígitos):
     *   Banco(3) + Moeda(1) + DígitoVerificador(1) + FatorVenc(4) + Valor(10) + CampoLivre(25)
     */
    private String converterLinhaDigitavelParaCodigoBarras(String ld) {
        // ld já está sem pontos/espaços, com 47 dígitos
        if (ld.length() != 47) return ld;

        // Extrai os 5 campos da linha digitável
        String campo1 = ld.substring(0, 10);   // banco+moeda+inicio campo livre (9) + digito (1)
        String campo2 = ld.substring(10, 21);  // campo livre meio (10) + digito (1)
        String campo3 = ld.substring(21, 32);  // campo livre fim (10) + digito (1)
        String campo4 = ld.substring(32, 33);  // dígito verificador do código de barras
        String campo5 = ld.substring(33, 47);  // fator vencimento (4) + valor (10)

        // Remove os dígitos verificadores dos campos (último dígito de cada campo)
        String parteA = campo1.substring(0, 9);  // banco(3) + moeda(1) + campo livre início(5)
        String parteB = campo2.substring(0, 10); // campo livre meio
        String parteC = campo3.substring(0, 10); // campo livre fim

        // Monta o campo livre completo (25 dígitos)
        String campoLivre = parteA.substring(4) + parteB + parteC; // 5 + 10 + 10 = 25

        // Código de barras = banco(3) + moeda(1) + dígVerif(1) + fatorVenc+valor(14) + campoLivre(25)
        String banco = parteA.substring(0, 3);
        String moeda = parteA.substring(3, 4);

        return banco + moeda + campo4 + campo5 + campoLivre;
    }

    /**
     * Converte ITF-44 bancário (44 dígitos) para linha digitável (47 dígitos).
     * Inverso de converterLinhaDigitavelParaCodigoBarras().
     *
     * ITF-44: banco(3)+moeda(1)+DV(1)+fatorVenc(4)+valor(10)+campoLivre(25) = 44
     * LD47:   campo1(10)+campo2(11)+campo3(11)+campo4(1)+campo5(14)         = 47
     */
    private String converterItf44ParaLinhaDigitavel(String itf) {
        if (itf.length() != 44) return null;

        String banco      = itf.substring(0, 3);
        String moeda      = itf.substring(3, 4);
        String dvBarras   = itf.substring(4, 5);
        String fatorValor = itf.substring(5, 19); // fator(4)+valor(10) = 14 dígitos
        String cl         = itf.substring(19);    // campo livre = 25 dígitos

        // Monta os 3 campos sem DV
        // Campo1: banco(3) + moeda(1) + cl[0:5](5) = 9 dígitos
        String c1sem = banco + moeda + cl.substring(0, 5);
        // Campo2: cl[5:15](10) = 10 dígitos
        String c2sem = cl.substring(5, 15);
        // Campo3: cl[15:25](10) = 10 dígitos
        String c3sem = cl.substring(15, 25);

        // Calcula DV mod10 de cada campo
        String c1 = c1sem + calcularMod10(c1sem);
        String c2 = c2sem + calcularMod10(c2sem);
        String c3 = c3sem + calcularMod10(c3sem);

        // LD47 = campo1(10) + campo2(11) + campo3(11) + DV(1) + fatorValor(14)
        return c1 + c2 + c3 + dvBarras + fatorValor;
    }

    private String calcularMod10(String numeros) {
        int soma = 0;
        boolean dobrar = true;
        for (int i = numeros.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(numeros.charAt(i));
            if (dobrar) { n *= 2; if (n > 9) n -= 9; }
            soma += n;
            dobrar = !dobrar;
        }
        return String.valueOf((10 - (soma % 10)) % 10);
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    private void abrirDatePicker() {
        int ano = dataSelecionada.get(Calendar.YEAR);
        int mes = dataSelecionada.get(Calendar.MONTH);
        int dia = dataSelecionada.get(Calendar.DAY_OF_MONTH);
        new DatePickerDialog(this, (view, y, m, d) -> {
            dataSelecionada.set(y, m, d);
            String fmt = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y);
            campoDataVencimento.setText(fmt);
        }, ano, mes, dia).show();
    }

    // ── Salvar ───────────────────────────────────────────────────────────────

    private void salvarBoleto() {
        String descricao = campoDescricao.getText() != null
                ? campoDescricao.getText().toString().trim() : "";
        String codigo = campoCodigoBarras.getText() != null
                ? campoCodigoBarras.getText().toString().trim() : "";
        String valorStr = campoValor.getText() != null
                ? campoValor.getText().toString().trim() : "";

        if (valorStr.isEmpty()) {
            Toast.makeText(this, "Informe o valor do boleto.", Toast.LENGTH_SHORT).show();
            return;
        }

        long valorCentavos;
        try {
            valorCentavos = Math.round(Double.parseDouble(valorStr.replace(",", ".")) * 100);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valor inválido.", Toast.LENGTH_SHORT).show();
            return;
        }

        BoletoModel boleto = new BoletoModel();
        boleto.setDescricao(descricao.isEmpty() ? "Boleto" : descricao);
        boleto.setCodigoBarras(codigo);
        boleto.setValor(valorCentavos);
        boleto.setDataVencimento(new Timestamp(dataSelecionada.getTime()));

        repository.salvar(boleto, new BoletoRepository.Callback() {
            @Override
            public void onSucesso(String msg) {
                Toast.makeText(AdicionarBoletoActivity.this, msg, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            @Override
            public void onErro(String erro) {
                Toast.makeText(AdicionarBoletoActivity.this,
                        "Erro: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private String formatarCodigoBarras(String numeros) {
        if (numeros.isEmpty()) return "";
        int len = numeros.length();

        // ITF-44 bancário/tributos (44 dígitos): grupos de 12
        // ex: 846300000011 000000731000 013408803198 926035105200
        if (len == 44) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                if (i > 0 && i % 12 == 0) sb.append(' ');
                sb.append(numeros.charAt(i));
            }
            return sb.toString();
        }

        // ITF-48 concessionárias (48 dígitos): grupos de 12
        // ex: 846300000011 000000731000 013408803198 926035105200
        if (len == 48) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                if (i > 0 && i % 12 == 0) sb.append(' ');
                sb.append(numeros.charAt(i));
            }
            return sb.toString();
        }

        // Linha digitável bancária (47 dígitos): AAAAA.AAAAA BBBBB.BBBBBB CCCCC.CCCCCC D EEEEEEEEEEEE
        // Pontos: após posições 4, 14, 24 — Espaços: após posições 9, 20, 30, 31
        if (len == 47) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                sb.append(numeros.charAt(i));
                if (i == 4 || i == 14 || i == 24) sb.append('.');
                else if (i == 9 || i == 20 || i == 30 || i == 31) sb.append(' ');
            }
            return sb.toString();
        }

        // Digitação parcial ou tamanho não reconhecido: grupos de 4
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (i > 0 && i % 4 == 0) sb.append(' ');
            sb.append(numeros.charAt(i));
        }
        return sb.toString();
    }
}