package com.banco.pj.servico.impl.strategy;

import com.banco.pj.servico.PoliticaTarifacao;

import java.math.BigDecimal;

public class PoliticaTarifacaoMEIStrategy implements PoliticaTarifacao {
    @Override
    public BigDecimal calcularTarifaMensal() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calcularTarifaOperacao() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calcularTarifaTed() {
        return new BigDecimal("4.75");
    }

    @Override
    public BigDecimal calcularTarifaBoleto() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal limitePix() {
        return new BigDecimal("5000.00");
    }
}
