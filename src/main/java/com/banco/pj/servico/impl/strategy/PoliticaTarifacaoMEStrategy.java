package com.banco.pj.servico.impl.strategy;

import com.banco.pj.servico.PoliticaTarifacao;

import java.math.BigDecimal;

public class PoliticaTarifacaoMEStrategy implements PoliticaTarifacao {
    @Override
    public BigDecimal calcularTarifaMensal() {
        return new BigDecimal("29.90");
    }

    @Override
    public BigDecimal calcularTarifaOperacao() {
        return new BigDecimal("1.50");
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
        return new BigDecimal("20000.00");
    }
}
