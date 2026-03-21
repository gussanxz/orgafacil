package com.gussanxz.orgafacil.funcionalidades.contas.movimentacoes.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * CelebracaoManager
 * ═══════════════════════════════════════════════════════════════
 * Orquestra a animação de celebração quando todas as parcelas de
 * uma série são quitadas.
 *
 * USO em ResumoParcelasActivity (substituir exibirEmptyStateTodasPagas):
 *
 *   NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt","BR"));
 *   String sub = "Total pago: " + fmt.format(resumo.valorTotalCentavos / 100.0);
 *   ViewGroup root = findViewById(R.id.main);
 *
 *   CelebracaoManager.celebrar(this, root, "Série quitada!", sub, null);
 *
 * Sequência visual:
 *   0 ms   → overlay escuro fade-in
 *   100 ms → confetti começa a cair
 *   400 ms → card central entra com overshoot
 *   3500ms → tudo some automaticamente
 *   *qualquer toque dispensa antes do tempo*
 *
 * Zero dependências externas — apenas APIs nativas do Android.
 * ═══════════════════════════════════════════════════════════════
 */
public class CelebracaoManager {

    // ─── Ponto de entrada público ──────────────────────────────────────────

    public static void celebrar(
            @NonNull  Context    context,
            @NonNull  ViewGroup  rootView,
            @NonNull  String     titulo,
            @NonNull  String     subtitulo,
            @Nullable Runnable   onDismiss
    ) {
        // ── Overlay ───────────────────────────────────────────────────────
        FrameLayout overlay = new FrameLayout(context);
        overlay.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setBackgroundColor(Color.argb(0, 0, 0, 0));
        overlay.setElevation(9999f);

        // ── Confetti ──────────────────────────────────────────────────────
        ConfettiView confetti = new ConfettiView(context);
        overlay.addView(confetti, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // ── Card central ──────────────────────────────────────────────────
        CelebracaoCardView card = new CelebracaoCardView(context, titulo, subtitulo);
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        cardParams.gravity       = Gravity.CENTER;
        cardParams.leftMargin    = dp(context, 24);
        cardParams.rightMargin   = dp(context, 24);
        card.setAlpha(0f);
        card.setScaleX(0.5f);
        card.setScaleY(0.5f);
        overlay.addView(card, cardParams);

        rootView.addView(overlay);

        // ── 1. Fade do overlay ────────────────────────────────────────────
        ObjectAnimator fadeOverlay = ObjectAnimator.ofArgb(
                overlay, "backgroundColor",
                Color.argb(0, 0, 0, 0),
                Color.argb(150, 0, 0, 0));
        fadeOverlay.setDuration(300);
        fadeOverlay.start();

        // ── 2. Confetti ───────────────────────────────────────────────────
        new Handler(Looper.getMainLooper()).postDelayed(confetti::iniciar, 100);

        // ── 3. Card com overshoot ─────────────────────────────────────────
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ObjectAnimator alpha  = ObjectAnimator.ofFloat(card, "alpha",  0f, 1f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(card, "scaleX", 0.5f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(card, "scaleY", 0.5f, 1f);

            alpha.setDuration(350);
            scaleX.setDuration(520);
            scaleY.setDuration(520);

            OvershootInterpolator overshoot = new OvershootInterpolator(2.2f);
            scaleX.setInterpolator(overshoot);
            scaleY.setInterpolator(overshoot);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(alpha, scaleX, scaleY);
            set.start();
        }, 400);

        // ── 4. Dismiss automático ─────────────────────────────────────────
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable autoDispense = () -> dispensar(overlay, rootView, onDismiss);
        handler.postDelayed(autoDispense, 3500);

        // ── 5. Dismiss no toque ───────────────────────────────────────────
        overlay.setOnClickListener(v -> {
            handler.removeCallbacks(autoDispense);
            dispensar(overlay, rootView, onDismiss);
        });
    }

    private static void dispensar(View overlay, ViewGroup root, @Nullable Runnable cb) {
        if (overlay.getParent() == null) return;
        overlay.animate()
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    root.removeView(overlay);
                    if (cb != null) cb.run();
                })
                .start();
    }


    // ═══════════════════════════════════════════════════════════════════════
    //  ConfettiView — partículas coloridas caindo com física simples
    // ═══════════════════════════════════════════════════════════════════════

    public static class ConfettiView extends View {

        private static final int QUANTIDADE = 90;
        private static final int[] CORES = {
                0xFFE53935, 0xFF8E24AA, 0xFF1E88E5, 0xFF00ACC1,
                0xFF43A047, 0xFFFB8C00, 0xFFFFD600, 0xFFF06292, 0xFF00BFA5
        };

        private final List<Particula> particulas = new ArrayList<>();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random rnd = new Random();
        private ValueAnimator animator;

        public ConfettiView(Context c)                         { super(c); }
        public ConfettiView(Context c, AttributeSet a)         { super(c, a); }
        public ConfettiView(Context c, AttributeSet a, int d)  { super(c, a, d); }

        public void iniciar() {
            particulas.clear();
            int w = getWidth() > 0 ? getWidth() : 1080;

            for (int i = 0; i < QUANTIDADE; i++) {
                particulas.add(new Particula(rnd, w, CORES));
            }

            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(2800);
            animator.addUpdateListener(a -> invalidate());
            animator.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    animate().alpha(0f).setDuration(600).start();
                }
            });
            animator.start();
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            if (animator == null || particulas.isEmpty()) return;

            float progress = animator.getAnimatedFraction();
            int   h        = getHeight() > 0 ? getHeight() : 1920;

            for (Particula p : particulas) {
                if (progress < p.delay) continue;

                float t = Math.min(1f, (progress - p.delay) / (1f - p.delay));
                float x = p.x  + p.vx  * t * 200f;
                float y = p.sy + p.vy  * t * h * 0.5f + 400f * t * t;
                float r = p.r0 + p.vr  * t * 540f;

                // Fade out nos últimos 25%
                int alpha = t > 0.75f ? (int) (255 * (1f - (t - 0.75f) / 0.25f)) : 255;
                paint.setColor(p.cor);
                paint.setAlpha(alpha);

                canvas.save();
                canvas.translate(x, y);
                canvas.rotate(r);

                float s = p.tam / 2f;
                switch (p.forma) {
                    case 0:  // quadrado
                        canvas.drawRect(-s, -s, s, s, paint);
                        break;
                    case 1:  // círculo
                        canvas.drawCircle(0, 0, s, paint);
                        break;
                    default: // streamer (retângulo fino)
                        canvas.drawRect(-s * 0.35f, -p.tam, s * 0.35f, p.tam, paint);
                        break;
                }
                canvas.restore();
            }
        }

        private static class Particula {
            float x, sy, vx, vy, vr, r0, tam, delay;
            int cor, forma;

            Particula(Random rnd, int w, int[] cores) {
                x     = rnd.nextFloat() * w;
                sy    = -(rnd.nextFloat() * 300f + 20f);
                vx    = (rnd.nextFloat() - 0.5f) * 1.2f;
                vy    = rnd.nextFloat() * 1.2f + 0.8f;
                vr    = (rnd.nextFloat() - 0.5f) * 3f;
                r0    = rnd.nextFloat() * 360f;
                tam   = rnd.nextFloat() * 14f + 7f;
                delay = rnd.nextFloat() * 0.35f;
                cor   = cores[rnd.nextInt(cores.length)];
                forma = rnd.nextInt(3);
            }
        }
    }


    // ═══════════════════════════════════════════════════════════════════════
    //  CelebracaoCardView — card branco com emoji pulsante
    // ═══════════════════════════════════════════════════════════════════════

    private static class CelebracaoCardView extends FrameLayout {

        CelebracaoCardView(Context ctx, String titulo, String subtitulo) {
            super(ctx);

            // Fundo branco arredondado
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(dp(ctx, 20));
            setBackground(bg);

            int padH = dp(ctx, 32);
            int padV = dp(ctx, 24);
            setPadding(padH, padV, padH, dp(ctx, 18));

            // Layout interno
            LinearLayout inner = new LinearLayout(ctx);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setGravity(Gravity.CENTER_HORIZONTAL);

            // Emoji 🎉
            final TextView tvEmoji = new TextView(ctx);
            tvEmoji.setText("\uD83C\uDF89");
            tvEmoji.setTextSize(TypedValue.COMPLEX_UNIT_SP, 56);
            tvEmoji.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            ep.bottomMargin = dp(ctx, 10);
            inner.addView(tvEmoji, ep);
            pulsarEmoji(tvEmoji);

            // Título
            TextView tvTitulo = new TextView(ctx);
            tvTitulo.setText(titulo);
            tvTitulo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            tvTitulo.setTypeface(tvTitulo.getTypeface(), Typeface.BOLD);
            tvTitulo.setTextColor(Color.parseColor("#1B5E20"));
            tvTitulo.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            tp.bottomMargin = dp(ctx, 6);
            inner.addView(tvTitulo, tp);

            // Subtítulo
            TextView tvSub = new TextView(ctx);
            tvSub.setText(subtitulo);
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvSub.setTextColor(Color.parseColor("#666666"));
            tvSub.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            sp.bottomMargin = dp(ctx, 18);
            inner.addView(tvSub, sp);

            // Hint
            TextView tvHint = new TextView(ctx);
            tvHint.setText("Toque para fechar");
            tvHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvHint.setTextColor(Color.parseColor("#BBBBBB"));
            tvHint.setGravity(Gravity.CENTER);
            inner.addView(tvHint);

            addView(inner);
        }

        private void pulsarEmoji(final View v) {
            v.animate()
                    .scaleX(1.15f).scaleY(1.15f)
                    .setDuration(550)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> v.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(550)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .withEndAction(() -> pulsarEmoji(v))
                            .start())
                    .start();
        }
    }

    // ─── Util ─────────────────────────────────────────────────────────────

    private static int dp(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }
}