package com.banco.pj.servico;

import com.banco.pj.dominio.ContaPJ;

import java.math.BigDecimal;

public interface PoliticaTarifacao {
    BigDecimal calcularTarifaMensal(ContaPJ conta, BigDecimal tarifa);
    BigDecimal calcularTarifaOperacao(ContaPJ conta, BigDecimal tarifaBase);
    BigDecimal calcularTarifaTed(ContaPJ origem);
    BigDecimal calcularTarifaBoleto(ContaPJ conta);
}
