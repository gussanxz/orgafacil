package com.gussanxz.orgafacil.ui.contas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.contas.negocio.modelos.MovimentacaoModel;
import com.gussanxz.orgafacil.ui.contas.movimentacao.AdapterExibeListaMovimentacaoContas;
import com.gussanxz.orgafacil.ui.contas.movimentacao.ExibirItemListaMovimentacaoContas;
import com.gussanxz.orgafacil.ui.contas.movimentacao.HelperExibirDatasMovimentacao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FragmentMovimentacoes extends Fragment implements AdapterExibeListaMovimentacaoContas.OnItemActionListener {

    private RecyclerView recyclerView;
    private AdapterExibeListaMovimentacaoContas adapter;

    // Lista final processada (com headers e itens)
    private List<ExibirItemListaMovimentacaoContas> listaProcessada = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_generica, container, false);

        recyclerView = view.findViewById(R.id.recyclerInterno);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 1. Carregar TODOS os dados brutos (Mockados)
        List<MovimentacaoModel> dadosBrutos = criarDadosFalsosParaTeste();

        // ---------------------------------------------------------
        // LÓGICA DE FILTRO: 5 MAIS RECENTES
        // ---------------------------------------------------------

        // A. Ordenar primeiro (do mais recente para o antigo)
        // Isso garante que quando cortarmos a lista, pegaremos os 5 mais novos, não 5 aleatórios.
        Collections.sort(dadosBrutos, (o1, o2) -> {
            // Compara data 2 com data 1 para ordem decrescente
            return converterData(o2).compareTo(converterData(o1));
        });

        // B. Cortar a lista (Pegar os 5 primeiros ou o tamanho total se for menor que 5)
        int limite = 5;
        List<MovimentacaoModel> dadosLimitados = new ArrayList<>();
        if (!dadosBrutos.isEmpty()) {
            dadosLimitados = dadosBrutos.subList(0, Math.min(dadosBrutos.size(), limite));
        }

        // ---------------------------------------------------------

        // 2. Processar APENAS os dados limitados
        // O Helper vai criar os Cabeçalhos (Headers) baseados nessa lista curta
        listaProcessada = HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(dadosLimitados);

        // 3. Configurar Adapter
        adapter = new AdapterExibeListaMovimentacaoContas(getContext(), listaProcessada, this);
        recyclerView.setAdapter(adapter);

        return view;
    }

    // --- Implementação dos Cliques do Adapter ---

    @Override
    public void onDeleteClick(MovimentacaoModel movimentacaoModel) {
        // Lógica para deletar
        Toast.makeText(getContext(), "Deletar: " + movimentacaoModel.getDescricao(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLongClick(MovimentacaoModel movimentacaoModel) {
        // Lógica do clique longo
        Toast.makeText(getContext(), "Segurou em: " + movimentacaoModel.getDescricao(), Toast.LENGTH_SHORT).show();
    }

    // --- Métodos Auxiliares ---

    /**
     * Converte as strings de data/hora do Model para um objeto Date Java.
     * Necessário para a ordenação funcionar corretamente.
     */
    private Date converterData(MovimentacaoModel m) {
        try {
            // Ajuste o padrão aqui se o seu banco salvar diferente (ex: "dd/MM/yyyy HH:mm")
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"));

            // Concatena data e hora para precisão
            String dataHoraString = m.getData() + " " + m.getHora();
            return sdf.parse(dataHoraString);
        } catch (ParseException e) {
            // Em caso de erro, retorna data zero (1970) para ir pro final da lista
            return new Date(0);
        }
    }

    // --- Geração de Dados Falsos (MOCK) ---
    private List<MovimentacaoModel> criarDadosFalsosParaTeste() {
        List<MovimentacaoModel> lista = new ArrayList<>();

        // Simular Hoje (21/01/2026)
        lista.add(mockMov("Compra Mercado", "Alimentação", "21/01/2026", "14:30", 150.50, "d"));
        lista.add(mockMov("Venda Teclado", "Vendas", "21/01/2026", "10:00", 200.00, "r"));

        // Simular Ontem (20/01/2026)
        lista.add(mockMov("Uber", "Transporte", "20/01/2026", "18:00", 25.90, "d"));
        lista.add(mockMov("Lanche", "Alimentação", "20/01/2026", "12:30", 35.00, "d"));

        // Simular Passado
        lista.add(mockMov("Salário", "Renda", "05/01/2026", "08:00", 3500.00, "r"));
        lista.add(mockMov("Aluguel", "Moradia", "05/01/2026", "09:00", 1200.00, "d"));

        return lista;
    }

    private MovimentacaoModel mockMov(String titulo, String cat, String data, String hora, double valor, String tipo) {
        MovimentacaoModel m = new MovimentacaoModel();
        m.setDescricao(titulo);
        m.setCategoria(cat);
        m.setData(data);
        m.setHora(hora);
        m.setValor(valor);
        m.setTipo(tipo); // "r" = receita, "d" = despesa
        return m;
    }
}