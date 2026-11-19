package com.banco.pj.dominio;

import java.math.BigDecimal;

public record CondicoesEmprestimo(BigDecimal valorMaximo, BigDecimal taxa, int bonusTipo, int maxEmprestimos) {
    public static CondicoesEmprestimo paraTipoEmpresa(TipoEmpresa tipo) {
        return switch (tipo) {
            case MEI -> new CondicoesEmprestimo(
                    new BigDecimal("20000.00"),
                    new BigDecimal("0.035"),
                    0,
                    1);
            case ME -> new CondicoesEmprestimo(
                    new BigDecimal("100000.00"),
                    new BigDecimal("0.028"),
                    50,
                    3);
            case EPP -> new CondicoesEmprestimo(
                    new BigDecimal("500000.00"),
                    new BigDecimal("0.022"),
                    100,
                    3);
            case LTDA -> new CondicoesEmprestimo(
                    new BigDecimal("2000000.00"),
                    new BigDecimal("0.018"),
                    150,
                    3);
            default -> new CondicoesEmprestimo(
                    new BigDecimal("10000000.00"),
                    new BigDecimal("0.012"),
                    250,
                    3);
        };
    }
}
