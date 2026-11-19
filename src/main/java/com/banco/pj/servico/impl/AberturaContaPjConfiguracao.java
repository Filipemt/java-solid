package com.banco.pj.servico.impl;

import com.banco.pj.dominio.ContaPJ;
import com.banco.pj.dominio.enums.TipoEmpresa;

import java.math.BigDecimal;

public class AberturaContaPjConfiguracao {
    private AberturaContaPjConfiguracao() {
    }


    public static void configurarConta(TipoEmpresa tipo, ContaPJ conta) {

        switch (tipo) {
            case MEI:
                break;
            case ME:
                conta.setLimiteChequeEspecial(new BigDecimal("5000.00"));
                break;
            case EPP:
                conta.setLimiteChequeEspecial(new BigDecimal("20000.00"));
                break;
            case LTDA:
                conta.setLimiteChequeEspecial(new BigDecimal("50000.00"));
                break;
            case SA:
                conta.setLimiteChequeEspecial(new BigDecimal("200000.00"));
                break;
        }
    }
}
