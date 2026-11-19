package com.banco.pj.servico.impl;

import com.banco.pj.dominio.exceptions.OperacaoInvalidaException;
import com.banco.pj.dominio.enums.TipoEmpresa;
import com.banco.pj.infra.RepositorioContasEmMemoria;
import com.banco.pj.infra.ValidadorCnpj;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AberturaContaValidador {
    private final RepositorioContasEmMemoria repositorio;

    public AberturaContaValidador(RepositorioContasEmMemoria repositorio) {
        this.repositorio = repositorio;
    }

    public void validar(String cnpj, String razaoSocial, TipoEmpresa tipoEmpresa, String emailContato, BigDecimal depositoInicial) {
        if (!ValidadorCnpj.isValido(cnpj)) {
            throw new OperacaoInvalidaException("CNPJ invalido");
        }
        String cnpjLimpo = ValidadorCnpj.somenteDigitos(cnpj);
        if (razaoSocial == null || razaoSocial.trim().isEmpty()) {
            throw new OperacaoInvalidaException("Razao social obrigatoria");
        }
        if (tipoEmpresa == null) {
            throw new OperacaoInvalidaException("Tipo de empresa obrigatorio");
        }
        if (emailContato == null || !emailContato.contains("@")) {
            throw new OperacaoInvalidaException("Email invalido");
        }
        if (repositorio.existe(cnpjLimpo)) {
            throw new OperacaoInvalidaException("Ja existe conta para este CNPJ");
        }
        if (depositoInicial == null) {
            throw new OperacaoInvalidaException("Deposito inicial obrigatorio");
        }

        BigDecimal deposito = depositoInicial.setScale(2, RoundingMode.HALF_UP);
        if (deposito.signum() < 0) {
            throw new OperacaoInvalidaException("Deposito inicial invalido");
        }

        BigDecimal depositoMinimo = switch (tipoEmpresa) {
            case SA -> new BigDecimal("10000.00");
            case LTDA -> new BigDecimal("1000.00");
            case EPP -> new BigDecimal("500.00");
            default -> BigDecimal.ZERO;
        };
        if (deposito.compareTo(depositoMinimo) < 0) {
            throw new OperacaoInvalidaException("Deposito inicial abaixo do minimo para o tipo de empresa");
        }
    }
}
