package com.banco.pj.servico.impl.strategy;

import com.banco.pj.servico.PoliticaTarifacao;

import java.math.BigDecimal;

public class PoliticaTarifacaoSAStrategy implements PoliticaTarifacao {
    @Override
    public BigDecimal calcularTarifaMensal() {
        return new BigDecimal("149.90");
    }

    @Override
    public BigDecimal calcularTarifaOperacao() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calcularTarifaTed() {
        return new BigDecimal("6.00");
    }

    @Override
    public BigDecimal calcularTarifaBoleto() {
        return new BigDecimal("1.00");
    }

    @Override
    public BigDecimal limitePix() {
        return new BigDecimal("500000.00");
    }
}
