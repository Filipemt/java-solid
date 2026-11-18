package com.banco.pj.servico;

import com.banco.pj.dominio.ContaPJ;
import com.banco.pj.dominio.OperacaoInvalidaException;
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

    public static BigDecimal executarTarifaPix(ContaPJ origem, BigDecimal v) {
        BigDecimal limitePix;
        if (origem.getTipo() == TipoEmpresa.MEI) {
            limitePix = new BigDecimal("5000.00");
        } else if (origem.getTipo() == TipoEmpresa.ME) {
            limitePix = new BigDecimal("20000.00");
        } else if (origem.getTipo() == TipoEmpresa.EPP) {
            limitePix = new BigDecimal("50000.00");
        } else if (origem.getTipo() == TipoEmpresa.LTDA) {
            limitePix = new BigDecimal("100000.00");
        } else {
            limitePix = new BigDecimal("500000.00");
        }
        if (v.compareTo(limitePix) > 0) {
            throw new OperacaoInvalidaException("Valor acima do limite de PIX");
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal executarTarifaTed(ContaPJ origem) {
        BigDecimal tarifa;
        if (origem.getTipo() == TipoEmpresa.MEI) {
            tarifa = new BigDecimal("4.75");
        } else if (origem.getTipo() == TipoEmpresa.ME || origem.getTipo() == TipoEmpresa.EPP) {
            tarifa = new BigDecimal("9.50");
        } else if (origem.getTipo() == TipoEmpresa.LTDA) {
            tarifa = new BigDecimal("8.00");
        } else {
            tarifa = new BigDecimal("6.00");
        }
        return tarifa;
    }

    public static BigDecimal executarTarifaBoleto(ContaPJ conta) {
        BigDecimal tarifa;
        if (conta.getTipo() == TipoEmpresa.MEI) {
            tarifa = BigDecimal.ZERO;
        } else if (conta.getTipo() == TipoEmpresa.SA) {
            tarifa = new BigDecimal("1.00");
        } else {
            tarifa = new BigDecimal("2.00");
        }
        return tarifa;
    }
}
