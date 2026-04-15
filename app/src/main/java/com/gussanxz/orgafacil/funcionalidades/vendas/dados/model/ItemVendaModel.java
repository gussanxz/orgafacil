package com.gussanxz.orgafacil.funcionalidades.vendas.dados.model;

public interface ItemVendaModel {
    String getId();
    String getNome();      // Produto retorna nome, Serviço retorna descrição
    String getDescricao(); // Categoria ou detalhe

    // Atualizado para int (centavos)
    int getPreco();        // Preço ou Valor

    int getTipo();         // 1 = Produto, 2 = Serviço

    // Constantes para ajudar
    int TIPO_PRODUTO = 1;
    int TIPO_SERVICO = 2;
}