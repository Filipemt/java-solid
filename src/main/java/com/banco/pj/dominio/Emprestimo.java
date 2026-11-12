package com.banco.pj.dominio;

import java.math.BigDecimal;

public record Emprestimo(BigDecimal valorContratado,
                         int meses,
                         BigDecimal taxaMensal,
                         BigDecimal valorParcela,
                         BigDecimal totalAPagar) {
}
