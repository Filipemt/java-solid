package com.banco.pj.servico.impl.strategy;

import com.banco.pj.dominio.ContaPJ;
import com.banco.pj.dominio.Lancamento;
import com.banco.pj.dominio.ValorMonetario;
import com.banco.pj.servico.GeradorExtrato;

public class GeradorExtratoJsonStrategy implements GeradorExtrato {

    @Override
    public String gerarExtrato(ContaPJ conta) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"cnpj\":\"").append(conta.getCnpj()).append("\",");
        sb.append("\"razaoSocial\":\"").append(conta.getRazaoSocial().replace("\"", "\\\"")).append("\",");
        sb.append("\"saldo\":").append(ValorMonetario.normalizar(conta.getSaldo()).toPlainString()).append(",");
        sb.append("\"lancamentos\":[");
        for (int i = 0; i < conta.getLancamentos().size(); i++) {
            Lancamento l = conta.getLancamentos().get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"tipo\":\"").append(l.tipo()).append("\",");
            sb.append("\"descricao\":\"").append(l.descricao()).append("\",");
            sb.append("\"valor\":").append(ValorMonetario.normalizar(l.valor()).toPlainString()).append("}");
        }
        sb.append("]}");
        return sb.toString();
    }
}
