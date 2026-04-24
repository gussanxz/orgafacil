package com.gussanxz.orgafacil.funcionalidades.vendas.visual.novavenda.helper;

import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemSacolaVendaModel;
import com.gussanxz.orgafacil.funcionalidades.vendas.negocio.modelos.ItemVendaModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CarrinhoManager {

    private final Map<String, ItemSacolaVendaModel> itens = new LinkedHashMap<>();

    public void adicionar(ItemVendaModel item, int quantidade) {
        String chave = ItemSacolaVendaModel.gerarChave(item);
        ItemSacolaVendaModel existente = itens.get(chave);
        if (existente == null) {
            ItemSacolaVendaModel novo = new ItemSacolaVendaModel(item);
            for (int i = 1; i < quantidade; i++) novo.incrementarQuantidade();
            itens.put(chave, novo);
        } else {
            for (int i = 0; i < quantidade; i++) existente.incrementarQuantidade();
        }
    }

    public void incrementar(String chave) {
        ItemSacolaVendaModel item = itens.get(chave);
        if (item != null) item.incrementarQuantidade();
    }

    public void decrementar(String chave) {
        ItemSacolaVendaModel item = itens.get(chave);
        if (item != null) {
            item.decrementarQuantidade();
            if (item.getQuantidade() <= 0) itens.remove(chave);
        }
    }

    public void remover(String chave) {
        itens.remove(chave);
    }

    public void restaurar(List<ItemSacolaVendaModel> lista) {
        itens.clear();
        for (ItemSacolaVendaModel item : lista) itens.put(item.getChave(), item);
    }

    public void limpar() {
        itens.clear();
    }

    public List<ItemSacolaVendaModel> getItens() {
        return new ArrayList<>(itens.values());
    }

    public boolean isEmpty() {
        return itens.isEmpty();
    }

    public int getQuantidadeTotal() {
        int total = 0;
        for (ItemSacolaVendaModel item : itens.values()) total += item.getQuantidade();
        return total;
    }

    public double getValorTotal() {
        double total = 0.0;
        for (ItemSacolaVendaModel item : itens.values()) total += item.getSubtotal();
        return total;
    }
}
