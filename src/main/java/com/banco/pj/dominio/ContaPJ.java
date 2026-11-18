package com.banco.pj.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ContaPJ {

    private final String cnpj;
    private String razaoSocial;
    private TipoEmpresa tipo;
    private String email;
    private String telefone;
    private BigDecimal saldo = BigDecimal.ZERO;
    private BigDecimal limiteChequeEspecial = BigDecimal.ZERO;
    private boolean ativa = true;
    private final List<Lancamento> lancamentos = new ArrayList<>();
    private final List<Emprestimo> emprestimos = new ArrayList<>();

    public void registrarLancamento(String tipo, String descricao, BigDecimal valor) {
        lancamentos.add(new Lancamento(tipo, descricao, valor));
    }

    public BigDecimal debitar(BigDecimal valor) {
        saldo = saldo.subtract(valor);
        return valor;
    }

    public BigDecimal creditar(BigDecimal valor) {
        saldo = saldo.add(valor);
        return valor;
    }

    public void validarSaldo(BigDecimal valor) {
        BigDecimal disponivel = saldo.add(limiteChequeEspecial);
        if (valor.compareTo(disponivel) > 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente para a operação");
        }
    }

    public ContaPJ(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public TipoEmpresa getTipo() {
        return tipo;
    }

    public void setTipo(TipoEmpresa tipo) {
        this.tipo = tipo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public BigDecimal getLimiteChequeEspecial() {
        return limiteChequeEspecial;
    }

    public void setLimiteChequeEspecial(BigDecimal limiteChequeEspecial) {
        this.limiteChequeEspecial = limiteChequeEspecial;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public List<Lancamento> getLancamentos() {
        return lancamentos;
    }

    public List<Emprestimo> getEmprestimos() {
        return emprestimos;
    }
}
