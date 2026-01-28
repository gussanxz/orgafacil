package com.gussanxz.orgafacil.ui.contas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager; // 1. IMPORT NECESSÁRIO
import androidx.recyclerview.widget.RecyclerView;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.data.model.Movimentacao;
import com.gussanxz.orgafacil.ui.contas.movimentacao.AdapterExibeListaMovimentacaoContas;
import com.gussanxz.orgafacil.ui.contas.movimentacao.ExibirItemListaMovimentacaoContas;
import com.gussanxz.orgafacil.ui.contas.movimentacao.HelperExibirDatasMovimentacao;

import java.util.ArrayList;
import java.util.List;

// 2. CORREÇÃO: Implementar a interface do Adapter para que o "this" funcione
public class FragmentContasAVencer extends Fragment implements AdapterExibeListaMovimentacaoContas.OnItemActionListener {

    private RecyclerView recyclerView;
    private AdapterExibeListaMovimentacaoContas adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_generica, container, false);

        // 3. CORREÇÃO: Usar a variável global 'recyclerView' em vez de criar uma nova local
        recyclerView = view.findViewById(R.id.recyclerInterno);

        // 4. CORREÇÃO: Definir o LayoutManager (obrigatório para aparecer a lista)
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 1. Criar dados MOCKADOS
        List<Movimentacao> contasFuturas = criarDadosFuturos();

        // 2. Processar os dados
        List<ExibirItemListaMovimentacaoContas> listaProcessada =
                HelperExibirDatasMovimentacao.agruparPorDiaOrdenar(contasFuturas);

        // 3. Configurar Adapter
        adapter = new AdapterExibeListaMovimentacaoContas(getContext(), listaProcessada, this);
        recyclerView.setAdapter(adapter);

        return view;
    }

    // --- Ações de Clique (Obrigatórias pela interface) ---
    @Override
    public void onDeleteClick(Movimentacao movimentacao) {
        Toast.makeText(getContext(), "Pagar conta: " + movimentacao.getDescricao(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLongClick(Movimentacao movimentacao) {
        Toast.makeText(getContext(), "Detalhes de: " + movimentacao.getDescricao(), Toast.LENGTH_SHORT).show();
    }

    // --- MOCK DATA ---
    private List<Movimentacao> criarDadosFuturos() {
        List<Movimentacao> lista = new ArrayList<>();

        lista.add(mockMov("Fornecedor Bebidas", "Estoque", "28/01/2026", "08:00", 450.00, "d"));
        lista.add(mockMov("Energia Elétrica", "Fixo", "05/02/2026", "10:00", 280.90, "d"));
        lista.add(mockMov("Internet Fibra", "Fixo", "05/02/2026", "11:00", 99.90, "d"));
        lista.add(mockMov("Aluguel Loja", "Aluguel", "10/02/2026", "09:00", 1500.00, "d"));
        lista.add(mockMov("Parcela Empréstimo (2/10)", "Financeiro", "15/02/2026", "00:00", 350.00, "d"));
        lista.add(mockMov("Repasse Maquininha", "Vendas", "12/02/2026", "14:00", 1200.00, "r"));

        return lista;
    }

    private Movimentacao mockMov(String titulo, String cat, String data, String hora, double valor, String tipo) {
        Movimentacao m = new Movimentacao();
        m.setDescricao(titulo);
        m.setCategoria(cat);
        m.setData(data);
        m.setHora(hora);
        m.setValor(valor);
        m.setTipo(tipo);
        return m;
    }
}