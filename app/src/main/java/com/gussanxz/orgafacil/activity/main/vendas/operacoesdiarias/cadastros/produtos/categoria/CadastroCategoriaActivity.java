package com.gussanxz.orgafacil.activity.main.vendas.operacoesdiarias.cadastros.produtos.categoria;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.gussanxz.orgafacil.R;
import android.widget.GridLayout;
import android.util.Log;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.gussanxz.orgafacil.model.Categoria;

public class CadastroCategoriaActivity extends AppCompatActivity {

    // Componentes da tela
    private FloatingActionButton fabVoltar;
    private FloatingActionButton fabSalvarCategoria;
    private GridLayout containerIcones; // O container que segura os cards
    private MaterialCardView cardBtnSelecionarIcones; // O card que está selecionado
    private LinearLayout layoutSelecao; // O layout que mostra os ícones

    //Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;


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

        //Inicializar o firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
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
        int corVerde = Color.parseColor("#25D366"); // Cor de seleção
        int corFundoCinza = Color.parseColor("#F5F5F5"); // Cor desmarcado
        int corIconeCinza = Color.parseColor("#9E9E9E"); // Icone desmarcado
        int corBranco = Color.WHITE;
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
                    if (icone != null) icone.setColorFilter(corBranco);
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

    public void exibeSelecaoDeIcones(View view) {

        if (layoutSelecao.getVisibility() == View.GONE) {

            layoutSelecao.setVisibility(View.VISIBLE);
            // Mostra um Toast para informar que o ícone foi selecionado
            Toast.makeText(this, "Selecione o Ícone!", Toast.LENGTH_SHORT).show();
        }
        else {
            layoutSelecao.setVisibility(View.GONE);
        }

    }

    public void salvarCategoriaTeste(View view) {

        // 1. Verificação básica
        if (iconeSelecionadoIndex == -1) {
            Toast.makeText(this, "Por favor, selecione um ícone.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Pegar os textos dos Inputs
        TextInputEditText editNome = findViewById(R.id.editCategoria);
        TextInputEditText editDesc = findViewById(R.id.editDescricao);

        String nomeCategoria = editNome.getText().toString();
        String descCategoria = editDesc.getText().toString();

        //3. Verificação se o nome da categoria esta vazia
        if (nomeCategoria.isEmpty()) {
            editNome.setError("Nome da categoria é obrigatório.");
            return;
        }

        //4. Verificando usuário logado (segurança)
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Usuário não está logado.", Toast.LENGTH_SHORT).show();
            return;
        }

        //5. Pegando ID do Usuario
        String idUsuario = mAuth.getCurrentUser().getUid();

        //6. Preparando objeto para salvar no Firebase
        MaterialSwitch switchAtiva = findViewById(R.id.switchAtiva);
        boolean estaAtiva = switchAtiva.isChecked();

        Categoria novaCategoria = new Categoria(idUsuario, nomeCategoria, descCategoria, iconeSelecionadoIndex, estaAtiva);

        //7. Definindo caminho do firebase: vendas -> uid -> cadastros -> categorias
        DatabaseReference categoriasRef = mDatabase
                .child("vendas")
                .child("uid")
                .child(idUsuario)
                .child("cadastros")
                .child("categorias");

        //8. Gera um ID único para a categoria (push)
        DatabaseReference novaCategoriaRef = categoriasRef.push();
        String idGerado = novaCategoriaRef.getKey(); //Pega id gerado pelo firebase
        novaCategoria.setId(idGerado);

        //9. Salva no firebase
        novaCategoriaRef.setValue(novaCategoria)
            .addOnSuccessListener(aVoid -> {

                // 1. Mensagem de sucesso pro usuário
                Toast.makeText(this, "Categoria salva com sucesso!", Toast.LENGTH_SHORT).show();

                // 2. LOGS PARA CONFERÊNCIA (Filtrar por: APP_DEBUG)
                Log.d("APP_DEBUG", "\n"); // Pula linha
                Log.d("APP_DEBUG", "🟢 === SUCESSO NO FIREBASE ===");

                // MOSTRA O CAMINHO EXATO (Isso facilita muito achar no console)
                String caminho = "vendas/" + idUsuario + "/cadastros/categorias/" + idGerado;
                Log.d("APP_DEBUG", "📂 CAMINHO: " + caminho);
                Log.d("APP_DEBUG", "-----------------------------------");

                // MOSTRA OS DADOS SALVOS
                Log.d("APP_DEBUG", "🏷️ Nome:      " + novaCategoria.getNome());
                Log.d("APP_DEBUG", "📝 Descrição: " + novaCategoria.getDescricao());
                Log.d("APP_DEBUG", "🎨 Ícone IDX: " + novaCategoria.getIndexIcone());
                Log.d("APP_DEBUG", "🔌 Ativa:     " + novaCategoria.isAtiva());
                Log.d("APP_DEBUG", "🔑 ID Gerado: " + novaCategoria.getId());
                Log.d("APP_DEBUG", "===================================\n");
                Log.d("APP_DEBUG", "\n"); // Pula linha

                finish();
            })
            .addOnFailureListener(e -> {
                Log.e("APP_DEBUG", "🔴 ERRO AO SALVAR: " + e.getMessage());
                Toast.makeText(this, "Erro ao salvar categoria: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

       /* //  4. EXIBIR NO TERMINAL (Logcat)
        // Use a Tag "APP_DEBUG" para filtrar fácil depois
        Log.d("APP_DEBUG", "=== CATEGORIA SALVA COM SUCESSO ===");
        Log.d("APP_DEBUG", "Nome: " + nomeCategoria);
        Log.d("APP_DEBUG", "Descrição: " + descCategoria);
        Log.d("APP_DEBUG", "ID do Ícone: " + iconeSelecionadoIndex);
        Log.d("APP_DEBUG", "Ativa: " + estaAtiva);
        Log.d("APP_DEBUG", "===================================");

        Toast.makeText(this, "Salvo e exibido no Logcat!", Toast.LENGTH_SHORT).show();
        finish();*/
    }
}

