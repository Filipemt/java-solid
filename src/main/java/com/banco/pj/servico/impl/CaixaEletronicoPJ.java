package com.banco.pj.servico.impl;

import com.banco.pj.dominio.exceptions.OperacaoInvalidaException;
import com.banco.pj.servico.OperacoesContaPJ;

import java.math.BigDecimal;

public class CaixaEletronicoPJ {

    private static final BigDecimal LIMITE_SAQUE = new BigDecimal("5000");
    private static final BigDecimal LIMITE_DEPOSITO_ENVELOPE = new BigDecimal("10000");

    private final OperacoesContaPJ operacoes;

    public CaixaEletronicoPJ(OperacoesContaPJ operacoes) {
        this.operacoes = operacoes;
    }

    public void sacar(String cnpj, BigDecimal valor) {
        if (valor.compareTo(LIMITE_SAQUE) > 0) {
            throw new OperacaoInvalidaException("Limite de saque no caixa excedido");
        }
        if (valor.remainder(BigDecimal.TEN).compareTo(BigDecimal.ZERO) != 0) {
            throw new OperacaoInvalidaException("Caixa so dispensa multiplos de 10");
        }
        operacoes.sacar(cnpj, valor);
        System.out.println("[CAIXA] Saque de R$ " + valor + " realizado para " + cnpj);
    }

    public void depositarEnvelope(String cnpj, BigDecimal valor) {
        if (valor.compareTo(LIMITE_DEPOSITO_ENVELOPE) > 0) {
            throw new OperacaoInvalidaException("Limite de deposito em envelope excedido");
        }
        operacoes.depositar(cnpj, valor);
        System.out.println("[CAIXA] Deposito de R$ " + valor + " realizado para " + cnpj);
    }

    public BigDecimal consultarSaldo(String cnpj) {
        return operacoes.consultarSaldo(cnpj);
    }
}
