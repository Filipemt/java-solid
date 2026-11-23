package com.banco.pj.servico.impl;

import com.banco.pj.dominio.CondicoesLimiteChequeEspecial;
import com.banco.pj.dominio.enums.TipoEmpresa;
import com.banco.pj.servico.PoliticaChequeEspecial;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class PoliticaChequeEspecialImpl implements PoliticaChequeEspecial {
    private final Map<TipoEmpresa, CondicoesLimiteChequeEspecial> condicoesChequeEspecial = new EnumMap<>(Map.of(
            TipoEmpresa.ME, new CondicoesLimiteChequeEspecial(new BigDecimal("20000.00")),
            TipoEmpresa.EPP, new CondicoesLimiteChequeEspecial(new BigDecimal("80000.00")),
            TipoEmpresa.LTDA, new CondicoesLimiteChequeEspecial(new BigDecimal("200000.00")),
            TipoEmpresa.SA, new CondicoesLimiteChequeEspecial(new BigDecimal("1000000.00"))
    ));

    @Override
    public CondicoesLimiteChequeEspecial determinarCondicoesLimiteChequeEspecial(TipoEmpresa tipoEmpresa) {
        return Optional.ofNullable(condicoesChequeEspecial.get(tipoEmpresa))
                .orElseThrow(() -> new IllegalStateException("Condições de cheque especial não configurado para o tipo de empresa: " + tipoEmpresa));
    }
}
