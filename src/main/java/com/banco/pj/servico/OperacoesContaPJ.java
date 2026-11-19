package com.banco.pj.servico;

import com.banco.pj.dominio.ContaPJ;
import com.banco.pj.dominio.Emprestimo;
import com.banco.pj.dominio.enums.TipoEmpresa;

import java.math.BigDecimal;

public interface OperacoesContaPJ {

    ContaPJ abrirConta(String cnpj, String razaoSocial, TipoEmpresa tipo,
                       String email, String telefone, BigDecimal depositoInicial);

    void encerrarConta(String cnpj);

    BigDecimal consultarSaldo(String cnpj);

    void depositar(String cnpj, BigDecimal valor);

    void sacar(String cnpj, BigDecimal valor);

    void transferir(String cnpjOrigem, String cnpjDestino, BigDecimal valor, String tipoTransferencia);

    void pagarBoleto(String cnpj, String codigoBarras, BigDecimal valor);

    Emprestimo solicitarEmprestimo(String cnpj, BigDecimal valor, int meses);

    void solicitarAumentoLimite(String cnpj, BigDecimal novoLimite);

    String gerarExtrato(String cnpj, String formato);

    BigDecimal cobrarTarifaMensal(String cnpj);
}
