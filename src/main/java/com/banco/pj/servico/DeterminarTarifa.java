package com.banco.pj.servico;

import com.banco.pj.dominio.ContaPJ;
import com.banco.pj.dominio.TipoEmpresa;

import java.math.BigDecimal;

public class DeterminarTarifa {

    public static BigDecimal executarTarifaMensal(ContaPJ conta, BigDecimal tarifa) {
        if (conta.getTipo() == TipoEmpresa.MEI) {
            tarifa = BigDecimal.ZERO;
        } else if (conta.getTipo() == TipoEmpresa.ME) {
            tarifa = new BigDecimal("29.90");
        } else if (conta.getTipo() == TipoEmpresa.EPP) {
            tarifa = new BigDecimal("49.90");
        } else if (conta.getTipo() == TipoEmpresa.LTDA) {
            tarifa = new BigDecimal("79.90");
        } else {
            tarifa = new BigDecimal("149.90");
        }
        return tarifa;
    }

    public static BigDecimal executarTarifaOperacao(ContaPJ conta, BigDecimal tarifa) {
        if (conta.getTipo() == TipoEmpresa.MEI) {
            tarifa = BigDecimal.ZERO;
        } else if (conta.getTipo() == TipoEmpresa.ME) {
            tarifa = new BigDecimal("1.50");
        } else if (conta.getTipo() == TipoEmpresa.EPP) {
            tarifa = new BigDecimal("1.00");
        } else if (conta.getTipo() == TipoEmpresa.LTDA) {
            tarifa = new BigDecimal("0.50");
        } else {
            tarifa = BigDecimal.ZERO;
        }
        return tarifa;
    }
}
