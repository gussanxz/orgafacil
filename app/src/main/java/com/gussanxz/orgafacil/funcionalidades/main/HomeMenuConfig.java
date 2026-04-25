package com.gussanxz.orgafacil.funcionalidades.main;

import com.gussanxz.orgafacil.R;
import com.gussanxz.orgafacil.funcionalidades.main.HomeMenuItem.StatusModulo;

/**
 * HomeMenuConfig
 *
 * Fonte única de verdade para os itens da grade da home.
 *
 * Boas práticas aplicadas:
 *  - Toda configuração de menu em um só lugar: para adicionar,
 *    remover ou alterar um card, edite apenas este arquivo.
 *  - Constantes de índice (IDX_*) mapeiam cada posição do array
 *    ao CardView correspondente no layout, tornando o vínculo
 *    explícito e seguro contra reordenações acidentais.
 *  - Classe utilitária: construtor privado, método estático puro.
 *
 * Ordem dos cards no layout (ac_main_home.xml):
 *  [0] Contas        [1] Vendas
 *  [2] Mercado       [3] Atividades
 *  [4] Boleto        [5] Minha Conta
 */
public final class HomeMenuConfig {

    // ─────────────────────────────────────────────────────────────
    // Índices — mantidos em sincronia com o layout XML
    // ─────────────────────────────────────────────────────────────

    public static final int IDX_CONTAS       = 0;
    public static final int IDX_VENDAS       = 1;
    public static final int IDX_MERCADO      = 2;
    public static final int IDX_ATIVIDADES   = 3;
    public static final int IDX_BOLETO       = 4;
    public static final int IDX_MINHA_CONTA  = 5;

    // ─────────────────────────────────────────────────────────────
    // Construtor privado — classe utilitária, não instanciável
    // ─────────────────────────────────────────────────────────────

    private HomeMenuConfig() {}

    // ─────────────────────────────────────────────────────────────
    // Definição dos itens
    // ─────────────────────────────────────────────────────────────

    /**
     * Retorna a lista de itens na ordem exata dos cards do layout.
     * Altere aqui para modificar ícone, título, status ou disponibilidade
     * de qualquer card — sem tocar em nenhuma outra classe.
     */
    public static HomeMenuItem[] obterItens() {
        return new HomeMenuItem[] {

                /* 0 – Contas */
                new HomeMenuItem(
                        R.drawable.ic_wallet_96,
                        R.string.menu_home_contas,
                        StatusModulo.EM_TESTE,
                        true
                ),

                /* 1 – Vendas */
                new HomeMenuItem(
                        R.drawable.ic_sale_96,
                        R.string.menu_home_vendas,
                        StatusModulo.EM_DESENVOLVIMENTO,
                        true
                ),

                /* 2 – Lista Mercado */
                new HomeMenuItem(
                        R.drawable.ic_shopping_cart_96,
                        R.string.menu_home_mercado,
                        StatusModulo.EM_TESTE,
                        true
                ),

                /* 3 – Lista Atividades */
                new HomeMenuItem(
                        R.drawable.ic_checklist_96,
                        R.string.menu_home_atividades,
                        StatusModulo.EM_BREVE,
                        false   // bloqueado: exibe Toast ao clicar
                ),

                /* 4 – Boleto por CPF */
                new HomeMenuItem(
                        R.drawable.ic_barcode_96,
                        R.string.menu_home_boleto,
                        StatusModulo.EM_BREVE,
                        false   // bloqueado: exibe Toast ao clicar
                ),

                /* 5 – Minha Conta */
                new HomeMenuItem(
                        R.drawable.ic_manage_accounts_96,
                        R.string.menu_home_minha_conta,
                        StatusModulo.EM_DESENVOLVIMENTO,
                        true
                ),
        };
    }
}