package com.banco.pj;

import com.banco.pj.dominio.Auditoria;
import com.banco.pj.dominio.enums.TipoEmpresa;
import com.banco.pj.infra.EnviadorEmail;
import com.banco.pj.infra.EnviadorSms;
import com.banco.pj.servico.impl.AberturaContaValidador;
import com.banco.pj.infra.RepositorioContasEmMemoria;
import com.banco.pj.servico.impl.ServicoContaPJ;
import com.banco.pj.servico.impl.strategy.SeletorPoliticaTarifacao;

import java.math.BigDecimal;

public class App {
    public static void main(String[] args) {
        RepositorioContasEmMemoria repositorio = new RepositorioContasEmMemoria();
        AberturaContaValidador aberturaContaValidador = new AberturaContaValidador(repositorio);
        Auditoria auditoriaObject = new Auditoria();
        EnviadorEmail email = new EnviadorEmail();
        EnviadorSms sms = new EnviadorSms();

        ServicoContaPJ servico = new ServicoContaPJ(repositorio, email, sms, aberturaContaValidador, auditoriaObject, SeletorPoliticaTarifacao.padrao());

        servico.abrirConta("11.444.777/0001-61", "Padaria Pao Quente", TipoEmpresa.ME,
                "contato@paoquente.com.br", "61999990000", new BigDecimal("15000.00"));
        servico.abrirConta("11.222.333/0001-81", "Tech Solutions", TipoEmpresa.LTDA,
                "financeiro@techsolutions.com.br", "61988880000", new BigDecimal("80000.00"));

        servico.transferir("11444777000161", "11222333000181", new BigDecimal("2500.00"), "PIX");
        servico.sacar("11222333000181", new BigDecimal("300.00"));
        servico.cobrarTarifaMensal("11444777000161");

        System.out.println(servico.gerarExtrato("11444777000161", "TXT"));
    }
}
