package com.banco.pj;

import com.banco.pj.dominio.Auditoria;
import com.banco.pj.dominio.enums.TipoEmpresa;
import com.banco.pj.servico.impl.AberturaContaValidador;
import com.banco.pj.infra.RepositorioContasEmMemoria;
import com.banco.pj.servico.impl.ServicoContaPJ;

import java.math.BigDecimal;

public class App {
    public static void main(String[] args) {
        RepositorioContasEmMemoria repositorio = new RepositorioContasEmMemoria();
        AberturaContaValidador aberturaContaValidador = new AberturaContaValidador(repositorio);
        Auditoria auditoriaObject = new Auditoria();

        ServicoContaPJ servico = new ServicoContaPJ(aberturaContaValidador, repositorio, auditoriaObject);

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
