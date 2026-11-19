package com.banco.pj.servico.impl;

import com.banco.pj.dominio.ContaMEI;
import com.banco.pj.dominio.ContaPJ;
import com.banco.pj.dominio.enums.TipoEmpresa;

import java.math.BigDecimal;

public class AberturaContaPjCriacao {
    public static ContaPJ criar(TipoEmpresa tipo, String cnpjLimpo, String razaoSocial, String emailContato, String telefone, BigDecimal deposito) {
        ContaPJ conta;
        if (tipo == TipoEmpresa.MEI) {
            conta = new ContaMEI(cnpjLimpo);
        } else {
            conta = new ContaPJ(cnpjLimpo);
        }

        conta.setRazaoSocial(razaoSocial.trim());
        conta.setTipo(tipo);
        conta.setEmail(emailContato);
        conta.setTelefone(telefone);
        conta.setSaldo(deposito);

        return conta;
    }
}
