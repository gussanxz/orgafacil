package com.gussanxz.orgafacil.funcionalidades.boletos;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.oned.ITFReader;
import com.gussanxz.orgafacil.R;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class EscanearBoletoActivity extends AppCompatActivity {

    public static final String EXTRA_CODIGO = "codigo_barras";
    private static final String TAG = "EscanearBoleto";

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private final AtomicBoolean codigoEncontrado = new AtomicBoolean(false);

    // ITFReader + hints são imutáveis após onCreate, acesso apenas na thread do cameraExecutor
    private ITFReader itfReader;
    private Map<DecodeHintType, Object> hints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_escanear_boleto);

        previewView    = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Monta hints e reader uma única vez
        hints = new EnumMap<>(DecodeHintType.class);
        // TRY_HARDER *desligado* intencionalmente: com ele ativo o ITFReader aceita
        // decodificações parciais e ignora ALLOWED_LENGTHS, lendo 44 dígitos num código de 48.
        // ALLOWED_LENGTHS só funciona como filtro quando TRY_HARDER está ausente.
        hints.put(DecodeHintType.ALLOWED_LENGTHS, new int[]{44, 48});
        itfReader = new ITFReader();

        // Animação da linha de scan
        View scanArea = findViewById(R.id.scanArea);
        View scanLine = findViewById(R.id.scanLine);
        scanArea.post(() -> {
            float altura = scanArea.getHeight();
            ObjectAnimator anim = ObjectAnimator.ofFloat(scanLine, "translationY", 0f, altura - 4f);
            anim.setDuration(1800);
            anim.setRepeatMode(ObjectAnimator.REVERSE);
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            anim.setInterpolator(new LinearInterpolator());
            anim.start();
        });

        findViewById(R.id.btnCancelar).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        iniciarCamera();
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void iniciarCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 1920x1080 é suficiente e mais compatível que 4K
                ResolutionStrategy strategy = new ResolutionStrategy(
                        new android.util.Size(1920, 1080),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER);

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setResolutionSelector(new ResolutionSelector.Builder()
                                .setResolutionStrategy(strategy)
                                .build())
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (codigoEncontrado.get()) {
                        imageProxy.close();
                        return;
                    }
                    try {
                        android.media.Image mediaImage = imageProxy.getImage();
                        if (mediaImage == null) return;

                        // Extrai plano Y respeitando rowStride (pode ter padding em alguns devices)
                        android.media.Image.Plane yPlane = mediaImage.getPlanes()[0];
                        ByteBuffer yBuffer = yPlane.getBuffer();
                        int width     = mediaImage.getWidth();
                        int height    = mediaImage.getHeight();
                        int rowStride = yPlane.getRowStride();

                        byte[] yData;
                        if (rowStride == width) {
                            yData = new byte[yBuffer.remaining()];
                            yBuffer.get(yData);
                        } else {
                            yData = new byte[width * height];
                            byte[] rowBuf = new byte[rowStride];
                            for (int row = 0; row < height; row++) {
                                yBuffer.get(rowBuf, 0, Math.min(rowStride, yBuffer.remaining()));
                                System.arraycopy(rowBuf, 0, yData, row * width, width);
                            }
                        }

                        // Recorte: sem margem lateral (evita truncar barras),
                        // 20% de margem vertical para eliminar ruído acima/abaixo
                        int margemV      = height / 5;
                        int alturaCorte  = height - 2 * margemV;  // 60% central em altura
                        // Sem margem lateral: left=0, width=total
                        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                                yData, width, height,
                                0, margemV, width, alturaCorte, false);

                        // Tenta leitura normal
                        String codigo = tentarLer(
                                new BinaryBitmap(new HybridBinarizer(source)));

                        // Se falhou, tenta com buffer rotacionado 180°
                        // (ITF lido ao contrário produz dígitos embaralhados — não é simples reversão)
                        if (codigo == null) {
                            byte[] yInv = rotacionar180(yData, width, height);
                            PlanarYUVLuminanceSource sourceInv = new PlanarYUVLuminanceSource(
                                    yInv, width, height,
                                    0, margemV, width, alturaCorte, false);
                            codigo = tentarLer(
                                    new BinaryBitmap(new HybridBinarizer(sourceInv)));
                        }

                        if (codigo != null && codigoEncontrado.compareAndSet(false, true)) {
                            final String codigoFinal = codigo;
                            runOnUiThread(() -> {
                                Intent intent = new Intent();
                                intent.putExtra(EXTRA_CODIGO, codigoFinal);
                                setResult(RESULT_OK, intent);
                                finish();
                            });
                        }

                    } finally {
                        imageProxy.close();
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Erro ao iniciar câmera", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Erro ao iniciar câmera.", Toast.LENGTH_SHORT).show());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Tenta decodificar o bitmap como ITF.
     * Retorna a string com apenas dígitos se o comprimento for 44 ou 48, ou null caso contrário.
     */
    private String tentarLer(BinaryBitmap bitmap) {
        try {
            Result result = itfReader.decode(bitmap, hints);
            String soDigitos = result.getText().replaceAll("[^0-9]", "");
            int len = soDigitos.length();
            Log.d(TAG, "tentarLer: " + len + "d → " + soDigitos);
            if (len == 44 || len == 48) {
                return soDigitos;
            }
        } catch (NotFoundException | FormatException ignored) {
            // frame sem código legível — normal
        }
        return null;
    }

    /**
     * Rotaciona o buffer de luminância Y 180°.
     * Necessário quando a câmera captura o código da direita para a esquerda,
     * o que no ITF embaralha os dígitos (não é simples reversão da string).
     */
    private byte[] rotacionar180(byte[] yData, int width, int height) {
        byte[] resultado = new byte[width * height];
        int total = width * height;
        for (int i = 0; i < total; i++) {
            resultado[i] = yData[total - 1 - i];
        }
        return resultado;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}