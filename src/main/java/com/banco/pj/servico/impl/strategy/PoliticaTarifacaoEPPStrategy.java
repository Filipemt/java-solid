package com.banco.pj.servico.impl.strategy;

import com.banco.pj.servico.PoliticaTarifacao;

import java.math.BigDecimal;

public class PoliticaTarifacaoEPPStrategy implements PoliticaTarifacao {
    @Override
    public BigDecimal calcularTarifaMensal() {
        return new BigDecimal("49.90");
    }

    @Override
    public BigDecimal calcularTarifaOperacao() {
        return new BigDecimal("1.00");
    }

    @Override
    public BigDecimal calcularTarifaTed() {
        return new BigDecimal("9.50");
    }

    @Override
    public BigDecimal calcularTarifaBoleto() {
        return new BigDecimal("2.00");
    }

    @Override
    public BigDecimal limitePix() {
        return new BigDecimal("50000.00");
    }
}
