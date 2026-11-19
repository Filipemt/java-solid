package com.banco.pj.dominio.exceptions;

public class ContaNaoEncontradaException extends RuntimeException {
    public ContaNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}
