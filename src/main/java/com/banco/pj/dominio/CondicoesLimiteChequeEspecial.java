package com.banco.pj.dominio;

import com.banco.pj.dominio.enums.TipoEmpresa;

import java.math.BigDecimal;

public record CondicoesLimiteChequeEspecial(BigDecimal limiteMaximo) {
    public static CondicoesLimiteChequeEspecial paraTipoEmpresa(TipoEmpresa tipo) {
        return switch (tipo) {
            case ME -> new CondicoesLimiteChequeEspecial(new BigDecimal("20000.00"));
            case EPP -> new CondicoesLimiteChequeEspecial(new BigDecimal("80000.00"));
            case LTDA -> new CondicoesLimiteChequeEspecial(new BigDecimal("200000.00"));
            default -> new CondicoesLimiteChequeEspecial(new BigDecimal("1000000.00"));
        };
    }
}
