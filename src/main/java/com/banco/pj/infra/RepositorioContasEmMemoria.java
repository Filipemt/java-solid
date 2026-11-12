package com.banco.pj.infra;

import com.banco.pj.dominio.ContaPJ;

import java.util.HashMap;
import java.util.Map;

public class RepositorioContasEmMemoria {

    private final Map<String, ContaPJ> contas = new HashMap<>();

    public void salvar(ContaPJ conta) {
        contas.put(conta.getCnpj(), conta);
    }

    public ContaPJ buscarPorCnpj(String cnpj) {
        return contas.get(cnpj);
    }

    public boolean existe(String cnpj) {
        return contas.containsKey(cnpj);
    }
}
