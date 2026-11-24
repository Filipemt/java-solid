package com.banco.pj.dominio.enums;

import com.banco.pj.dominio.exceptions.OperacaoInvalidaException;

import java.util.Arrays;

public enum TipoExtrato {
    TXT, CSV, JSON;

    public static TipoExtrato deFormato(String formato) {
        if (formato == null) {
            throw new OperacaoInvalidaException("Formato obrigatorio");
        }
        String formatoNormalizado = formato.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(tipo -> tipo.name().equals(formatoNormalizado))
                .findFirst()
                .orElseThrow(() -> new OperacaoInvalidaException("Formato de extrato nao suportado: " + formato));
    }
}
