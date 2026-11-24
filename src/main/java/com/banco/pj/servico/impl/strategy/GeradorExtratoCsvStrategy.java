package com.banco.pj.servico.impl.strategy;

import com.banco.pj.dominio.ContaPJ;
import com.banco.pj.dominio.Lancamento;
import com.banco.pj.dominio.ValorMonetario;
import com.banco.pj.servico.GeradorExtrato;

public class GeradorExtratoCsvStrategy implements GeradorExtrato {

    @Override
    public String gerarExtrato(ContaPJ conta) {
        StringBuilder sb = new StringBuilder();
        sb.append("tipo;descricao;valor\n");
        for (Lancamento l : conta.getLancamentos()) {
            sb.append(l.tipo()).append(";").append(l.descricao()).append(";")
                    .append(ValorMonetario.normalizar(l.valor()).toPlainString()).append("\n");
        }
        return sb.toString();
    }
}
