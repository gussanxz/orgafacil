package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.categoria;

import android.graphics.Color; // Importante para as cores
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView; // Importante para o ícone dentro do card
import android.widget.LinearLayout; // Importante para o container
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView; // Importante para os cards
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.gussanxz.orgafacil.R;

public class CadastroCategoriaActivity extends AppCompatActivity {

    // Componentes da tela
    private FloatingActionButton fabVoltar;
    private FloatingActionButton fabSalvarCategoria;
    private LinearLayout containerIcones; // O container que segura os cards
    private MaterialCardView cardBtnSelecionarIcones; // O card que está selecionado
    private LinearLayout layoutSelecao; // O layout que mostra os ícones


    // Variável para guardar qual ícone o usuário escolheu (0, 1, 2...)
    // -1 significa que nenhum foi escolhido ainda
    private int iconeSelecionadoIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Habilita o edge to edge
        EdgeToEdge.enable(this);

        // Definimos o xml/layout que iremos apresentar na activity
        setContentView(R.layout.activity_cadastro_categorias);

        // Ajuste de insets (barra de status, etc)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Chama o método que busca os IDs e configura os cliques
        inicializarComponentes();

    }

    private void inicializarComponentes() {
        // IDs que você já tinha
        fabVoltar = findViewById(R.id.fabVoltar);
        fabSalvarCategoria = findViewById(R.id.fabSuperiorSalvarCategoria);
        cardBtnSelecionarIcones = findViewById(R.id.cardBtnSelecionarIcones);
        layoutSelecao = findViewById(R.id.layoutSelecao);

        // NOVO: Buscando o container dos ícones no XML
        containerIcones = findViewById(R.id.containerIcones);

        // NOVO: Chama a função que configura o clique nos ícones
        configurarSelecaoIcones();
    }

    // --- NOVA LÓGICA DE SELEÇÃO DE ÍCONES ---

    private void configurarSelecaoIcones() {
        // Verifica se o container foi encontrado para evitar erros (NullPointerException)
        if (containerIcones == null) return;

        // Loop: Passa por cada filho dentro do container (cada card)
        for (int i = 0; i < containerIcones.getChildCount(); i++) {

            View view = containerIcones.getChildAt(i);
            final int indexAtual = i; // Guarda a posição atual (0, 1, 2...)

            // Verifica se o item é realmente um CardView
            if (view instanceof MaterialCardView) {
                MaterialCardView card = (MaterialCardView) view;

                // Define o clique do card
                card.setOnClickListener(v -> {
                    iconeSelecionadoIndex = indexAtual; // Salva qual foi escolhido
                    atualizarCores(card); // Atualiza o visual (verde/cinza)
                });
            }
        }
    }

    // Método visual: Pinta o escolhido de verde e reseta os outros para cinza
    private void atualizarCores(MaterialCardView cardClicado) {

        // Definição das cores
        int corVerde = Color.parseColor("#25D366");
        int corFundoCinza = Color.parseColor("#F5F5F5");
        int corIconeCinza = Color.parseColor("#9E9E9E");
        int corBorda = Color.parseColor("#E0E0E0");

        // Loop novamente para pintar todos corretamente
        for (int i = 0; i < containerIcones.getChildCount(); i++) {
            View view = containerIcones.getChildAt(i);

            if (view instanceof MaterialCardView) {
                MaterialCardView cardAtual = (MaterialCardView) view;

                // Pega a imagem (ícone) dentro do card
                ImageView icone = null;
                if (cardAtual.getChildCount() > 0 && cardAtual.getChildAt(0) instanceof ImageView) {
                    icone = (ImageView) cardAtual.getChildAt(0);
                }

                // Lógica de Pintura
                if (cardAtual == cardClicado) {
                    // SELECIONADO (Verde)
                    cardAtual.setCardBackgroundColor(corVerde);
                    cardAtual.setStrokeWidth(0);
                    if (icone != null) icone.setColorFilter(Color.WHITE);
                } else {
                    // NÃO SELECIONADO (Cinza)
                    cardAtual.setCardBackgroundColor(corFundoCinza);
                    cardAtual.setStrokeWidth(3); // Borda fina
                    cardAtual.setStrokeColor(corBorda);
                    if (icone != null) icone.setColorFilter(corIconeCinza);
                }
            }
        }
    }

    // --- MÉTODOS DOS BOTÕES ---

    public void retornarParaVendasCadastros(View view) {
        finish(); // Retorna para tela anterior
    }

    public void salvarCategoria(View view) {

        // Verificação simples: O usuário escolheu um ícone?
        if (iconeSelecionadoIndex == -1) {
            Toast.makeText(this, "Por favor, selecione um ícone para a categoria.", Toast.LENGTH_SHORT).show();
            return; // Para o código aqui e não salva
        }

        // Se chegou aqui, está tudo certo
        Toast toast = Toast.makeText(this,
                "Categoria salva! Ícone escolhido: " + iconeSelecionadoIndex,
                Toast.LENGTH_SHORT);
        toast.show();

        finish(); // Retorna para tela anterior
    }

    public void exibeSelecaoDeIcones(View view) {



        if (layoutSelecao.getVisibility() == View.INVISIBLE) {

            layoutSelecao.setVisibility(View.VISIBLE);
            // Mostra um Toast para informar que o ícone foi selecionado
            Toast.makeText(this, "Selecione o Ícone!", Toast.LENGTH_SHORT).show();
        }
        else {
            layoutSelecao.setVisibility(View.INVISIBLE);
        }

    }
}