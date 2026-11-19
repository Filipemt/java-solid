package com.banco.pj.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Emprestimo(BigDecimal valorContratado,
                         int meses,
                         BigDecimal taxaMensal,
                         BigDecimal valorParcela,
                         BigDecimal totalAPagar) {

    public static Emprestimo calcular(BigDecimal valor, int meses, BigDecimal taxaMensal) {
        BigDecimal fator = BigDecimal.ONE.add(taxaMensal.multiply(BigDecimal.valueOf(meses)));
        BigDecimal total = valor.multiply(fator).setScale(2, RoundingMode.HALF_UP);
        BigDecimal parcela = total.divide(BigDecimal.valueOf(meses), 2, RoundingMode.HALF_UP);
        return new Emprestimo(valor, meses, taxaMensal, parcela, total);
    }
}
