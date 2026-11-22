package com.banco.pj.servico.impl.strategy;

import com.banco.pj.dominio.ContaPJ;
import com.banco.pj.dominio.enums.TipoEmpresa;
import com.banco.pj.dominio.exceptions.OperacaoInvalidaException;
import com.banco.pj.servico.PoliticaTarifacao;

import java.util.EnumMap;
import java.util.Map;

/**
 * Escolhe a Strategy de tarifacao de acordo com o tipo da empresa.
 */
public class SeletorPoliticaTarifacao {

    private final Map<TipoEmpresa, PoliticaTarifacao> politicas;

    public SeletorPoliticaTarifacao(Map<TipoEmpresa, PoliticaTarifacao> politicas) {
        this.politicas = new EnumMap<>(politicas);
    }

    public static SeletorPoliticaTarifacao padrao() {
        return new SeletorPoliticaTarifacao(Map.of(
                TipoEmpresa.MEI, new PoliticaTarifacaoMEIStrategy(),
                TipoEmpresa.ME, new PoliticaTarifacaoMEStrategy(),
                TipoEmpresa.EPP, new PoliticaTarifacaoEPPStrategy(),
                TipoEmpresa.LTDA, new PoliticaTarifacaoLTDAStrategy(),
                TipoEmpresa.SA, new PoliticaTarifacaoSAStrategy()
        ));
    }

    public PoliticaTarifacao para(ContaPJ conta) {
        PoliticaTarifacao politica = politicas.get(conta.getTipo());
        if (politica == null) {
            throw new OperacaoInvalidaException("Sem politica de tarifacao para o tipo " + conta.getTipo());
        }
        return politica;
    }
}
