package com.banco.pj.servico.impl.strategy;

import com.banco.pj.dominio.ContaPJ;
import com.banco.pj.dominio.Lancamento;
import com.banco.pj.dominio.ValorMonetario;
import com.banco.pj.servico.GeradorExtrato;

public class GeradorExtratoTxtStrategy implements GeradorExtrato {

    @Override
    public String gerarExtrato(ContaPJ conta) {
        StringBuilder sb = new StringBuilder();
        sb.append("EXTRATO - ").append(conta.getRazaoSocial()).append(" (").append(conta.getCnpj()).append(")\n");
        for (Lancamento l : conta.getLancamentos()) {
            sb.append(l.tipo()).append(" | ").append(l.descricao()).append(" | ")
                    .append(ValorMonetario.normalizar(l.valor()).toPlainString()).append("\n");
        }
        sb.append("SALDO: ").append(ValorMonetario.normalizar(conta.getSaldo()).toPlainString()).append("\n");
        return sb.toString();
    }
}
