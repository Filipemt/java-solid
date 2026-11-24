package com.banco.pj.servico.impl.strategy;

import com.banco.pj.dominio.enums.TipoExtrato;
import com.banco.pj.servico.GeradorExtrato;

import java.util.EnumMap;
import java.util.Map;

public class SeletorGeracaoExtrato {

    private final Map<TipoExtrato, GeradorExtrato> extratoMap;

    public SeletorGeracaoExtrato(Map<TipoExtrato, GeradorExtrato> extratoMap) {
        this.extratoMap = new EnumMap<>(extratoMap);
    }

    public static SeletorGeracaoExtrato padrao() {
        return new SeletorGeracaoExtrato(Map.of(
                TipoExtrato.CSV, new GeradorExtratoCsvStrategy(),
                TipoExtrato.TXT, new GeradorExtratoTxtStrategy(),
                TipoExtrato.JSON, new GeradorExtratoJsonStrategy()
        ));
    }

    public GeradorExtrato getGeradorExtrato(TipoExtrato tipoExtrato) {
        GeradorExtrato geradorStrategy = extratoMap.get(tipoExtrato);
        if (geradorStrategy == null) {
            throw new IllegalStateException("Gerador de extrato não configurado para o tipo: " + tipoExtrato);
        }
        return geradorStrategy;
    }
}
