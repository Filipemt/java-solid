package com.banco.pj.servico;

import java.math.BigDecimal;

/**
 * Strategy: cada tipo de empresa tem sua propria politica de tarifas e limites.
 */
public interface PoliticaTarifacao {
    BigDecimal calcularTarifaMensal();
    BigDecimal calcularTarifaOperacao();
    BigDecimal calcularTarifaTed();
    BigDecimal calcularTarifaBoleto();
    BigDecimal limitePix();
}
