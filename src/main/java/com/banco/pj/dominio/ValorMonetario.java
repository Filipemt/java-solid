package com.banco.pj.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ValorMonetario {

    private static final int CASAS_DECIMAIS = 2;
    private static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_UP;

    private ValorMonetario() {
    }

    public static BigDecimal normalizar(BigDecimal valor) {
        return valor.setScale(CASAS_DECIMAIS, ARREDONDAMENTO);
    }
}
