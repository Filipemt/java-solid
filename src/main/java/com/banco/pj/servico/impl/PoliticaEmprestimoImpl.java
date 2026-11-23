package com.banco.pj.servico.impl;

import com.banco.pj.dominio.CondicoesEmprestimo;
import com.banco.pj.dominio.enums.TipoEmpresa;
import com.banco.pj.servico.PoliticaEmprestimo;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class PoliticaEmprestimoImpl implements PoliticaEmprestimo {
    private final Map<TipoEmpresa, CondicoesEmprestimo> condicoesEmprestimos = new EnumMap<>(Map.of(
            TipoEmpresa.MEI,  new CondicoesEmprestimo(new BigDecimal("20000.00"),    new BigDecimal("0.035"), 0,   1),
            TipoEmpresa.ME,   new CondicoesEmprestimo(new BigDecimal("100000.00"),   new BigDecimal("0.028"), 50,  3),
            TipoEmpresa.EPP,  new CondicoesEmprestimo(new BigDecimal("500000.00"),   new BigDecimal("0.022"), 100, 3),
            TipoEmpresa.LTDA, new CondicoesEmprestimo(new BigDecimal("2000000.00"),  new BigDecimal("0.018"), 150, 3),
            TipoEmpresa.SA,   new CondicoesEmprestimo(new BigDecimal("10000000.00"), new BigDecimal("0.012"), 250, 3)));

    @Override
    public CondicoesEmprestimo determinarCondicoesEmprestimo(TipoEmpresa tipoEmpresa) {
        return Optional.ofNullable(condicoesEmprestimos.get(tipoEmpresa))
                .orElseThrow(() -> new IllegalStateException("Condições de empréstimo não configurado para o tipo de empresa: " + tipoEmpresa));
    }
}
