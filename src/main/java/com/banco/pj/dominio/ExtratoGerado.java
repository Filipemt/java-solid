package com.banco.pj.dominio;

import com.banco.pj.dominio.exceptions.OperacaoInvalidaException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ExtratoGerado(String conteudo) {

    public static ExtratoGerado paraFormato(String formato, ContaPJ conta) {
        return switch (formato) {
            case "TXT" -> new ExtratoGerado(formatarTxt(conta));
            case "CSV" -> new ExtratoGerado(formatarCsv(conta));
            case "JSON" -> new ExtratoGerado(formatarJson(conta));
            default -> throw new OperacaoInvalidaException("Formato de extrato nao suportado: " + formato);
        };
    }

    private static String formatarTxt(ContaPJ conta) {
        StringBuilder sb = new StringBuilder();
        sb.append("EXTRATO - ").append(conta.getRazaoSocial()).append(" (").append(conta.getCnpj()).append(")\n");
        for (Lancamento l : conta.getLancamentos()) {
            sb.append(l.tipo()).append(" | ").append(l.descricao()).append(" | ")
                    .append(normalizar(l.valor()).toPlainString()).append("\n");
        }
        sb.append("SALDO: ").append(normalizar(conta.getSaldo()).toPlainString()).append("\n");
        return sb.toString();
    }

    private static String formatarCsv(ContaPJ conta) {
        StringBuilder sb = new StringBuilder();
        sb.append("tipo;descricao;valor\n");
        for (Lancamento l : conta.getLancamentos()) {
            sb.append(l.tipo()).append(";").append(l.descricao()).append(";")
                    .append(normalizar(l.valor()).toPlainString()).append("\n");
        }
        return sb.toString();
    }

    private static String formatarJson(ContaPJ conta) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"cnpj\":\"").append(conta.getCnpj()).append("\",");
        sb.append("\"razaoSocial\":\"").append(conta.getRazaoSocial().replace("\"", "\\\"")).append("\",");
        sb.append("\"saldo\":").append(normalizar(conta.getSaldo()).toPlainString()).append(",");
        sb.append("\"lancamentos\":[");
        for (int i = 0; i < conta.getLancamentos().size(); i++) {
            Lancamento l = conta.getLancamentos().get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"tipo\":\"").append(l.tipo()).append("\",");
            sb.append("\"descricao\":\"").append(l.descricao()).append("\",");
            sb.append("\"valor\":").append(normalizar(l.valor()).toPlainString()).append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static BigDecimal normalizar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
